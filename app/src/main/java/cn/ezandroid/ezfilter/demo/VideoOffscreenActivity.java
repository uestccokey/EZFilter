package cn.ezandroid.ezfilter.demo;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.PicassoEngine;

import java.util.List;

import cn.ezandroid.ezfilter.EZFilter;
import cn.ezandroid.ezfilter.environment.SurfaceRenderView;
import cn.ezandroid.ezfilter.media.AudioTrackTranscoder;
import cn.ezandroid.ezfilter.media.MediaUtil;
import cn.ezandroid.ezfilter.media.QueuedMuxer;
import cn.ezandroid.ezfilter.media.VideoTrackTranscoder;

/**
 * VideoOffscreenActivity
 *
 * @author like
 * @date 2017-09-24
 */
public class VideoOffscreenActivity extends BaseActivity {

    private static final int REQUEST_CODE_CHOOSE = 1;

    private SurfaceRenderView mRenderView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_offscreen);
        mRenderView = $(R.id.render_view);

        $(R.id.choose_video).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Matisse.from(VideoOffscreenActivity.this)
                        .choose(MimeType.of(MimeType.MP4), false)
                        .showSingleMediaType(true)
                        .maxSelectable(1)
                        .countable(false)
                        .gridExpectedSize(getResources().getDimensionPixelSize(R.dimen.grid_expected_size))
                        .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                        .thumbnailScale(0.85f)
                        .imageEngine(new PicassoEngine())
                        .forResult(REQUEST_CODE_CHOOSE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CHOOSE && resultCode == RESULT_OK) {
            final List<String> paths = Matisse.obtainPathResult(data);
            if (!paths.isEmpty()) {
                new Thread() {
                    public void run() {
                        renderVideo(paths.get(0), "/sdcard/render.mp4");
                    }
                }.start();
            }
        }
    }

    public boolean renderVideo(String input, final String output) {
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(input);
            // 读取音视频轨
            MediaUtil.Track track = MediaUtil.getFirstTrack(extractor);
            if (null == track || null == track.videoTrackFormat) return false;
            int w = track.videoTrackFormat.getInteger(MediaFormat.KEY_WIDTH);
            int h = track.videoTrackFormat.getInteger(MediaFormat.KEY_HEIGHT);

            // 修正角度，可能需要交换宽高
            int degrees = 0;
            if (track.videoTrackFormat.containsKey(MediaUtil.KEY_ROTATION)) {
                degrees = track.videoTrackFormat.getInteger(MediaUtil.KEY_ROTATION);
            } else {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                try {
                    retriever.setDataSource(input);
                    String rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
                    degrees = Integer.parseInt(rotation);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                } finally {
                    retriever.release();
                }
            }
            if (degrees == 90 || degrees == 270) {
                // 交换宽高
                w = w ^ h;
                h = w ^ h;
                w = w ^ h;
            }

            int sampleRate = track.audioTrackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            int channel = track.audioTrackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

            // 视频Format
            MediaFormat videoFormat = MediaUtil.createVideoFormat(w, h);
            // 音频Format
            MediaFormat audioFormat = MediaUtil.createAudioFormat(sampleRate, channel);

            // 初始化转码器
            MediaMuxer muxer = new MediaMuxer(output, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            QueuedMuxer queuedMuxer = new QueuedMuxer(muxer);

            if (null != track.audioTrackFormat) {
                // 音视频轨道都需要
                queuedMuxer.setTrackCount(QueuedMuxer.TRACK_VIDEO & QueuedMuxer.TRACK_AUDIO);
                VideoTrackTranscoder videoTrack = new VideoTrackTranscoder(extractor, track.videoTrackIndex, videoFormat, queuedMuxer);
                AudioTrackTranscoder audioTrack = new AudioTrackTranscoder(extractor, track.audioTrackIndex, audioFormat, queuedMuxer);

                videoTrack.setup();
                audioTrack.setup();

                while (!videoTrack.isFinished() || !audioTrack.isFinished()) {
                    boolean stepped = videoTrack.stepPipeline() || audioTrack.stepPipeline();
                    if (!stepped) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            // nothing to do
                        }
                    }
                }

                videoTrack.release();
                audioTrack.release();
            } else {
                // 只需视频轨
                queuedMuxer.setTrackCount(QueuedMuxer.TRACK_VIDEO);
                VideoTrackTranscoder videoTrack = new VideoTrackTranscoder(extractor, track.videoTrackIndex, videoFormat, queuedMuxer);

                videoTrack.setup();

                while (!videoTrack.isFinished()) {
                    boolean stepped = videoTrack.stepPipeline();
                    if (!stepped) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            // nothing to do
                        }
                    }
                }

                videoTrack.release();
            }

            muxer.stop();
            muxer.release();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    EZFilter.setVideo(Uri.parse(output))
                            .setVideoLoop(true)
                            .into(mRenderView);

                }
            });

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            extractor.release();
        }
    }
}

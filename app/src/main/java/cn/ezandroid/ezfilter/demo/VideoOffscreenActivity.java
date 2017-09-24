package cn.ezandroid.ezfilter.demo;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.os.Bundle;
import android.support.annotation.Nullable;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_offscreen);
    }

    public static boolean renderVideo(String input, String output) {
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
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            extractor.release();
        }
    }
}

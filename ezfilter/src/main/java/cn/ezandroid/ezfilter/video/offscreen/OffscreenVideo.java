package cn.ezandroid.ezfilter.video.offscreen;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;

import java.io.IOException;

import cn.ezandroid.ezfilter.core.FilterRender;
import cn.ezandroid.ezfilter.core.RenderPipeline;
import cn.ezandroid.ezfilter.video.offscreen.media.AudioTrackTranscoder;
import cn.ezandroid.ezfilter.video.offscreen.media.MediaUtil;
import cn.ezandroid.ezfilter.video.offscreen.media.QueuedMuxer;
import cn.ezandroid.ezfilter.video.offscreen.media.VideoFBORender;
import cn.ezandroid.ezfilter.video.offscreen.media.VideoTrackTranscoder;

/**
 * 离屏渲染视频
 *
 * @author like
 * @date 2017-09-24
 */
public class OffscreenVideo {

    private RenderPipeline mPipeline;

    private MediaExtractor mExtractor;
    private MediaUtil.Track mTrack;
    private String mVideoPath;
    private VideoFBORender mOffscreenRender;

    private int mWidth;
    private int mHeight;

    public OffscreenVideo(String videoPath) {
        mVideoPath = videoPath;

        init();
    }

    private void init() {
        mExtractor = new MediaExtractor();
        try {
            mExtractor.setDataSource(mVideoPath);
            // 读取音视频轨
            mTrack = MediaUtil.getFirstTrack(mExtractor);
            if (null == mTrack || null == mTrack.videoTrackFormat) {
                return;
            }
            int w = mTrack.videoTrackFormat.getInteger(MediaFormat.KEY_WIDTH);
            int h = mTrack.videoTrackFormat.getInteger(MediaFormat.KEY_HEIGHT);

            // 修正角度，可能需要交换宽高
            int degrees = 0;
            if (mTrack.videoTrackFormat.containsKey(MediaUtil.KEY_ROTATION)) {
                degrees = mTrack.videoTrackFormat.getInteger(MediaUtil.KEY_ROTATION);
            } else {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                try {
                    retriever.setDataSource(mVideoPath);
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

            mWidth = w;
            mHeight = h;

            mOffscreenRender = new VideoFBORender();
            mPipeline = new RenderPipeline();
            mPipeline.onSurfaceCreated(null, null);
            mPipeline.onSurfaceChanged(null, w, h);
            mPipeline.setStartPointRender(mOffscreenRender);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addFilterRender(FilterRender filterRender) {
        mPipeline.addFilterRender(filterRender);
    }

    public void save(String output) throws IOException {
        if (null == mTrack || null == mTrack.videoTrackFormat) {
            return;
        }
        mPipeline.startRender();

        // 视频Format
        MediaFormat videoFormat = MediaUtil.createVideoFormat(mWidth, mHeight);

        // 初始化转码器
        MediaMuxer muxer = new MediaMuxer(output, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        QueuedMuxer queuedMuxer = new QueuedMuxer(muxer);

        if (null != mTrack.audioTrackFormat) {
            int sampleRate = mTrack.audioTrackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            int channel = mTrack.audioTrackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

            // 音频Format
            MediaFormat audioFormat = MediaUtil.createAudioFormat(sampleRate, channel);

            // 音视频轨道都需要
            queuedMuxer.setTrackCount(QueuedMuxer.TRACK_VIDEO & QueuedMuxer.TRACK_AUDIO);
            VideoTrackTranscoder videoTrack = new VideoTrackTranscoder(mExtractor, mTrack.videoTrackIndex,
                    videoFormat, queuedMuxer, mOffscreenRender);
            AudioTrackTranscoder audioTrack = new AudioTrackTranscoder(mExtractor, mTrack.audioTrackIndex,
                    audioFormat, queuedMuxer);

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

            mPipeline.onSurfaceDestroyed(); // 需要在videoTrack.release();前调用
            videoTrack.release();
            audioTrack.release();
        } else {
            // 只需视频轨
            queuedMuxer.setTrackCount(QueuedMuxer.TRACK_VIDEO);
            VideoTrackTranscoder videoTrack = new VideoTrackTranscoder(mExtractor, mTrack.videoTrackIndex,
                    videoFormat, queuedMuxer, mOffscreenRender);

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

            mPipeline.onSurfaceDestroyed(); // 需要在videoTrack.release();前调用
            videoTrack.release();
        }

        muxer.stop();
        muxer.release();

        mExtractor.release();
    }
}

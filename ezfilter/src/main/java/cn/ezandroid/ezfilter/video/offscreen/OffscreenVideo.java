package cn.ezandroid.ezfilter.video.offscreen;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.media.AudioFormat;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.os.Build;

import java.io.IOException;
import java.util.List;

import cn.ezandroid.ezfilter.core.FBORender;
import cn.ezandroid.ezfilter.core.GLRender;
import cn.ezandroid.ezfilter.core.RenderPipeline;
import cn.ezandroid.ezfilter.media.transcode.AudioTrackTranscoder;
import cn.ezandroid.ezfilter.media.transcode.IVideoRender;
import cn.ezandroid.ezfilter.media.transcode.QueuedMuxer;
import cn.ezandroid.ezfilter.media.transcode.VideoFBORender;
import cn.ezandroid.ezfilter.media.transcode.VideoTrackTranscoder;
import cn.ezandroid.ezfilter.media.util.MediaUtil;

/**
 * 离屏渲染视频
 *
 * @author like
 * @date 2017-09-24
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class OffscreenVideo {

    private RenderPipeline mPipeline;

    private MediaExtractor mExtractor;
    private MediaUtil.Track mTrack;
    private String mVideoPath;
    private VideoFBORender mOffscreenRender;

    private IVideoRenderListener mVideoRenderListener;

    private int mWidth;
    private int mHeight;

    public interface IVideoRenderListener {

        /**
         * 当前帧绘制回调
         *
         * @param time 当前帧时间（单位纳秒）
         */
        void onFrameDraw(long time);
    }

    public OffscreenVideo(String videoPath) {
        mVideoPath = videoPath;

        initRenderSize();
    }

    private void initRenderSize() {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initPipeline() {
        if (mPipeline != null) {
            return;
        }
        mOffscreenRender = new VideoFBORender();
        mOffscreenRender.setRenderSize(mWidth, mHeight);
        mPipeline = new RenderPipeline();
        mPipeline.onSurfaceCreated(null, null);
        mPipeline.setStartPointRender(mOffscreenRender);
        mPipeline.addEndPointRender(new GLRender());
    }

    public void setVideoRenderListener(IVideoRenderListener videoRenderListener) {
        mVideoRenderListener = videoRenderListener;
    }

    public void addFilterRender(FBORender filterRender) {
        initPipeline();
        mPipeline.addFilterRender(filterRender);
    }

    public void removeFilterRender(FBORender filterRender) {
        initPipeline();
        mPipeline.removeFilterRender(filterRender);
    }

    public List<FBORender> getFilterRenders() {
        initPipeline();
        return mPipeline.getFilterRenders();
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    private int getInteger(String name, int defaultValue) {
        try {
            return mTrack.audioTrackFormat.getInteger(name);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private VideoTrackTranscoder initVideoTrack(MediaFormat videoFormat, QueuedMuxer queuedMuxer) {
        return new VideoTrackTranscoder(mExtractor, mTrack.videoTrackIndex,
                videoFormat, queuedMuxer, new IVideoRender() {
            @Override
            public void drawFrame(long time) {
                if (mVideoRenderListener != null) {
                    mVideoRenderListener.onFrameDraw(time);
                }
                mOffscreenRender.drawFrame(time);
            }

            @Override
            public SurfaceTexture getSurfaceTexture() {
                return mOffscreenRender.getSurfaceTexture();
            }
        });
    }

    private AudioTrackTranscoder initAudioTrack(MediaFormat audioFormat, QueuedMuxer queuedMuxer) {
        return new AudioTrackTranscoder(mExtractor, mTrack.audioTrackIndex,
                audioFormat, queuedMuxer);
    }

    public void save(String output) throws IOException {
        save(output, mWidth, mHeight);
    }

    public void save(String output, int width, int height) throws IOException {
        if (null == mTrack || null == mTrack.videoTrackFormat) {
            return;
        }
        initPipeline();
        mPipeline.onSurfaceChanged(null, width, height);
        mPipeline.startRender();

        // 视频Format 保证输入和输出的码率不变，防止视频处理后变模糊
        MediaFormat videoFormat = MediaUtil.createVideoFormat(width, height,
                MediaUtil.getMetadata(mVideoPath).bitrate, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

        // 初始化转码器
        MediaMuxer muxer = new MediaMuxer(output, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        QueuedMuxer queuedMuxer = new QueuedMuxer(muxer);

        if (null != mTrack.audioTrackFormat) {
            int sampleRate = getInteger(MediaFormat.KEY_SAMPLE_RATE, 44100);
            int channelMask = getInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_STEREO);
            int channelCount = getInteger(MediaFormat.KEY_CHANNEL_COUNT, 2);

            // 音频Format
            MediaFormat audioFormat = MediaUtil.createAudioFormat(sampleRate, channelMask, channelCount);

            // 音视频轨道都需要
            queuedMuxer.setTrackCount(QueuedMuxer.TRACK_VIDEO & QueuedMuxer.TRACK_AUDIO);
            VideoTrackTranscoder videoTrack = initVideoTrack(videoFormat, queuedMuxer);
            AudioTrackTranscoder audioTrack = initAudioTrack(audioFormat, queuedMuxer);

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
            VideoTrackTranscoder videoTrack = initVideoTrack(videoFormat, queuedMuxer);

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

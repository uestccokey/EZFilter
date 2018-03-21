package cn.ezandroid.ezfilter.media.record;

import java.io.IOException;

import cn.ezandroid.ezfilter.core.EndPointRender;
import cn.ezandroid.ezfilter.core.FBORender;
import cn.ezandroid.ezfilter.core.ISupportRecord;

/**
 * 支持视频录制的终点渲染器
 *
 * @author like
 * @date 2017-10-13
 */
public class RecordableEndPointRender extends EndPointRender implements ISupportRecord {

    private MediaVideoEncoder mVideoEncoder;
    private MediaMuxerWrapper mMuxerWrapper;

    private boolean mRecordVideo = true;
    private boolean mRecordAudio = true;

    private String mOutputPath;

    private final MediaEncoder.MediaEncoderListener mMediaEncoderListener =
            new MediaEncoder.MediaEncoderListener() {
                @Override
                public void onPrepared(final MediaEncoder encoder) {
                    if (encoder instanceof MediaVideoEncoder) {
                        setVideoEncoder((MediaVideoEncoder) encoder);
                    }
                }

                @Override
                public void onStopped(final MediaEncoder encoder) {
                    if (encoder instanceof MediaVideoEncoder) {
                        setVideoEncoder(null);
                    }
                }
            };

    public RecordableEndPointRender(String outputPath, boolean recordVideo, boolean recordAudio) {
        mOutputPath = outputPath;

        mRecordVideo = recordVideo;
        mRecordAudio = recordAudio;
    }

    @Override
    public void onTextureAcceptable(int texture, FBORender source) {
        super.onTextureAcceptable(texture, source);
        synchronized (this) {
            if (mVideoEncoder != null) {
                int oldTexture = mVideoEncoder.getInputTextureId();
                if (texture != oldTexture) {
                    mVideoEncoder.setInputTextureId(texture);
                }
            }
        }
    }

    /**
     * 设置视频编码器
     *
     * @param encoder
     */
    public void setVideoEncoder(final MediaVideoEncoder encoder) {
        synchronized (this) {
            mVideoEncoder = encoder;
        }
    }

    /**
     * 设置输出路径
     *
     * @param outputPath
     */
    @Override
    public void setRecordOutputPath(String outputPath) {
        mOutputPath = outputPath;
    }

    /**
     * 音频录制开关
     *
     * @param enable
     */
    @Override
    public void enableRecordAudio(boolean enable) {
        mRecordAudio = enable;
    }

    /**
     * 影像录制开关
     *
     * @param enable
     */
    @Override
    public void enableRecordVideo(boolean enable) {
        mRecordVideo = enable;
    }

    /**
     * 是否正在录制视频
     *
     * @return
     */
    public boolean isRecording() {
        return mMuxerWrapper != null && mMuxerWrapper.isRecording();
    }

    /**
     * 开始录制
     */
    public void startRecording() {
        try {
            mMuxerWrapper = new MediaMuxerWrapper(mOutputPath);
            if (mRecordVideo) {
                new MediaVideoEncoder(mMuxerWrapper, mMediaEncoderListener, getWidth(), getHeight());
            }
            if (mRecordAudio) {
                new MediaAudioEncoder(mMuxerWrapper, mMediaEncoderListener);
            }
            mMuxerWrapper.prepare();
            mMuxerWrapper.startRecording();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止录制
     */
    public void stopRecording() {
        if (mMuxerWrapper != null) {
            mMuxerWrapper.stopRecording();
            mMuxerWrapper = null;
        }
    }

    @Override
    protected void drawFrame() {
        super.drawFrame();
        synchronized (this) {
            if (mVideoEncoder != null) {
                mVideoEncoder.frameAvailableSoon();
            }
        }
    }
}

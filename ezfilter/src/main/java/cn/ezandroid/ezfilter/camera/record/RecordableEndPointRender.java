package cn.ezandroid.ezfilter.camera.record;

import java.io.IOException;

import cn.ezandroid.ezfilter.core.EndPointRender;

/**
 * 支持视频录制的终点渲染器
 *
 * @author like
 * @date 2017-10-13
 */
public class RecordableEndPointRender extends EndPointRender {

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

    /**
     * 设置视频编码器
     *
     * @param encoder
     */
    public void setVideoEncoder(final MediaVideoEncoder encoder) {
        // 在GL线程绑定输入纹理
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    if (encoder != null) {
                        encoder.setInputTextureId(mTextureIn);
                    }
                    mVideoEncoder = encoder;
                }
            }
        });
    }

    /**
     * 是否正在录制视频
     *
     * @return
     */
    public boolean isRecording() {
        return mMuxerWrapper != null && mMuxerWrapper.isStarted();
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

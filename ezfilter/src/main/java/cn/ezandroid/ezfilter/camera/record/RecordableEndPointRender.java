package cn.ezandroid.ezfilter.camera.record;

import android.opengl.EGL14;

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

    private final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
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

    /**
     * 设置视频编码器
     *
     * @param encoder
     */
    public void setVideoEncoder(final MediaVideoEncoder encoder) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    if (encoder != null) {
                        encoder.setEglContext(EGL14.eglGetCurrentContext(), mTextureIn);
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
            mMuxerWrapper = new MediaMuxerWrapper(".mp4");
            new MediaVideoEncoder(mMuxerWrapper, mMediaEncoderListener,
                    getWidth(), getHeight());
            new MediaAudioEncoder(mMuxerWrapper, mMediaEncoderListener);
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
                mVideoEncoder.frameAvailableSoon(null);
            }
        }
    }
}

package cn.ezandroid.lib.ezfilter.media.record;

import cn.ezandroid.lib.ezfilter.core.FBORender;
import cn.ezandroid.lib.ezfilter.core.GLRender;

/**
 * 支持视频录制的渲染器
 *
 * @author like
 * @date 2017-10-13
 */
public class RecordableRender extends FBORender implements ISupportRecord {

    private MediaVideoEncoder mVideoEncoder;
    private MediaMuxerWrapper mMuxerWrapper;

    private boolean mRecordVideo;
    private boolean mRecordAudio;

    private String mOutputPath;

    private IRecordListener mRecordListener;

    private IAudioExtraEncoder mAudioExtraEncoder;

    private int mRecordWidth;
    private int mRecordHeight;

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

                @Override
                public void onInterrupted(MediaEncoder encoder) {
                    stopRecording();
                }
            };

    public RecordableRender(String outputPath, boolean recordVideo, boolean recordAudio) {
        mOutputPath = outputPath;

        mRecordVideo = recordVideo;
        mRecordAudio = recordAudio;
    }

    @Override
    public void onTextureAcceptable(int texture, GLRender source) {
        super.onTextureAcceptable(texture, source);
        try {
            if (mVideoEncoder != null) {
                int oldTexture = mVideoEncoder.getInputTextureId();
                if (texture != oldTexture) {
                    mVideoEncoder.setInputTextureId(texture);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置视频录制监听器
     *
     * @param listener
     */
    public void setRecordListener(IRecordListener listener) {
        mRecordListener = listener;
    }

    /**
     * 设置音频的额外编码器
     *
     * @param encoder
     */
    public void setAudioExtraEncoder(IAudioExtraEncoder encoder) {
        mAudioExtraEncoder = encoder;
    }

    /**
     * 设置视频编码器
     *
     * @param encoder
     */
    public void setVideoEncoder(final MediaVideoEncoder encoder) {
        mVideoEncoder = encoder;
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
     * 设置视频录制宽高
     *
     * @param width
     * @param height
     */
    @Override
    public void setRecordSize(int width, int height) {
        mRecordWidth = width;
        mRecordHeight = height;
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
    public boolean startRecording() {
        try {
            mMuxerWrapper = new MediaMuxerWrapper(mOutputPath);
            if (mRecordVideo) {
                new MediaVideoEncoder(mMuxerWrapper, mMediaEncoderListener,
                        mRecordWidth <= 0 ? getWidth() : mRecordWidth, mRecordHeight <= 0 ? getHeight() : mRecordHeight);
            }
            if (mRecordAudio) {
                MediaAudioEncoder audioEncoder = new MediaAudioEncoder(mMuxerWrapper, mMediaEncoderListener);
                audioEncoder.setAudioExtraEncoder(mAudioExtraEncoder);
            }
            mMuxerWrapper.setRecordListener(mRecordListener);
            mMuxerWrapper.prepare();
            return mMuxerWrapper.startRecording();
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 停止录制
     */
    public void stopRecording() {
        if (mMuxerWrapper != null) {
            mMuxerWrapper.stopRecording();
//            mMuxerWrapper = null;
        }
    }

    @Override
    protected void drawFrame() {
        super.drawFrame();
        try {
            if (mVideoEncoder != null) {
                mVideoEncoder.frameAvailableSoon();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

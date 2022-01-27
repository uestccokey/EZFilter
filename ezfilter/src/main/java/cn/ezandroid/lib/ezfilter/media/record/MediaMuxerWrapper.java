package cn.ezandroid.lib.ezfilter.media.record;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 音视频复用器
 * <p>
 * 将音频和视频合成mp4
 */
public class MediaMuxerWrapper {

    private String mOutputPath;
    private final MediaMuxer mMediaMuxer;
    private int mEncoderCount, mStartedCount;
    private volatile boolean mIsStarted;
    private MediaEncoder mVideoEncoder, mAudioEncoder;

    private IRecordListener mRecordListener;

    /**
     * @param outPath 视频输出路径
     * @throws IOException
     */
    public MediaMuxerWrapper(String outPath) throws IOException {
        mOutputPath = outPath;
        mMediaMuxer = new MediaMuxer(mOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        mEncoderCount = mStartedCount = 0;
        mIsStarted = false;
    }

    /**
     * 设置视频录制监听器
     *
     * @param listener
     */
    public void setRecordListener(IRecordListener listener) {
        mRecordListener = listener;
    }

    public String getOutputPath() {
        return mOutputPath;
    }

    public void prepare() throws IOException, IllegalStateException {
        if (mVideoEncoder != null) {
            mVideoEncoder.prepare();
        }
        if (mAudioEncoder != null) {
            mAudioEncoder.prepare();
        }
    }

    public boolean startRecording() {
        boolean success = false;
        if (mVideoEncoder != null) {
            success = mVideoEncoder.startRecording();
        }
        if (mAudioEncoder != null) {
            success = success && mAudioEncoder.startRecording();
        }
        if (mRecordListener != null && success) {
            mRecordListener.onStart();
        }
        return success;
    }

    public void stopRecording() {
        if (mVideoEncoder != null) {
            mVideoEncoder.stopRecording();
        }
        mVideoEncoder = null;
        if (mAudioEncoder != null) {
            mAudioEncoder.stopRecording();
        }
        mAudioEncoder = null;
        if (mRecordListener != null) {
            mRecordListener.onStop();
        }
    }

    public synchronized boolean isStarted() {
        return mIsStarted;
    }

    void addEncoder(final MediaEncoder encoder) {
        if (encoder instanceof MediaVideoEncoder) {
            if (mVideoEncoder != null) {
                throw new IllegalArgumentException("Video encoder already added.");
            }
            mVideoEncoder = encoder;
        } else if (encoder instanceof MediaAudioEncoder) {
            if (mAudioEncoder != null) {
                throw new IllegalArgumentException("Audio encoder already added.");
            }
            mAudioEncoder = encoder;
        } else {
            throw new IllegalArgumentException("unsupported encoder");
        }
        mEncoderCount = (mVideoEncoder != null ? 1 : 0) + (mAudioEncoder != null ? 1 : 0);
    }

    synchronized boolean start() {
        mStartedCount++;
        if ((mEncoderCount > 0) && (mStartedCount == mEncoderCount)) {
            mMediaMuxer.start();
            mIsStarted = true;
            notifyAll();
        }
        return mIsStarted;
    }

    synchronized void stop() {
        mStartedCount--;
        if ((mEncoderCount > 0) && (mStartedCount <= 0)) {
            try {
                mMediaMuxer.stop();
                mMediaMuxer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mIsStarted = false;

            if (mRecordListener != null) {
                mRecordListener.onFinish();
            }
        }
    }

    synchronized int addTrack(final MediaFormat format) {
        if (mIsStarted) {
            throw new IllegalStateException("muxer already started");
        }
        return mMediaMuxer.addTrack(format);
    }

    synchronized void writeSampleData(final int trackIndex, final ByteBuffer byteBuf, final MediaCodec.BufferInfo bufferInfo) {
        if (mStartedCount > 0) {
            mMediaMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
        }
    }
}

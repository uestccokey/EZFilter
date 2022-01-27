package cn.ezandroid.lib.ezfilter.media.record;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import cn.ezandroid.lib.ezfilter.media.util.CodecUtil;

/**
 * 编码器抽象类
 */
public abstract class MediaEncoder implements Runnable {

    private static final String TAG = "MediaEncoder";

    // 超时时间
    private static final int TIMEOUT_USEC = 10000;    // 10[msec]

    public interface MediaEncoderListener {

        void onPrepared(MediaEncoder encoder);

        void onStopped(MediaEncoder encoder);

        void onInterrupted(MediaEncoder encoder);
    }

    protected final Object mSync = new Object();

    protected volatile boolean mIsCapturing;
    private int mRequestDrain;
    protected volatile boolean mRequestStop;
    protected boolean mIsEOS;
    protected boolean mMuxerStarted;
    protected int mTrackIndex;
    protected MediaCodec mMediaCodec;
    protected final WeakReference<MediaMuxerWrapper> mWeakMuxer;
    private MediaCodec.BufferInfo mBufferInfo;

    protected final MediaEncoderListener mListener;

    public MediaEncoder(final MediaMuxerWrapper muxer, final MediaEncoderListener listener) {
        if (listener == null) throw new NullPointerException("MediaEncoderListener is null");
        if (muxer == null) throw new NullPointerException("MediaMuxerWrapper is null");
        mWeakMuxer = new WeakReference<>(muxer);
        muxer.addEncoder(this);
        mListener = listener;
        synchronized (mSync) {
            // 使用Buffer减少GC
            mBufferInfo = new MediaCodec.BufferInfo();
            new Thread(this, getClass().getSimpleName()).start();
            try {
                mSync.wait();
            } catch (final InterruptedException e) {
            }
        }
    }

    public String getOutputPath() {
        final MediaMuxerWrapper muxer = mWeakMuxer.get();
        return muxer != null ? muxer.getOutputPath() : null;
    }

    public boolean frameAvailableSoon() {
        synchronized (mSync) {
            if (!mIsCapturing || mRequestStop) {
                return false;
            }
            mRequestDrain++;
            mSync.notifyAll();
        }
        return true;
    }

    @Override
    public void run() {
//		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        synchronized (mSync) {
            mRequestStop = false;
            mRequestDrain = 0;
            mSync.notify();
        }
        final boolean isRunning = true;
        boolean localRequestStop;
        boolean localRequestDrain;
        while (isRunning) {
            synchronized (mSync) {
                localRequestStop = mRequestStop;
                localRequestDrain = (mRequestDrain > 0);
                if (localRequestDrain) {
                    mRequestDrain--;
                }
            }
            if (localRequestStop) {
                drain();
                // request stop recording
                signalEndOfInputStream();
                // process output data again for EOS signale
                drain();
                // release all related objects
                release();
                break;
            }
            if (localRequestDrain) {
                drain();
            } else {
                synchronized (mSync) {
                    try {
                        mSync.wait();
                    } catch (final InterruptedException e) {
                        break;
                    }
                }
            }
        } // end of while
        synchronized (mSync) {
            mRequestStop = true;
            mIsCapturing = false;
        }
    }

    abstract void prepare() throws IOException, IllegalStateException;

    boolean startRecording() {
        synchronized (mSync) {
            mIsCapturing = true;
            mRequestStop = false;
            mSync.notifyAll();
        }
        return true;
    }

    void stopRecording() {
        synchronized (mSync) {
            if (!mIsCapturing || mRequestStop) {
                return;
            }
            mRequestStop = true;    // for rejecting newer frame
            mSync.notifyAll();
            // We can not know when the encoding and writing finish.
            // so we return immediately after request to avoid delay of caller thread
        }
    }

    protected void release() {
        try {
            mListener.onStopped(this);
        } catch (final Exception e) {
            Log.e(TAG, "failed onStopped", e);
        }
        mIsCapturing = false;
        if (mMediaCodec != null) {
            try {
                mMediaCodec.stop();
                mMediaCodec.release();
                mMediaCodec = null;
            } catch (final Exception e) {
                Log.e(TAG, "failed releasing MediaCodec", e);
            }
        }
        if (mMuxerStarted) {
            final MediaMuxerWrapper muxer = mWeakMuxer != null ? mWeakMuxer.get() : null;
            if (muxer != null) {
                muxer.stop();
            }
        }
        mBufferInfo = null;
    }

    protected void signalEndOfInputStream() {
        // signalEndOfInputStream is only avairable for video encoding with surface
        // and equivalent sending a empty buffer with BUFFER_FLAG_END_OF_STREAM flag.
//		mMediaCodec.signalEndOfInputStream();	// API >= 18
        encode(null, 0, getPTSUs());
    }

    protected void encode(final ByteBuffer buffer, final int length, final long presentationTimeUs) {
        if (!mIsCapturing) return;
        while (mIsCapturing) {
            final int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
            if (inputBufferIndex >= 0) {
                final ByteBuffer inputBuffer = CodecUtil.getInputBuffer(mMediaCodec, inputBufferIndex);
                inputBuffer.clear();
                if (buffer != null) {
                    inputBuffer.put(buffer);
                }
//                if (length <= 0) {
//                    // send EOS
//                    mIsEOS = true;
//                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, 0,
//                            presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
//                    break;
//                } else {
                mMediaCodec.queueInputBuffer(inputBufferIndex, 0, length,
                        presentationTimeUs, 0);
//                }
                break;
            } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // wait for MediaCodec encoder is ready to encode
                // nothing to do here because MediaCodec#dequeueInputBuffer(TIMEOUT_USEC)
                // will wait for maximum TIMEOUT_USEC(10msec) on each call
            }
        }
    }

    protected void drain() {
        if (mMediaCodec == null) return;
        int encoderStatus, count = 0;
        final MediaMuxerWrapper muxer = mWeakMuxer.get();
        if (muxer == null) {
            return;
        }
        LOOP:
        while (mIsCapturing) {
            // get encoded data with maximum timeout duration of TIMEOUT_USEC(=10[msec])
            try {
                encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            } catch (Exception e) {
                encoderStatus = MediaCodec.INFO_TRY_AGAIN_LATER;

                e.printStackTrace();
            }
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // wait 3 counts(=TIMEOUT_USEC x 3 = 30msec) until data/EOS come
                if (!mIsEOS) {
                    if (++count > 3) {
                        break;        // out of while
                    }
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // this shoud not come when encoding
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // this status indicate the output format of codec is changed
                // this should come only once before actual encoded data
                // but this status never come on Android4.3 or less
                // and in that case, you should treat when MediaCodec.BUFFER_FLAG_CODEC_CONFIG come.
                if (mMuxerStarted) {    // second time request is error
                    throw new RuntimeException("format changed twice");
                }
                // get output format from codec and pass them to muxer
                // getOutputFormat should be called after INFO_OUTPUT_FORMAT_CHANGED otherwise crash.
                final MediaFormat format = mMediaCodec.getOutputFormat(); // API >= 16
                mTrackIndex = muxer.addTrack(format);
                mMuxerStarted = true;
                if (!muxer.start()) {
                    // we should wait until muxer is ready
                    synchronized (muxer) {
                        while (!muxer.isStarted())
                            try {
                                muxer.wait(100);
                            } catch (final InterruptedException e) {
                                break LOOP;
                            }
                    }
                }
            } else if (encoderStatus < 0) {
                // unexpected status
            } else {
                final ByteBuffer encodedData = CodecUtil.getOutputBuffer(mMediaCodec, encoderStatus);
                if (encodedData == null) {
                    // this never should come...may be a MediaCodec internal error
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                }
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // You shoud set output format to muxer here when you target Android4.3 or less
                    // but MediaCodec#getOutputFormat can not call here(because INFO_OUTPUT_FORMAT_CHANGED don't come yet)
                    // therefor we should expand and prepare output format from buffer data.
                    // This sample is for API>=18(>=Android 4.3), just ignore this flag here
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                    // encoded data is ready, clear waiting counter
                    count = 0;
                    if (!mMuxerStarted) {
                        // muxer is not ready...this will prrograming failure.
                        throw new RuntimeException("drain:muxer hasn't started");
                    }
                    // write encoded data to muxer(need to adjust presentationTimeUs.
                    mBufferInfo.presentationTimeUs = getPTSUs();
                    muxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                    mPrevOutputPTSUs = mBufferInfo.presentationTimeUs;
                }
                // return buffer to encoder
                mMediaCodec.releaseOutputBuffer(encoderStatus, false);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    // when EOS come.
                    mIsCapturing = false;
                    break;      // out of while
                }
            }
        }
    }

    private long mPrevOutputPTSUs = 0;

    protected long getPTSUs() {
        long result = System.nanoTime() / 1000L;
        // presentationTimeUs should be monotonic
        // otherwise muxer fail to write
        if (result < mPrevOutputPTSUs)
            result = (mPrevOutputPTSUs - result) + result;
        return result;
    }
}

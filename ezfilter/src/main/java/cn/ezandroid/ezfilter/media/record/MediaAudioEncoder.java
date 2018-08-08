package cn.ezandroid.ezfilter.media.record;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

import cn.ezandroid.ezfilter.media.util.MediaUtil;

/**
 * 音频编码器
 */
public class MediaAudioEncoder extends MediaEncoder {

    private static final String TAG = "MediaAudioEncoder";

    private static final String MIME_TYPE = "audio/mp4a-latm";
    private static final int SAMPLE_RATE = 44100; // 44.1[KHz] is only setting guaranteed to be available on all devices.
    private static final int SAMPLES_PER_FRAME = 1024;

    private final Object mAudioThreadLock = new Object();

    private AudioThread mAudioThread;

    private IAudioExtraEncoder mAudioExtraEncoder;

    private int mSamplingRate = SAMPLE_RATE;

    public MediaAudioEncoder(MediaMuxerWrapper muxer, MediaEncoderListener listener) {
        super(muxer, listener);
    }

    public void setAudioExtraEncoder(IAudioExtraEncoder encoder) {
        mAudioExtraEncoder = encoder;
    }

    @Override
    protected void prepare() throws IOException, IllegalStateException {
        mTrackIndex = -1;
        mMuxerStarted = mIsEOS = false;

        final MediaCodecInfo audioCodecInfo = selectAudioCodec(MIME_TYPE);
        if (audioCodecInfo == null) {
            Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
            return;
        }

        // 音频Format
        MediaFormat audioFormat = MediaUtil.createAudioFormat(SAMPLE_RATE, AudioFormat.CHANNEL_IN_STEREO, 2);

        mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mMediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();
        if (mListener != null) {
            try {
                mListener.onPrepared(this);
            } catch (final Exception e) {
                Log.e(TAG, "prepare:", e);
            }
        }
    }

    @Override
    protected boolean startRecording() {
        super.startRecording();
        // 使用锁是为了防止刚刚创建出AudioThread后，在调用isAlive之前，release函数被另外的线程调用，导致mAudioThread为null引起的空指针异常
        synchronized (mAudioThreadLock) {
            // create and execute audio capturing thread using internal mic
            if (mAudioThread == null) {
                mAudioThread = new AudioThread();
                mAudioThread.start();
            }
            return mAudioThread.isAlive();
        }
    }

    @Override
    protected void release() {
        synchronized (mAudioThreadLock) {
            mAudioThread = null;
        }
        super.release();
    }

    private static final int[] AUDIO_SOURCES = new int[]{
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT,
            MediaRecorder.AudioSource.CAMCORDER,
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
    };

    private class AudioThread extends Thread {

        private AudioRecord mAudioRecord;

        @Override
        public synchronized void start() {
            try {
                mAudioRecord = findAudioRecord();
                if (mAudioRecord != null) {
                    long time = System.currentTimeMillis();
                    // 在AudioThread的创建线程调用startRecording，以便解决Vivo手机上mAudioRecord.startRecording触发录音权限弹框时，没有阻塞住录制线程的问题
                    mAudioRecord.startRecording();
                    // 通过startRecording阻塞的时间来判断是否VIVO手机弹出了权限申请框，未弹出权限申请框时，执行时间一般都小于10ms
                    if (System.currentTimeMillis() - time > 100) {
                        // 无论选择什么，当前都停止录制，回调onError
                        if (mListener != null) {
                            mListener.onInterrupted(MediaAudioEncoder.this);
                        }
                    } else {
                        super.start();
                    }
                } else {
                    Log.e(TAG, "failed to initialize AudioRecord");
                }
            } catch (final Exception e) {
                Log.e(TAG, "AudioThread#new", e);
            }
        }

        /**
         * 查找可用的音频录制器
         *
         * @return
         */
        private AudioRecord findAudioRecord() {
            int[] samplingRates = new int[]{44100, 22050, 11025, 8000};
            int[] audioFormats = new int[]{
                    AudioFormat.ENCODING_PCM_16BIT,
                    AudioFormat.ENCODING_PCM_8BIT};
            int[] channelConfigs = new int[]{
                    AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.CHANNEL_IN_MONO};

            for (int rate : samplingRates) {
                for (int format : audioFormats) {
                    for (int config : channelConfigs) {
                        try {
                            int bufferSize = AudioRecord.getMinBufferSize(rate, config, format);
                            if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                                for (int source : AUDIO_SOURCES) {
                                    AudioRecord recorder = new AudioRecord(source, rate, config, format, bufferSize * 4);
                                    if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                                        mSamplingRate = rate;
                                        return recorder;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Init AudioRecord Error." + Log.getStackTraceString(e));
                        }
                    }
                }
            }
            return null;
        }

        private int getChannels(int channelConfig) {
            int channels;
            switch (channelConfig) {
                case AudioFormat.CHANNEL_IN_MONO:
                    channels = 1;
                    break;
                case AudioFormat.CHANNEL_IN_STEREO:
                    channels = 2;
                    break;
                default:
                    channels = 2;
                    break;
            }
            return channels;
        }

        private int getBitsPerSample(int audioFormat) {
            int bitsPerSample;
            switch (audioFormat) {
                case AudioFormat.ENCODING_PCM_16BIT:
                    bitsPerSample = 16;
                    break;
                case AudioFormat.ENCODING_PCM_8BIT:
                    bitsPerSample = 8;
                    break;
                default:
                    bitsPerSample = 16;
                    break;
            }
            return bitsPerSample;
        }

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            if (mAudioRecord != null) {
                try {
                    if (mIsCapturing) {
                        final ByteBuffer buf = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME);
                        int readBytes;
                        try {
                            if (mAudioExtraEncoder != null) {
                                mAudioExtraEncoder.setup(getChannels(mAudioRecord.getChannelConfiguration()),
                                        mSamplingRate, getBitsPerSample(mAudioRecord.getAudioFormat()) / 8);
                            }
                            for (; mIsCapturing && !mRequestStop && !mIsEOS; ) {
                                // read audio data from internal mic
                                buf.clear();
                                readBytes = mAudioRecord.read(buf, SAMPLES_PER_FRAME);
                                if (readBytes > 0) {
                                    // set audio data to encoder
                                    buf.position(readBytes);
                                    buf.flip();
                                    if (mAudioExtraEncoder != null) {
                                        ByteBuffer buffer = mAudioExtraEncoder.encode(buf);
                                        encode(buffer, buffer.hasArray() ? buffer.array().length : buffer.remaining(), getPTSUs());
                                    } else {
                                        encode(buf, readBytes, getPTSUs());
                                    }
                                    frameAvailableSoon();
                                }
                            }
                            frameAvailableSoon();
                        } finally {
                            mAudioRecord.stop();
                        }
                    }
                } finally {
                    if (mAudioExtraEncoder != null) {
                        mAudioExtraEncoder.release();
                    }
                    mAudioRecord.release();
                }
            }
        }
    }

    private static MediaCodecInfo selectAudioCodec(final String mimeType) {
        // get the list of available codecs
        final int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {    // skipp decoder
                continue;
            }
            final String[] types = codecInfo.getSupportedTypes();
            for (String type : types) {
                if (type.equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }
}
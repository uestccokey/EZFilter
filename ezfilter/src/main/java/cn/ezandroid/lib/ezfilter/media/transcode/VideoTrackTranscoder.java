package cn.ezandroid.lib.ezfilter.media.transcode;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import java.io.IOException;

import cn.ezandroid.lib.ezfilter.media.util.CodecUtil;

/**
 * 视轨转码器
 * https://android.googlesource.com/platform/cts/+/lollipop-release/tests/tests/media/src/android/media/cts/ExtractDecodeEditEncodeMuxTest.java
 *
 * @date 2017-08-23
 */
public class VideoTrackTranscoder implements TrackTranscoder {

    private static final int DRAIN_STATE_NONE = 0;
    private static final int DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY = 1;
    private static final int DRAIN_STATE_CONSUMED = 2;

    private final MediaExtractor mExtractor;
    private final QueuedMuxer mMuxer;

    private final int mTrackIndex;
    private final MediaFormat mOutputFormat;

    private final MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private MediaCodec mDecoder;
    private MediaCodec mEncoder;
    private MediaFormat mActualOutputFormat;

    private boolean mIsExtractorEOS;
    private boolean mIsDecoderEOS;
    private boolean mIsEncoderEOS;
    private boolean mDecoderStarted;
    private boolean mEncoderStarted;

    private OutputSurface mDecoderOutputSurfaceWrapper;
    private InputSurface mEncoderInputSurfaceWrapper;
    private IVideoRender mSurfaceRender;

    public VideoTrackTranscoder(MediaExtractor extractor, int trackIndex, MediaFormat outputFormat, QueuedMuxer muxer) {
        this(extractor, trackIndex, outputFormat, muxer, null);
    }

    public VideoTrackTranscoder(MediaExtractor extractor, int trackIndex, MediaFormat outputFormat, QueuedMuxer muxer,
                                IVideoRender render) {
        mExtractor = extractor;
        mTrackIndex = trackIndex;
        mOutputFormat = outputFormat;
        mMuxer = muxer;
        if (null != render) {
            mSurfaceRender = render;
        } else {
            mSurfaceRender = new DefaultVideoRender();
        }
    }

    @Override
    public void setup() throws IOException {
        mExtractor.selectTrack(mTrackIndex);
        mEncoder = MediaCodec.createEncoderByType(mOutputFormat.getString(MediaFormat.KEY_MIME));
        mEncoder.configure(mOutputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mEncoderInputSurfaceWrapper = new InputSurface(mEncoder.createInputSurface());
        mEncoder.start();
        mEncoderStarted = true;

        MediaFormat inputFormat = mExtractor.getTrackFormat(mTrackIndex);
//        if (inputFormat.containsKey(MediaUtil.KEY_ROTATION)) {
//            // Decoded video is rotated automatically in Android 5.0 lollipop.
//            // Turn off here because we don't want to encode rotated one.
//            // refer: https://android.googlesource
//            // .com/platform/frameworks/av/+blame/lollipop-release/media/libstagefright/Utils.cpp
//            inputFormat.setInteger(MediaUtil.KEY_ROTATION, 0);
//        }

        mDecoderOutputSurfaceWrapper = new OutputSurface(mSurfaceRender);
        mDecoder = MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME));
        mDecoder.configure(inputFormat, mDecoderOutputSurfaceWrapper.getSurface(), null, 0);
        mDecoder.start();
        mDecoderStarted = true;
    }

    @Override
    public boolean stepPipeline() {
        boolean busy = false;

        int status;
        while (drainEncoder() != DRAIN_STATE_NONE) busy = true;
        do {
            status = drainDecoder();
            if (status != DRAIN_STATE_NONE) busy = true;
            // NOTE: not repeating to keep from deadlock when encoder is full.
        } while (status == DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY);

        while (drainExtractor() != DRAIN_STATE_NONE) busy = true;

        return busy;
    }

    @Override
    public boolean isFinished() {
        return mIsEncoderEOS;
    }

    @Override
    public void release() {
        if (mDecoderOutputSurfaceWrapper != null) {
            mDecoderOutputSurfaceWrapper.release();
            mDecoderOutputSurfaceWrapper = null;
        }
        if (mEncoderInputSurfaceWrapper != null) {
            mEncoderInputSurfaceWrapper.release();
            mEncoderInputSurfaceWrapper = null;
        }
        if (mDecoder != null) {
            if (mDecoderStarted) mDecoder.stop();
            mDecoder.release();
            mDecoder = null;
        }
        if (mEncoder != null) {
            if (mEncoderStarted) mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
    }

    private int drainExtractor() {
        if (mIsExtractorEOS) return DRAIN_STATE_NONE;
        int trackIndex = mExtractor.getSampleTrackIndex();
        if (trackIndex >= 0 && trackIndex != mTrackIndex) {
            return DRAIN_STATE_NONE;
        }

        int result = mDecoder.dequeueInputBuffer(0);
        if (result < 0) return DRAIN_STATE_NONE;
        if (trackIndex < 0) {
            mIsExtractorEOS = true;
            mDecoder.queueInputBuffer(result, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            return DRAIN_STATE_NONE;
        }

        int sampleSize = mExtractor.readSampleData(CodecUtil.getInputBuffer(mDecoder, result), 0);
        boolean isKeyFrame = (mExtractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0;
        mDecoder.queueInputBuffer(result, 0, sampleSize, mExtractor.getSampleTime(),
                isKeyFrame ? MediaCodec.BUFFER_FLAG_SYNC_FRAME : 0);
        mExtractor.advance();
        return DRAIN_STATE_CONSUMED;
    }

    private int drainDecoder() {
        if (mIsDecoderEOS) return DRAIN_STATE_NONE;

        int result;
        try {
            result = mDecoder.dequeueOutputBuffer(mBufferInfo, 0);
        } catch (Exception e) {
            e.printStackTrace();

            result = MediaCodec.INFO_TRY_AGAIN_LATER;
        }
        switch (result) {
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                return DRAIN_STATE_NONE;
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
        }

        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            mEncoder.signalEndOfInputStream();
            mIsDecoderEOS = true;
            mBufferInfo.size = 0;
        }
        boolean doRender = (mBufferInfo.size > 0);
        // NOTE: doRender will block if buffer (of encoder) is full.
        // Refer: http://bigflake.com/mediacodec/CameraToMpegTest.java.txt
        mDecoder.releaseOutputBuffer(result, doRender);
        if (doRender) {
            mDecoderOutputSurfaceWrapper.awaitNewImage();
            mDecoderOutputSurfaceWrapper.drawImage(mBufferInfo.presentationTimeUs * 1000);
            mEncoderInputSurfaceWrapper.setPresentationTime(mBufferInfo.presentationTimeUs * 1000);
            // swapBuffers后之前的OpenGL操作会渲染到InputSurface绑定的Surface中
            mEncoderInputSurfaceWrapper.swapBuffers();
        }

        return DRAIN_STATE_CONSUMED;
    }

    private int drainEncoder() {
        if (mIsEncoderEOS) return DRAIN_STATE_NONE;

        int result;
        try {
            result = mEncoder.dequeueOutputBuffer(mBufferInfo, 0);
        } catch (Exception e) {
            e.printStackTrace();

            result = MediaCodec.INFO_TRY_AGAIN_LATER;
        }
        switch (result) {
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                return DRAIN_STATE_NONE;
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                if (mActualOutputFormat != null) {
                    throw new RuntimeException("Video output format changed twice.");
                }
                mActualOutputFormat = mEncoder.getOutputFormat();
                mMuxer.setOutputFormat(QueuedMuxer.SampleType.VIDEO, mActualOutputFormat);
                return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
        }

        if (mActualOutputFormat == null) {
            throw new RuntimeException("Could not determine actual output format.");
        }

        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            mIsEncoderEOS = true;
            mBufferInfo.set(0, 0, 0, mBufferInfo.flags);
        }
        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            // SPS or PPS, which should be passed by MediaFormat.
            mEncoder.releaseOutputBuffer(result, false);
            return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY;
        }
        mMuxer.writeSampleData(QueuedMuxer.SampleType.VIDEO, CodecUtil.getOutputBuffer(mEncoder, result), mBufferInfo);
        mEncoder.releaseOutputBuffer(result, false);
        return DRAIN_STATE_CONSUMED;
    }
}

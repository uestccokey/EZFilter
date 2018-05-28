package cn.ezandroid.ezfilter.media.transcode;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import cn.ezandroid.ezfilter.media.util.MediaUtil;

public class QueuedMuxer {

    private static final String TAG = "QueuedMuxer";
    // appropriate or not...
    private final MediaMuxer mMuxer;
    private MediaFormat mVideoFormat;
    private MediaFormat mAudioFormat;
    private int mVideoTrackIndex;
    private int mAudioTrackIndex;
    private ByteBuffer mByteBuffer;
    private final List<SampleInfo> mSampleInfoList;
    private boolean mStarted;

    public static final int TRACK_VIDEO = 0x01;
    public static final int TRACK_AUDIO = 0x10;

    private int mTrackCount = TRACK_VIDEO & TRACK_AUDIO;

    public QueuedMuxer(MediaMuxer muxer) {
        mMuxer = muxer;
        mSampleInfoList = new ArrayList<>();
    }

    /**
     * 设置轨道数量
     *
     * @param count 轨道类型：{@link #TRACK_VIDEO}或者{@link #TRACK_AUDIO}或者{@link #TRACK_VIDEO}&{@link #TRACK_AUDIO}
     */
    public void setTrackCount(int count) {
        mTrackCount = count;
    }

    public void setOutputFormat(SampleType sampleType, MediaFormat format) {
        switch (sampleType) {
            case VIDEO:
                mVideoFormat = format;
                break;
            case AUDIO:
                mAudioFormat = format;
                break;
            default:
                throw new AssertionError();
        }
        onSetOutputFormat();
    }

    private void onSetOutputFormat() {
        // 为了支持单独使用视频or音频，然而这里可能多次回调，所以必须过滤到不合法的调用
        switch (mTrackCount) {
            case TRACK_VIDEO:
                if (mVideoFormat == null) return;
                break;

            case TRACK_AUDIO:
                if (mAudioFormat == null) return;
                break;

            case TRACK_AUDIO & TRACK_VIDEO:
            default:
                if (mVideoFormat == null || mAudioFormat == null) return;
                break;
        }

        if (mVideoFormat != null) {
            mVideoTrackIndex = mMuxer.addTrack(mVideoFormat);
            Log.v(TAG, "Added track #" + mVideoTrackIndex + " with " + mVideoFormat.getString(MediaFormat.KEY_MIME) + " to muxer");
        }

        if (mAudioFormat != null) {
            mAudioTrackIndex = mMuxer.addTrack(mAudioFormat);
            Log.v(TAG, "Added track #" + mAudioTrackIndex + " with " + mAudioFormat.getString(MediaFormat.KEY_MIME) + " to muxer");
        }
        mMuxer.start();
        mStarted = true;

        if (mByteBuffer == null) {
            mByteBuffer = ByteBuffer.allocate(0);
        }
        mByteBuffer.flip();
//        Log.v(TAG, "Output format determined, writing " + mSampleInfoList.size() + " samples / " + mByteBuffer.limit() + " bytes to muxer.");
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int offset = 0;
        for (SampleInfo sampleInfo : mSampleInfoList) {
            sampleInfo.writeToBufferInfo(bufferInfo, offset);
            mMuxer.writeSampleData(getTrackIndexForSampleType(sampleInfo.mSampleType), mByteBuffer, bufferInfo);
            offset += sampleInfo.mSize;
        }
        mSampleInfoList.clear();
        mByteBuffer = null;
    }

    public void writeSampleData(SampleType sampleType, ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
        if (mStarted) {
            mMuxer.writeSampleData(getTrackIndexForSampleType(sampleType), byteBuf, bufferInfo);
            return;
        }
        byteBuf.limit(bufferInfo.offset + bufferInfo.size);
        byteBuf.position(bufferInfo.offset);
        if (mByteBuffer == null) {
            mByteBuffer = ByteBuffer.allocate(MediaUtil.BUFFER_SIZE);
        }
        mByteBuffer.put(byteBuf);
        mSampleInfoList.add(new SampleInfo(sampleType, bufferInfo.size, bufferInfo));
    }

    private int getTrackIndexForSampleType(SampleType sampleType) {
        switch (sampleType) {
            case VIDEO:
                return mVideoTrackIndex;
            case AUDIO:
                return mAudioTrackIndex;
            default:
                throw new AssertionError();
        }
    }

    public enum SampleType {VIDEO, AUDIO}

    private static class SampleInfo {
        private final SampleType mSampleType;
        private final int mSize;
        private final long mPresentationTimeUs;
        private final int mFlags;

        private SampleInfo(SampleType sampleType, int size, MediaCodec.BufferInfo bufferInfo) {
            mSampleType = sampleType;
            mSize = size;
            mPresentationTimeUs = bufferInfo.presentationTimeUs;
            mFlags = bufferInfo.flags;
        }

        private void writeToBufferInfo(MediaCodec.BufferInfo bufferInfo, int offset) {
            bufferInfo.set(offset, mSize, mPresentationTimeUs, mFlags);
        }
    }
}

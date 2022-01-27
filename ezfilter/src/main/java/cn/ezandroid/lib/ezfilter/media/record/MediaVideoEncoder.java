package cn.ezandroid.lib.ezfilter.media.record;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;

import cn.ezandroid.lib.ezfilter.media.util.MediaUtil;

/**
 * 视频编码器
 */
public class MediaVideoEncoder extends MediaEncoder {

    private static final String TAG = "MediaVideoEncoder";

    private static final String MIME_TYPE = "video/avc";

    private final int mWidth;
    private final int mHeight;
    private RenderHandler mRenderHandler;
    private Surface mSurface;

    public MediaVideoEncoder(MediaMuxerWrapper muxer, MediaEncoderListener listener,
                             int width, int height) {
        super(muxer, listener);
        mWidth = width;
        mHeight = height;
        mRenderHandler = RenderHandler.createHandler(TAG);
    }

    @Override
    public boolean frameAvailableSoon() {
        boolean result;
        if (result = super.frameAvailableSoon()) {
            mRenderHandler.draw();
        }
        return result;
    }

    @Override
    protected void prepare() throws IOException, IllegalStateException {
        mTrackIndex = -1;
        mMuxerStarted = mIsEOS = false;

        final MediaCodecInfo videoCodecInfo = selectVideoCodec(MIME_TYPE);
        if (videoCodecInfo == null) {
            Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
            return;
        }

        // 视频Format
        final MediaFormat videoFormat = MediaUtil.createVideoFormat(mWidth, mHeight);

        mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mMediaCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        // get Surface for encoder input
        // this method only can call between #configure and #start
        mSurface = mMediaCodec.createInputSurface();
        mMediaCodec.start();
        if (mListener != null) {
            try {
                mListener.onPrepared(this);
            } catch (final Exception e) {
                Log.e(TAG, "prepare:", e);
            }
        }
    }

    /**
     * 设置输入纹理
     * <p>
     * 必须在GL线程调用
     *
     * @param texId
     */
    public void setInputTextureId(final int texId) {
        mRenderHandler.setInputTextureId(mSurface, texId);
    }

    /**
     * 获取输入的纹理
     *
     * @return
     */
    public int getInputTextureId() {
        return mRenderHandler.getInputTextureId();
    }

    @Override
    protected void release() {
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        if (mRenderHandler != null) {
            mRenderHandler.release();
            mRenderHandler = null;
        }
        super.release();
    }

    private static MediaCodecInfo selectVideoCodec(final String mimeType) {
        // get the list of available codecs
        final int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {    // skipp decoder
                continue;
            }
            // select first codec that match a specific MIME type and color format
            for (String type : codecInfo.getSupportedTypes()) {
                if (type.equalsIgnoreCase(mimeType)) {
                    final int format = selectColorFormat(codecInfo, mimeType);
                    if (format > 0) {
                        return codecInfo;
                    }
                }
            }
        }
        return null;
    }

    private static int selectColorFormat(final MediaCodecInfo codecInfo, final String mimeType) {
        int result = 0;
        final MediaCodecInfo.CodecCapabilities caps;
        try {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            caps = codecInfo.getCapabilitiesForType(mimeType);
        } finally {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
        }
        for (int colorFormat : caps.colorFormats) {
            if (isRecognizedVideoFormat(colorFormat)) {
                result = colorFormat;
                break;
            }
        }
        if (result == 0) {
            Log.e(TAG, "couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
        }
        return result;
    }

    private static int[] sRecognizedFormats = new int[]{
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
    };

    private static boolean isRecognizedVideoFormat(final int colorFormat) {
        final int n = sRecognizedFormats != null ? sRecognizedFormats.length : 0;
        for (int i = 0; i < n; i++) {
            if (sRecognizedFormats[i] == colorFormat) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void signalEndOfInputStream() {
        try {
            mMediaCodec.signalEndOfInputStream();
        } catch (final Exception e) {
            e.printStackTrace();
        }
        mIsEOS = true;
    }
}

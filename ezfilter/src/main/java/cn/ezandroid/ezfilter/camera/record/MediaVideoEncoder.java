package cn.ezandroid.ezfilter.camera.record;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.opengl.EGL14;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;

/**
 * 视频编码器
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MediaVideoEncoder extends MediaEncoder {

    private static final String TAG = "MediaVideoEncoder";

    private static final String MIME_TYPE = "video/avc";
    private static final int FRAME_RATE = 20;
    private static final int FRAME_INTERVAL = 1;

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

    /**
     * 进行整除16对齐
     * <p>
     * MediaCodec这个API在设计的时候，过于贴近HAL层，这在很多Soc的实现上，是直接把传入MediaCodec的buffer，
     * 在不经过任何前置处理的情况下就直接送入了Soc中。 而在编码h264视频流的时候，由于h264的编码块大小一般是16x16，
     * 于是乎在一开始设置视频的宽高的时候，如果设置了一个没有对齐16的大小，例如960x540， 在某些cpu上，
     * 最终编码出来的视频就会直接花屏。
     *
     * @param size
     * @return
     */
    private static int align16(int size) {
        if (size % 16 > 0) {
            size = (size / 16) * 16 + 16;
        }
        return size;
    }

    @Override
    protected void prepare() throws IOException {
        mTrackIndex = -1;
        mMuxerStarted = mIsEOS = false;

        final MediaCodecInfo videoCodecInfo = selectVideoCodec(MIME_TYPE);
        if (videoCodecInfo == null) {
            Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
            return;
        }

        // FIXME 对齐后可能有几个像素的拉伸，暂时没有更好的方案
        final MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, align16(mWidth), align16(mHeight));
        // 数据来源
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        // 视频bit率
        format.setInteger(MediaFormat.KEY_BIT_RATE, (int) (0.2 * FRAME_RATE * mWidth * mHeight));
        // 帧率
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        // 设置关键帧时间间隔（单位为秒）表示：每隔多长时间有一个关键帧
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, FRAME_INTERVAL);

        mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
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
        mRenderHandler.setInputTextureId(EGL14.eglGetCurrentContext(), mSurface, texId);
    }

    /**
     * 获取输入纹理
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

    private static int[] recognizedFormats = new int[]{
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
    };

    private static boolean isRecognizedVideoFormat(final int colorFormat) {
        final int n = recognizedFormats != null ? recognizedFormats.length : 0;
        for (int i = 0; i < n; i++) {
            if (recognizedFormats[i] == colorFormat) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void signalEndOfInputStream() {
        mMediaCodec.signalEndOfInputStream();
        mIsEOS = true;
    }
}

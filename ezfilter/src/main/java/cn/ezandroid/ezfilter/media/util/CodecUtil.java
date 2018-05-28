package cn.ezandroid.ezfilter.media.util;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * CodecUtil
 *
 * @author like
 * @date 2018-03-13
 */
public class CodecUtil {

    private static final String TAG = "CodecUtil";

    /** 目前支持的两种颜色格式 */
    private static int[] sRecognizedFormats = new int[]{
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
    };

    /**
     * 防止出现 http://stackoverflow.com/q/30646885 的问题
     *
     * @param codec
     * @param index
     * @return
     */
    public static ByteBuffer getInputBuffer(MediaCodec codec, int index) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return codec.getInputBuffer(index);
        } else {
            return codec.getInputBuffers()[index];
        }
    }

    /**
     * 防止出现 http://stackoverflow.com/q/30646885 的问题
     *
     * @param codec
     * @param index
     * @return
     */
    public static ByteBuffer getOutputBuffer(MediaCodec codec, int index) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return codec.getOutputBuffer(index);
        } else {
            return codec.getOutputBuffers()[index];
        }
    }

    /**
     * 自动选择合适的颜色模式
     *
     * @return
     */
    public static int selectColorFormat() {
        return selectColorFormat(selectVideoCodec(MediaUtil.MIME_TYPE_MP4), MediaUtil.MIME_TYPE_MP4);
    }

    private static MediaCodecInfo selectVideoCodec(final String mimeType) {
        int count = MediaCodecList.getCodecCount();
        for (int i = 0; i < count; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                // skipp decoder
                continue;
            }

            // select first codec that match a specific MIME type and color format
            final String[] types = codecInfo.getSupportedTypes();
            for (String type : types) {
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

    private static int selectColorFormat(MediaCodecInfo codecInfo, String mimeType) {
        int result = 0;
        final MediaCodecInfo.CodecCapabilities capabilities;
        try {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            capabilities = codecInfo.getCapabilitiesForType(mimeType);
        } finally {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
        }

        int colorFormat;
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            colorFormat = capabilities.colorFormats[i];
            if (isRecognizedViewFormat(colorFormat)) {
                result = colorFormat;
                break;
            }
        }
        if (result == 0) {
            Log.e(TAG, "couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
        }
        return result;
    }

    private static boolean isRecognizedViewFormat(int colorFormat) {
        final int n = sRecognizedFormats != null ? sRecognizedFormats.length : 0;
        for (int i = 0; i < n; i++) {
            if (sRecognizedFormats[i] == colorFormat) {
                return true;
            }
        }
        return false;
    }
}

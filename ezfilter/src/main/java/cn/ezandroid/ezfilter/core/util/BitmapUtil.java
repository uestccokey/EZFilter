package cn.ezandroid.ezfilter.core.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * 图片工具类
 *
 * @author like
 * @date 2017-08-10
 */
public class BitmapUtil {

    public enum Scheme {
        FILE("file://"), // file:// + /sdcard/demo.mp4
        ASSETS("file:///android_asset/"), // file:///android_asset/ + demo.mp4
        DRAWABLE("drawable://"), // drawable:// + 3243342
        UNKNOWN("");

        private String mScheme;

        private Scheme(String scheme) {
            this.mScheme = scheme;
        }

        public static Scheme ofUri(String uri) {
            if (uri != null) {
                Scheme[] var1 = values();
                for (Scheme s : var1) {
                    if (s.belongsTo(uri)) {
                        return s;
                    }
                }
            }

            return UNKNOWN;
        }

        private boolean belongsTo(String uri) {
            return uri.toLowerCase(Locale.US).startsWith(this.mScheme);
        }

        public String wrap(String path) {
            return this.mScheme + path;
        }

        public String crop(String uri) {
            if (!this.belongsTo(uri)) {
                throw new IllegalArgumentException(String.format("URI [%1$s] doesn't have expected mScheme [%2$s]", new Object[]{uri, this.mScheme}));
            } else {
                return uri.substring(this.mScheme.length());
            }
        }
    }

    /**
     * 加载图片
     *
     * @param context
     * @param path
     * @return
     */
    public static Bitmap loadBitmap(Context context, String path) {
        InputStream in = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            options.inDither = false;
            options.inInputShareable = true;
            options.inPurgeable = true;
            if (Scheme.ASSETS.belongsTo(path)) {
                in = context.getAssets().open(Scheme.ASSETS.crop(path));
                if (in != null) {
                    return BitmapFactory.decodeStream(in, null, options);
                }
            } else if (Scheme.FILE.belongsTo(path)) {
                return BitmapFactory.decodeFile(Scheme.FILE.crop(path), options);
            } else if (Scheme.DRAWABLE.belongsTo(path)) {
                return BitmapFactory.decodeResource(context.getResources(),
                        Integer.parseInt(Scheme.DRAWABLE.crop(path)), options);
            } else {
                return BitmapFactory.decodeFile(path, options);
            }
        } catch (IOException | OutOfMemoryError e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 有大小限制的加载图片
     *
     * @param context
     * @param path
     * @param width   最大宽度
     * @param height  最大高度
     * @return
     */
    public static Bitmap loadBitmap(Context context, String path, int width, int height) {
        InputStream in = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            if (Scheme.ASSETS.belongsTo(path)) {
                in = context.getAssets().open(Scheme.ASSETS.crop(path));
                if (in != null) {
                    BitmapFactory.decodeStream(in, null, options);
                }
            } else if (Scheme.FILE.belongsTo(path)) {
                BitmapFactory.decodeFile(Scheme.FILE.crop(path), options);
            } else if (Scheme.DRAWABLE.belongsTo(path)) {
                BitmapFactory.decodeResource(context.getResources(),
                        Integer.parseInt(Scheme.DRAWABLE.crop(path)), options);
            } else {
                BitmapFactory.decodeFile(path, options);
            }
            int outWidth = options.outWidth;
            int outHeight = options.outHeight;
            int sampleSize = 1;
            while (outWidth / sampleSize > width || outHeight / sampleSize > height) {
                sampleSize++;
            }
            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize;
            if (Scheme.ASSETS.belongsTo(path)) {
                in = context.getAssets().open(Scheme.ASSETS.crop(path));
                if (in != null) {
                    return BitmapFactory.decodeStream(in, null, options);
                }
            } else if (Scheme.FILE.belongsTo(path)) {
                return BitmapFactory.decodeFile(Scheme.FILE.crop(path), options);
            } else if (Scheme.DRAWABLE.belongsTo(path)) {
                return BitmapFactory.decodeResource(context.getResources(),
                        Integer.parseInt(Scheme.DRAWABLE.crop(path)), options);
            } else {
                return BitmapFactory.decodeFile(path, options);
            }
        } catch (IOException | OutOfMemoryError e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 将bitmap绑定为纹理
     *
     * @param bitmap
     * @return
     */
    public static int bindBitmap(Bitmap bitmap) {
        int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex[0]);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        return tex[0];
    }
}

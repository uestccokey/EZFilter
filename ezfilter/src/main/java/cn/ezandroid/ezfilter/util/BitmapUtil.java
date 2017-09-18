package cn.ezandroid.ezfilter.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * 图片工具类
 *
 * @author like
 * @date 2017-08-10
 */
public class BitmapUtil {

    /**
     * 加载图片
     *
     * @param context
     * @param path
     * @return
     */
    public static Bitmap loadImage(Context context, String path) {
        InputStream in = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            options.inDither = false;
            options.inInputShareable = true;
            options.inPurgeable = true;
            if (path.startsWith(PathPrefix.PREFIX_ASSETS)) {
                in = context.getAssets().open(path.replace(PathPrefix.PREFIX_ASSETS, ""));
                if (in != null) {
                    return BitmapFactory.decodeStream(in, null, options);
                }
            } else if (path.startsWith(PathPrefix.PREFIX_FILE)) {
                return BitmapFactory.decodeFile(path.replace(PathPrefix.PREFIX_FILE, ""), options);
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
    public static Bitmap loadImage(Context context, String path, int width, int height) {
        InputStream in = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            if (path.startsWith(PathPrefix.PREFIX_ASSETS)) {
                in = context.getAssets().open(path.replace(PathPrefix.PREFIX_ASSETS, ""));
                if (in != null) {
                    BitmapFactory.decodeStream(in, null, options);
                }
            } else if (path.startsWith(PathPrefix.PREFIX_FILE)) {
                BitmapFactory.decodeFile(path.replace(PathPrefix.PREFIX_FILE, ""), options);
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
            if (path.startsWith(PathPrefix.PREFIX_ASSETS)) {
                in = context.getAssets().open(path.replace(PathPrefix.PREFIX_ASSETS, ""));
                if (in != null) {
                    return BitmapFactory.decodeStream(in, null, options);
                }
            } else if (path.startsWith(PathPrefix.PREFIX_FILE)) {
                return BitmapFactory.decodeFile(path.replace(PathPrefix.PREFIX_FILE, ""), options);
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
}

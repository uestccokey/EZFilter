package cn.ezandroid.ezfilter.core.cache;

import android.graphics.Bitmap;
import android.util.LruCache;

/**
 * LRU图片缓存
 *
 * @author like
 * @date 2017-08-03
 */
public class LruBitmapCache implements IBitmapCache {

    private static LruBitmapCache sInstance = new LruBitmapCache((int) (Runtime.getRuntime().maxMemory() / 8));

    private LruCache<String, Bitmap> mLruCache;

    public LruBitmapCache(int size) {
        mLruCache = new LruCache<String, Bitmap>(size) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };
    }

    public static LruBitmapCache getSingleInstance() {
        return sInstance;
    }

    @Override
    public Bitmap get(String key) {
        return mLruCache.get(key);
    }

    @Override
    public void put(String key, Bitmap bitmap) {
        mLruCache.put(key, bitmap);
    }

    @Override
    public void clear() {
        mLruCache.evictAll();
    }
}

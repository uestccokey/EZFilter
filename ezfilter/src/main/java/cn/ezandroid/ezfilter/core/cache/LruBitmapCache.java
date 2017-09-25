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

    private LruCache<String, Bitmap> mLruCache;

    public LruBitmapCache(int size) {
        mLruCache = new LruCache<String, Bitmap>(size) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }

//            @Override
//            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
//                Log.e("LruBitmapCache", "entryRemoved:" + key + " " + sizeOf(key, oldValue) + " " + size() + " " + maxSize());
//            }
        };
    }

    @Override
    public Bitmap get(String path) {
        return mLruCache.get(path);
    }

    @Override
    public void put(String path, Bitmap bitmap) {
        mLruCache.put(path, bitmap);
    }

    @Override
    public void clear() {
        mLruCache.evictAll();
    }
}

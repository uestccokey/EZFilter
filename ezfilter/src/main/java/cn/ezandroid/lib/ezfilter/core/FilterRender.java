package cn.ezandroid.lib.ezfilter.core;

import cn.ezandroid.lib.ezfilter.core.cache.IBitmapCache;
import cn.ezandroid.lib.ezfilter.core.cache.LruBitmapCache;

/**
 * 滤镜渲染器
 * <p>
 * 所有滤镜的父类
 * 继承自FBORender，支持设置图片缓存
 *
 * @author like
 * @date 2017-09-15
 */
public class FilterRender extends FBORender {

    protected IBitmapCache mBitmapCache = LruBitmapCache.getSingleInstance();

    /**
     * 设置图片缓存
     *
     * @param bitmapCache
     */
    public void setBitmapCache(IBitmapCache bitmapCache) {
        mBitmapCache = bitmapCache;
    }

    /**
     * 获取图片缓存
     *
     * @return
     */
    public IBitmapCache getBitmapCache() {
        return mBitmapCache;
    }
}

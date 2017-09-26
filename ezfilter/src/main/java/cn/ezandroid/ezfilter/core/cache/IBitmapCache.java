package cn.ezandroid.ezfilter.core.cache;

import android.graphics.Bitmap;

/**
 * 图片缓存接口
 * <p>
 * 用来缓存滤镜用到的纹理图片，加快切换滤镜时的渲染速度
 *
 * @author like
 * @date 2017-09-18
 */
public interface IBitmapCache {

    Bitmap get(String key);

    void put(String key, Bitmap bitmap);

    void clear();
}

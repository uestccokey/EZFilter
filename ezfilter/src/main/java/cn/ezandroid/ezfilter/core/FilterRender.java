package cn.ezandroid.ezfilter.core;

import cn.ezandroid.ezfilter.cache.IBitmapCache;

/**
 * 滤镜渲染器
 * <p>
 * 所有滤镜的父类
 * 继承自FBORender，支持滤镜渲染结果作为纹理输出
 * 实现了OnTextureAvailableListener接口，支持接受纹理作为输入进行渲染
 *
 * @author like
 * @date 2017-09-15
 */
public class FilterRender extends FBORender implements OnTextureAvailableListener {

    protected IBitmapCache mBitmapCache;

    public void setBitmapCache(IBitmapCache bitmapCache) {
        mBitmapCache = bitmapCache;
    }

    @Override
    public void onTextureAvailable(int texture, FBORender source) {
        mTextureIn = texture;
        setWidth(source.getWidth());
        setHeight(source.getHeight());
        onDrawFrame();
    }
}

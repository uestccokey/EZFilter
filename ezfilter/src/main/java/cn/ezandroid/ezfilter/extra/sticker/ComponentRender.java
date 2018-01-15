package cn.ezandroid.ezfilter.extra.sticker;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import cn.ezandroid.ezfilter.core.cache.IBitmapCache;
import cn.ezandroid.ezfilter.core.util.BitmapUtil;
import cn.ezandroid.ezfilter.extra.sticker.model.Component;

/**
 * 贴纸组件渲染器
 *
 * @author like
 * @date 2018-01-09
 */
public class ComponentRender {

    private Context mContext;
    private Component mComponent;
    private IBitmapCache mBitmapCache;

    private int mLastIndex;

    private long mStartTime = -1;

    public ComponentRender(Context context, Component component) {
        mContext = context;
        mComponent = component;
    }

    public void setBitmapCache(IBitmapCache bitmapCache) {
        mBitmapCache = bitmapCache;
    }

    /**
     * 在GL线程调用
     */
    public void onDraw() {
        if (mStartTime == -1) {
            mStartTime = System.currentTimeMillis();
        }
        long currentTime = System.currentTimeMillis();
        long position = (currentTime - mStartTime) % mComponent.duration;
        // 如mComponent.duration=3000，mComponent.length=60，position=1000，则currentIndex=20
        int currentIndex = Math.round((mComponent.length - 1) * 1.0f / mComponent.duration * position);

        String path = mComponent.resources.get(currentIndex);
        Bitmap bitmap = mBitmapCache.get(path);
        if (bitmap == null) {
            bitmap = BitmapUtil.loadBitmap(mContext, path);
            if (bitmap != null) {
                mBitmapCache.put(path, bitmap);
            } else {
                return;
            }
        }

        // TODO 绘制组件
        Log.e("ComponentRender", mComponent + ":" + path);

        mLastIndex = currentIndex;
    }
}

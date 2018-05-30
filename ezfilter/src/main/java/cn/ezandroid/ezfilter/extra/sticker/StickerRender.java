package cn.ezandroid.ezfilter.extra.sticker;

import android.content.Context;
import android.opengl.GLES20;

import java.util.ArrayList;
import java.util.List;

import cn.ezandroid.ezfilter.core.FilterRender;
import cn.ezandroid.ezfilter.core.cache.IBitmapCache;
import cn.ezandroid.ezfilter.extra.sticker.model.Component;
import cn.ezandroid.ezfilter.extra.sticker.model.ScreenAnchor;
import cn.ezandroid.ezfilter.extra.sticker.model.Sticker;

/**
 * 贴纸渲染器
 *
 * @author like
 * @date 2018-01-05
 */
public class StickerRender extends FilterRender {

    protected Context mContext;

    // 贴纸模型
    protected Sticker mSticker;

    // 锚点组
    protected ScreenAnchor mScreenAnchor;

    // 组件绘制器列表
    protected List<ComponentRender> mComponentRenders = new ArrayList<>();

    public StickerRender(Context context) {
        mContext = context;
    }

    public Sticker getSticker() {
        return mSticker;
    }

    /**
     * 设置贴纸模型
     *
     * @param sticker
     */
    public void setSticker(Sticker sticker) {
        mSticker = sticker;

        mComponentRenders.clear();
        for (Component component : mSticker.components) {
            ComponentRender componentRender = new ComponentRender(mContext, component);
            componentRender.setBitmapCache(mBitmapCache);
            mComponentRenders.add(componentRender);
        }
    }

    /**
     * 设置显示锚点
     *
     * @param screenAnchor
     */
    public void setScreenAnchor(ScreenAnchor screenAnchor) {
        mScreenAnchor = screenAnchor;
    }

    @Override
    public void destroy() {
        super.destroy();

        for (ComponentRender componentRender : mComponentRenders) {
            componentRender.destroy();
        }
    }

    @Override
    public void setBitmapCache(IBitmapCache bitmapCache) {
        super.setBitmapCache(bitmapCache);

        for (ComponentRender componentRender : mComponentRenders) {
            componentRender.setBitmapCache(bitmapCache);
        }
    }

    @Override
    protected void onRenderSizeChanged() {
        super.onRenderSizeChanged();
        if (mScreenAnchor != null) {
            mScreenAnchor.width = mWidth;
            mScreenAnchor.height = mHeight;
        }
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        // 开启混合
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        for (ComponentRender componentRender : mComponentRenders) {
            componentRender.setScreenAnchor(mScreenAnchor);
            componentRender.updateRenderVertices(getWidth(), getHeight());
            componentRender.onDraw(mTextureHandle, mPositionHandle, mTextureCoordHandle, mTextureVertices[2]);
        }

        GLES20.glDisable(GLES20.GL_BLEND);
    }
}

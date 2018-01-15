package cn.ezandroid.ezfilter.extra.sticker;

import android.content.Context;
import android.opengl.GLES20;

import java.util.ArrayList;
import java.util.List;

import cn.ezandroid.ezfilter.core.FilterRender;
import cn.ezandroid.ezfilter.core.cache.IBitmapCache;
import cn.ezandroid.ezfilter.extra.sticker.model.AnchorGroup;
import cn.ezandroid.ezfilter.extra.sticker.model.Component;
import cn.ezandroid.ezfilter.extra.sticker.model.Sticker;

/**
 * 贴纸渲染器
 *
 * @author like
 * @date 2018-01-05
 */
public class StickerRender extends FilterRender {

    private Context mContext;

    // 贴纸模型
    private Sticker mSticker;

    // 锚点组列表
    private List<AnchorGroup> mAnchorGroupList = new ArrayList<>();

    // 组件绘制器列表
    private List<ComponentRender> mComponentRenders = new ArrayList<>();

    public StickerRender(Context context) {
        mContext = context;
    }

    public Sticker getSticker() {
        return mSticker;
    }

    public void setSticker(Sticker sticker) {
        mSticker = sticker;

        mComponentRenders.clear();
        for (Component component : mSticker.components) {
            mComponentRenders.add(new ComponentRender(mContext, component));
        }
    }

    public void setAnchorGroupList(List<AnchorGroup> anchorGroupList) {
        mAnchorGroupList = anchorGroupList;
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
    protected void onDraw() {
        super.onDraw();

        // 开启混合
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        for (ComponentRender componentRender : mComponentRenders) {
            componentRender.onDraw(mTextureHandle, mPositionHandle, mTextureCoordHandle, mTextureVertices[2]);
        }

        GLES20.glDisable(GLES20.GL_BLEND);
    }
}

package cn.ezandroid.lib.ezfilter.image;

import android.graphics.Bitmap;

import cn.ezandroid.lib.ezfilter.EZFilter;
import cn.ezandroid.lib.ezfilter.core.FBORender;
import cn.ezandroid.lib.ezfilter.core.FilterRender;
import cn.ezandroid.lib.ezfilter.core.environment.IFitView;
import cn.ezandroid.lib.ezfilter.extra.IAdjustable;
import cn.ezandroid.lib.ezfilter.image.offscreen.OffscreenImage;

/**
 * 图片处理构造器
 *
 * @author like
 * @date 2017-09-15
 */
public class BitmapBuilder extends EZFilter.Builder {

    private Bitmap mBitmap;

    private BitmapInput mBitmapInput;

    public BitmapBuilder(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    public Bitmap output() {
        // 离屏渲染
        OffscreenImage offscreenImage = new OffscreenImage(mBitmap);
        for (FilterRender filterRender : mFilterRenders) {
            offscreenImage.addFilterRender(filterRender);
        }
        return offscreenImage.capture();
    }

    public Bitmap output(int width, int height) {
        // 离屏渲染
        OffscreenImage offscreenImage = new OffscreenImage(mBitmap);
        for (FilterRender filterRender : mFilterRenders) {
            offscreenImage.addFilterRender(filterRender);
        }
        return offscreenImage.capture(width, height);
    }

    @Override
    public FBORender getStartPointRender(IFitView view) {
        if (mBitmapInput == null) {
            mBitmapInput = new BitmapInput(mBitmap);
        }
        return mBitmapInput;
    }

    @Override
    public float getAspectRatio(IFitView view) {
        return mBitmap.getWidth() * 1.0f / mBitmap.getHeight();
    }

    @Override
    public BitmapBuilder addFilter(FilterRender filterRender) {
        return (BitmapBuilder) super.addFilter(filterRender);
    }

    @Override
    public <T extends FilterRender & IAdjustable> BitmapBuilder addFilter(T filterRender, float progress) {
        return (BitmapBuilder) super.addFilter(filterRender, progress);
    }

    @Override
    public BitmapBuilder enableRecord(String outputPath, boolean recordVideo, boolean recordAudio) {
        return (BitmapBuilder) super.enableRecord(outputPath, recordVideo, recordAudio);
    }
}

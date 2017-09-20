package cn.ezandroid.ezfilter.demo;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.Surface;
import android.widget.LinearLayout;

import cn.ezandroid.ezfilter.view.IGLView;
import cn.ezandroid.ezfilter.view.IRender;

/**
 * GLLinearLayout
 *
 * @author like
 * @date 2017-09-20
 */
public class GLLinearLayout extends LinearLayout implements IGLView {

    private Surface mSurface;
    private IRender mRender;

    public GLLinearLayout(Context context) {
        super(context);
    }

    public GLLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setSurface(Surface surface) {
        mSurface = surface;
    }

    @Override
    public void setRender(IRender render) {
        mRender = render;
    }

    @Override
    public void draw(Canvas canvas) {
        if (mSurface != null) {
            Canvas surfaceCanvas = mSurface.lockCanvas(null);
            super.draw(surfaceCanvas);
            mSurface.unlockCanvasAndPost(surfaceCanvas);

            if (mRender != null) {
                mRender.requestRender();
            }
            invalidate();
        } else {
            super.draw(canvas);
        }
    }
}

package cn.ezandroid.ezfilter.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

import cn.ezandroid.ezfilter.environment.IGLEnvironment;

/**
 * GLRelativeLayout
 *
 * @author like
 * @date 2017-09-21
 */
public class GLRelativeLayout extends RelativeLayout implements IGLView {

    private GLViewHelper mGLViewHelper = new GLViewHelper();

    public GLRelativeLayout(Context context) {
        super(context);
        init();
    }

    public GLRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        ViewTreeObserver observer = getViewTreeObserver();
        if (observer != null) {
            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (isDirty()) {
                        // 只在需要绘制的时候刷新
                        invalidate();
                    }
                    return true;
                }
            });
        }
    }

    @Override
    public void setSurface(Surface surface) {
        mGLViewHelper.setSurface(surface);
    }

    @Override
    public void setGLEnvironment(IGLEnvironment render) {
        mGLViewHelper.setGLEnvironment(render);
    }

    @Override
    public void draw(Canvas canvas) {
        Canvas surfaceCanvas = mGLViewHelper.drawStart(canvas);
        if (surfaceCanvas == null) {
            super.draw(canvas);
        } else {
            super.draw(surfaceCanvas);
            mGLViewHelper.drawEnd(surfaceCanvas);
        }
    }
}

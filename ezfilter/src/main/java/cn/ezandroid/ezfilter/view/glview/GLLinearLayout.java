package cn.ezandroid.ezfilter.view.glview;

import android.content.Context;
import android.graphics.Canvas;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import cn.ezandroid.ezfilter.core.environment.IGLEnvironment;

/**
 * 支持OpenGL渲染的绝对布局
 *
 * @author like
 * @date 2017-09-21
 */
public class GLLinearLayout extends LinearLayout implements IGLView {

    private GLViewHelper mGLViewHelper = new GLViewHelper();

    private int mRenderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY;

    public GLLinearLayout(Context context) {
        super(context);
        init();
    }

    public GLLinearLayout(Context context, AttributeSet attrs) {
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
                        if (mRenderMode == GLSurfaceView.RENDERMODE_WHEN_DIRTY) {
                            invalidate();
                        }
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
    public void setRenderMode(int model) {
        mRenderMode = model;
    }

    @Override
    public void draw(Canvas canvas) {
        Canvas surfaceCanvas = mGLViewHelper.drawStart(canvas);
        if (surfaceCanvas == null) {
            super.draw(canvas);
        } else {
            super.draw(surfaceCanvas);
            mGLViewHelper.drawEnd(surfaceCanvas);
            if (mRenderMode == GLSurfaceView.RENDERMODE_CONTINUOUSLY) {
                invalidate();
            }
        }
    }
}

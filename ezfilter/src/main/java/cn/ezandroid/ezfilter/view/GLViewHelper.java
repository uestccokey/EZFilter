package cn.ezandroid.ezfilter.view;

import android.graphics.Canvas;
import android.view.Surface;

import cn.ezandroid.ezfilter.environment.IGLEnvironment;

/**
 * GLViewHelper
 *
 * @author like
 * @date 2017-09-21
 */
public class GLViewHelper {

    private Surface mSurface;
    private IGLEnvironment mRender;

    public void setSurface(Surface surface) {
        mSurface = surface;
    }

    public void setGLEnvironment(IGLEnvironment render) {
        mRender = render;
    }

    public Canvas drawStart(Canvas canvas) {
        if (mSurface != null) {
            return mSurface.lockCanvas(null);
        }
        return null;
    }

    public void drawEnd(Canvas surfaceCanvas) {
        if (mSurface != null && surfaceCanvas != null) {
            mSurface.unlockCanvasAndPost(surfaceCanvas);

            if (mRender != null) {
                mRender.requestRender();
            }
        }
    }
}

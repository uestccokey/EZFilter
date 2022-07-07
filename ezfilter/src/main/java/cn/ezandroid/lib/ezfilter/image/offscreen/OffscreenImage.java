package cn.ezandroid.lib.ezfilter.image.offscreen;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.os.Build;

import java.nio.IntBuffer;
import java.util.List;

import cn.ezandroid.lib.ezfilter.core.FBORender;
import cn.ezandroid.lib.ezfilter.core.GLRender;
import cn.ezandroid.lib.ezfilter.core.RenderPipeline;
import cn.ezandroid.lib.ezfilter.core.environment.EGLEnvironment;
import cn.ezandroid.lib.ezfilter.image.BitmapInput;

import static javax.microedition.khronos.opengles.GL10.GL_RGBA;
import static javax.microedition.khronos.opengles.GL10.GL_UNSIGNED_BYTE;

/**
 * 离屏渲染图片
 *
 * @author like
 * @date 2017-09-18
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class OffscreenImage {

    private RenderPipeline mPipeline;

    private EGLEnvironment mEgl;
    private EGLEnvironment.EglSurface mInputSurface;

    private int mWidth;
    private int mHeight;

    private Bitmap mBitmap;

    public OffscreenImage(Bitmap bitmap) {
        mBitmap = bitmap;

        initRenderSize();
        initPipeline();
    }

    private void initRenderSize() {
        mWidth = mBitmap.getWidth();
        mHeight = mBitmap.getHeight();

        // 初始化EGL环境
        mEgl = new EGLEnvironment(EGL14.eglGetCurrentContext(), false);
        // 创建离屏缓冲
        mInputSurface = mEgl.createOffscreen(mWidth, mHeight);
        // 设置渲染环境可用
        mInputSurface.makeCurrent();
    }

    private void initPipeline() {
        BitmapInput bitmapInput = new BitmapInput(mBitmap);
        mPipeline = new RenderPipeline();
        mPipeline.onSurfaceCreated(null, null);
        mPipeline.setStartPointRender(bitmapInput);
        mPipeline.addEndPointRender(new GLRender());
    }

    public void addFilterRender(FBORender filterRender) {
        mPipeline.addFilterRender(filterRender);
    }

    public void removeFilterRender(FBORender filterRender) {
        mPipeline.removeFilterRender(filterRender);
    }

    public List<FBORender> getFilterRenders() {
        return mPipeline.getFilterRenders();
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public Bitmap capture(int width, int height) {
        mPipeline.onSurfaceChanged(null, width, height);
        mPipeline.startRender();
        mPipeline.onDrawFrame(null);

        int[] iat = new int[mWidth * mHeight];
        IntBuffer ib = IntBuffer.allocate(width * height);
        GLES20.glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, ib);

        int[] ia = ib.array();
        for (int i = 0; i < mHeight; i++) {
            System.arraycopy(ia, i * mWidth, iat, (mHeight - i - 1) * mWidth, mWidth);
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(IntBuffer.wrap(iat));

        mPipeline.onSurfaceDestroyed();

        // 释放EGL环境
        mInputSurface.release();
        mEgl.release();
        return bitmap;
    }

    public Bitmap capture() {
        return capture(mWidth, mHeight);
    }
}

package cn.ezandroid.ezfilter.image.offscreen;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.os.Build;

import java.nio.IntBuffer;

import cn.ezandroid.ezfilter.core.FilterRender;
import cn.ezandroid.ezfilter.core.RenderPipeline;
import cn.ezandroid.ezfilter.environment.EGLEnvironment;
import cn.ezandroid.ezfilter.image.BitmapInput;

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

    public OffscreenImage(Bitmap bitmap) {
        mWidth = bitmap.getWidth();
        mHeight = bitmap.getHeight();

        mEgl = new EGLEnvironment(EGL14.eglGetCurrentContext(), false);
        mInputSurface = mEgl.createOffscreen(mWidth, mHeight);
        mInputSurface.makeCurrent();

        BitmapInput bitmapInput = new BitmapInput(bitmap);

        mPipeline = new RenderPipeline();
        mPipeline.onSurfaceCreated(null, null);
        mPipeline.setStartPointRender(bitmapInput);
    }

    public void addFilterRender(FilterRender filterRender) {
        mPipeline.addFilterRender(filterRender);
    }

    public Bitmap capture(int width, int height) {
        mPipeline.onSurfaceChanged(null, width, height);
        mPipeline.startRender();
        mPipeline.onDrawFrame(null);

        int[] iat = new int[width * height];
        IntBuffer ib = IntBuffer.allocate(width * height);
        GLES20.glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, ib);

        int[] ia = ib.array();
        // Convert upside down mirror -reversed image to right - side up normal image.
        for (int i = 0; i < height; i++) {
            System.arraycopy(ia, i * width, iat, (height - i - 1) * width, width);
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(IntBuffer.wrap(iat));

        mPipeline.onSurfaceDestroyed();

        mInputSurface.release();
        mEgl.release();
        return bitmap;
    }

    public Bitmap capture() {
        return capture(mWidth, mHeight);
    }
}

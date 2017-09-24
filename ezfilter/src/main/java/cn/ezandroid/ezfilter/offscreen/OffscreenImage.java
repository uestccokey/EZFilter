package cn.ezandroid.ezfilter.offscreen;

import android.graphics.Bitmap;

import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

import cn.ezandroid.ezfilter.core.FilterRender;
import cn.ezandroid.ezfilter.core.RenderPipeline;
import cn.ezandroid.ezfilter.io.input.BitmapInput;

import static javax.microedition.khronos.egl.EGL10.EGL_DEFAULT_DISPLAY;
import static javax.microedition.khronos.egl.EGL10.EGL_HEIGHT;
import static javax.microedition.khronos.egl.EGL10.EGL_NONE;
import static javax.microedition.khronos.egl.EGL10.EGL_WIDTH;
import static javax.microedition.khronos.opengles.GL10.GL_RGBA;
import static javax.microedition.khronos.opengles.GL10.GL_UNSIGNED_BYTE;

/**
 * 离屏渲染图片
 *
 * @author like
 * @date 2017-09-18
 */
public class OffscreenImage {

    private RenderPipeline mPipeline;

    private EGL10 mEGL;
    private EGLDisplay mEGLDisplay;
    private EGLConfig mEGLConfig;
    private EGLContext mEGLContext;
    private EGLSurface mEGLSurface;
    private GL10 mGL;

    private int mWidth;
    private int mHeight;

    public OffscreenImage(Bitmap bitmap) {
        mWidth = bitmap.getWidth();
        mHeight = bitmap.getHeight();

        int[] version = new int[2];
        int[] attribList = new int[]{
                EGL_WIDTH, mWidth,
                EGL_HEIGHT, mHeight,
                EGL_NONE
        };

        mEGL = (EGL10) EGLContext.getEGL();
        mEGLDisplay = mEGL.eglGetDisplay(EGL_DEFAULT_DISPLAY);
        mEGL.eglInitialize(mEGLDisplay, version);
        mEGLConfig = new GLConfigChooser(8, 8, 8, 8, 0, 0).chooseConfig(mEGL, mEGLDisplay);
        mEGLContext = new GLContextFactory().createContext(mEGL, mEGLDisplay, mEGLConfig);

        mEGLSurface = mEGL.eglCreatePbufferSurface(mEGLDisplay, mEGLConfig, attribList);
        mEGL.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext);

        mGL = (GL10) mEGLContext.getGL();

        BitmapInput bitmapInput = new BitmapInput(bitmap);

        mPipeline = new RenderPipeline();
        mPipeline.onSurfaceCreated(mGL, mEGLConfig);
        mPipeline.setStartPointRender(bitmapInput);
    }

    public void addFilterRender(FilterRender filterRender) {
        mPipeline.addFilterRender(filterRender);
    }

    public Bitmap capture(int width, int height) {
        mPipeline.onSurfaceChanged(mGL, width, height);
        mPipeline.startRender();
        mPipeline.onDrawFrame(mGL);

        int[] iat = new int[width * height];
        IntBuffer ib = IntBuffer.allocate(width * height);
        mGL.glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, ib);

        int[] ia = ib.array();
        // Convert upside down mirror -reversed image to right - side up normal image.
        for (int i = 0; i < height; i++) {
            System.arraycopy(ia, i * width, iat, (height - i - 1) * width, width);
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(IntBuffer.wrap(iat));

        mPipeline.onSurfaceDestroyed();

        mEGL.eglMakeCurrent(mEGLDisplay, EGL10.EGL_NO_SURFACE,
                EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);

        mEGL.eglDestroySurface(mEGLDisplay, mEGLSurface);
        mEGL.eglDestroyContext(mEGLDisplay, mEGLContext);
        mEGL.eglTerminate(mEGLDisplay);
        return bitmap;
    }

    public Bitmap capture() {
        return capture(mWidth, mHeight);
    }
}

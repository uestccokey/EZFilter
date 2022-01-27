package cn.ezandroid.lib.ezfilter.core.environment;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * EGL环境
 */
public class EGLEnvironment {

    private static final String TAG = "EGLEnvironment";

    private static final int EGL_RECORDABLE_ANDROID = 0x3142;

    private EGLConfig mEglConfig;
    private EGLContext mEglContext = EGL14.EGL_NO_CONTEXT;
    private EGLDisplay mEglDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext mDefaultContext = EGL14.EGL_NO_CONTEXT;

    public static class EglSurface {
        private final EGLEnvironment mEgl;
        private EGLSurface mEglSurface = EGL14.EGL_NO_SURFACE;
        private final int mWidth, mHeight;

        EglSurface(final EGLEnvironment egl, final Object surface) {
            if (!(surface instanceof SurfaceView)
                    && !(surface instanceof Surface)
                    && !(surface instanceof SurfaceHolder)
                    && !(surface instanceof SurfaceTexture))
                throw new IllegalArgumentException("unsupported surface");
            mEgl = egl;
            mEglSurface = mEgl.createWindowSurface(surface);
            mWidth = mEgl.querySurface(mEglSurface, EGL14.EGL_WIDTH);
            mHeight = mEgl.querySurface(mEglSurface, EGL14.EGL_HEIGHT);
        }

        EglSurface(final EGLEnvironment egl, final int width, final int height) {
            mEgl = egl;
            mEglSurface = mEgl.createOffscreenSurface(width, height);
            mWidth = width;
            mHeight = height;
        }

        public void makeCurrent() {
            mEgl.makeCurrent(mEglSurface);
        }

        public void swap() {
            mEgl.swap(mEglSurface);
        }

        public EGLSurface geSurface() {
            return mEglSurface;
        }

        public EGLContext getContext() {
            return mEgl.getContext();
        }

        public void release() {
            mEgl.makeDefault();
            mEgl.destroyWindowSurface(mEglSurface);
            mEglSurface = EGL14.EGL_NO_SURFACE;
        }

        public int getWidth() {
            return mWidth;
        }

        public int getHeight() {
            return mHeight;
        }
    }

    public EGLEnvironment(final EGLContext sharedContext, final boolean withDepthBuffer) {
        init(sharedContext, withDepthBuffer);
    }

    public void setPresentationTime(long nsecs, EglSurface eglSurface) {
        EGLExt.eglPresentationTimeANDROID(mEglDisplay, eglSurface.geSurface(), nsecs);
    }

    public EglSurface createFromSurface(final Object surface) {
        final EglSurface eglSurface = new EglSurface(this, surface);
        eglSurface.makeCurrent();
        return eglSurface;
    }

    public EglSurface createOffscreen(final int width, final int height) {
        final EglSurface eglSurface = new EglSurface(this, width, height);
        eglSurface.makeCurrent();
        return eglSurface;
    }

    public EGLContext getContext() {
        return mEglContext;
    }

    public int querySurface(final EGLSurface eglSurface, final int what) {
        final int[] value = new int[1];
        EGL14.eglQuerySurface(mEglDisplay, eglSurface, what, value, 0);
        return value[0];
    }

    private void init(EGLContext sharedContext, final boolean withDepthBuffer) {
        if (mEglDisplay != EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("EGL already set up");
        }

        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed");
        }

        final int[] version = new int[2];
        if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
            mEglDisplay = null;
            throw new RuntimeException("eglInitialize failed");
        }

        sharedContext = sharedContext != null ? sharedContext : EGL14.EGL_NO_CONTEXT;
        if (mEglContext == EGL14.EGL_NO_CONTEXT) {
            mEglConfig = getConfig(withDepthBuffer);
            if (mEglConfig == null) {
                throw new RuntimeException("chooseConfig failed");
            }
            // create EGL rendering context
            mEglContext = createContext(sharedContext);
        }
        // confirm whether the EGL rendering context is successfully created
        final int[] values = new int[1];
        EGL14.eglQueryContext(mEglDisplay, mEglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, values, 0);
        makeDefault();
    }

    private boolean makeCurrent(final EGLSurface surface) {
        if (surface == null || surface == EGL14.EGL_NO_SURFACE) {
            final int error = EGL14.eglGetError();
            if (error == EGL14.EGL_BAD_NATIVE_WINDOW) {
                Log.e(TAG, "makeCurrent:returned EGL_BAD_NATIVE_WINDOW.");
            }
            return false;
        }
        // attach EGL renderring context to specific EGL window surface
        if (!EGL14.eglMakeCurrent(mEglDisplay, surface, surface, mEglContext)) {
            Log.w(TAG, "eglMakeCurrent:" + EGL14.eglGetError());
            return false;
        }
        return true;
    }

    private void makeDefault() {
        if (!EGL14.eglMakeCurrent(mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)) {
            Log.w("TAG", "makeDefault" + EGL14.eglGetError());
        }
    }

    private int swap(final EGLSurface surface) {
        if (!EGL14.eglSwapBuffers(mEglDisplay, surface)) {
            final int err = EGL14.eglGetError();
            return err;
        }
        return EGL14.EGL_SUCCESS;
    }

    private EGLContext createContext(final EGLContext sharedContext) {
        final int[] attributes = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        final EGLContext context = EGL14.eglCreateContext(mEglDisplay, mEglConfig, sharedContext, attributes, 0);
        checkEglError("eglCreateContext");
        return context;
    }

    private void destroyContext() {
        if (!EGL14.eglDestroyContext(mEglDisplay, mEglContext)) {
            Log.e("destroyContext", "display:" + mEglDisplay + " context: " + mEglContext);
            Log.e(TAG, "eglDestroyContex:" + EGL14.eglGetError());
        }
        mEglContext = EGL14.EGL_NO_CONTEXT;
        if (mDefaultContext != EGL14.EGL_NO_CONTEXT) {
            if (!EGL14.eglDestroyContext(mEglDisplay, mDefaultContext)) {
                Log.e("destroyContext", "display:" + mEglDisplay + " context: " + mDefaultContext);
                Log.e(TAG, "eglDestroyContex:" + EGL14.eglGetError());
            }
            mDefaultContext = EGL14.EGL_NO_CONTEXT;
        }
    }

    private EGLSurface createWindowSurface(final Object nativeWindow) {
        final int[] surfaceAttributes = {
                EGL14.EGL_NONE
        };
        EGLSurface result = null;
        try {
            result = EGL14.eglCreateWindowSurface(mEglDisplay, mEglConfig, nativeWindow, surfaceAttributes, 0);
        } catch (final IllegalArgumentException e) {
            Log.e(TAG, "eglCreateWindowSurface", e);
        }
        return result;
    }

    private EGLSurface createOffscreenSurface(final int width, final int height) {
        final int[] surfaceAttributes = {
                EGL14.EGL_WIDTH, width,
                EGL14.EGL_HEIGHT, height,
                EGL14.EGL_NONE
        };
        EGLSurface result = null;
        try {
            result = EGL14.eglCreatePbufferSurface(mEglDisplay, mEglConfig, surfaceAttributes, 0);
        } catch (final IllegalArgumentException e) {
            Log.e(TAG, "createOffscreenSurface", e);
        } catch (final RuntimeException e) {
            Log.e(TAG, "createOffscreenSurface", e);
        }
        return result;
    }

    private void destroyWindowSurface(EGLSurface surface) {
        if (surface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglMakeCurrent(mEglDisplay,
                    EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroySurface(mEglDisplay, surface);
        }
        surface = EGL14.EGL_NO_SURFACE;
    }

    private void checkEglError(final String msg) {
        int error;
        if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
            throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
        }
    }

    private EGLConfig getConfig(final boolean withDepthBuffer) {
        final int[] attributes = {
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_NONE, EGL14.EGL_NONE, // EGL_DEPTH_SIZE占位，下面再进行设置
                EGL14.EGL_NONE, EGL14.EGL_NONE, // EGL_RECORDABLE_ANDROID占位，下面再进行设置
                EGL14.EGL_NONE
        };
        int offset = 10;
        if (withDepthBuffer) {
            attributes[offset++] = EGL14.EGL_DEPTH_SIZE;
            attributes[offset++] = 16;
        }
        if (Build.VERSION.SDK_INT >= 18) {
            attributes[offset++] = EGL_RECORDABLE_ANDROID;
            attributes[offset++] = 1;
        }
        for (int i = attributes.length - 1; i >= offset; i--) {
            attributes[i] = EGL14.EGL_NONE;
        }
        final EGLConfig[] configs = new EGLConfig[1];
        final int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(mEglDisplay, attributes, 0,
                configs, 0, configs.length, numConfigs, 0)) {
            return null;
        }
        return configs[0];
    }

    public void release() {
        if (mEglDisplay != EGL14.EGL_NO_DISPLAY) {
            destroyContext();
            EGL14.eglTerminate(mEglDisplay);
            EGL14.eglReleaseThread();
        }
        mEglDisplay = EGL14.EGL_NO_DISPLAY;
        mEglContext = EGL14.EGL_NO_CONTEXT;
    }
}

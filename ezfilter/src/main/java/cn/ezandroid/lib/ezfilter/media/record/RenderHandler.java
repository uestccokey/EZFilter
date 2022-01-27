package cn.ezandroid.lib.ezfilter.media.record;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.text.TextUtils;
import android.view.Surface;
import android.view.SurfaceHolder;

import cn.ezandroid.lib.ezfilter.core.GLRender;
import cn.ezandroid.lib.ezfilter.core.environment.EGLEnvironment;

public final class RenderHandler implements Runnable {

    private static final String TAG = "RenderHandler";

    private final Object mSync = new Object();
    private EGLContext mSharedContext;
    private Object mSurface;
    private int mTextureId = -1;

    private boolean mRequestSetEglContext;
    private boolean mRequestRelease;
    private int mRequestDraw;

    private EGLEnvironment mEgl;
    private EGLEnvironment.EglSurface mInputSurface;
    private SimpleRender mRecordRender;

    public static RenderHandler createHandler(final String name) {
        final RenderHandler handler = new RenderHandler();
        synchronized (handler.mSync) {
            new Thread(handler, !TextUtils.isEmpty(name) ? name : TAG).start();
            try {
                handler.mSync.wait();
            } catch (final InterruptedException e) {
            }
        }
        return handler;
    }

    /**
     * 获取输入的纹理
     *
     * @return
     */
    public final int getInputTextureId() {
        return mTextureId;
    }

    /**
     * 设置输入纹理
     * <p>
     * 必须在GL线程调用
     *
     * @param surface
     * @param texId
     */
    public final void setInputTextureId(final Object surface, final int texId) {
        if (!(surface instanceof Surface)
                && !(surface instanceof SurfaceTexture)
                && !(surface instanceof SurfaceHolder)) {
            throw new RuntimeException("unsupported window type:" + surface);
        }
        synchronized (mSync) {
            if (mRequestRelease) return;
            mSharedContext = EGL14.eglGetCurrentContext();
            mTextureId = texId;
            mSurface = surface;
            mRequestSetEglContext = true;
            mSync.notifyAll();
            try {
                mSync.wait();
            } catch (final InterruptedException e) {
            }
        }
    }

    public final void draw() {
        synchronized (mSync) {
            if (mRequestRelease) return;
            mRequestDraw++;
            mSync.notifyAll();
        }
    }

    public final void release() {
        synchronized (mSync) {
            if (mRequestRelease) return;
            mRequestRelease = true;
            mSync.notifyAll();
            try {
                mSync.wait();
            } catch (final InterruptedException e) {
            }
        }
    }

    @Override
    public final void run() {
        synchronized (mSync) {
            mRequestSetEglContext = mRequestRelease = false;
            mRequestDraw = 0;
            mSync.notifyAll();
        }
        boolean localRequestDraw;
        for (; ; ) {
            synchronized (mSync) {
                if (mRequestRelease) break;
                if (mRequestSetEglContext) {
                    mRequestSetEglContext = false;
                    internalPrepare();
                }
                localRequestDraw = mRequestDraw > 0;
                if (localRequestDraw) {
                    mRequestDraw--;
                }
            }
            if (localRequestDraw) {
                if ((mEgl != null) && mTextureId >= 0) {
                    mInputSurface.makeCurrent();
                    mRecordRender.draw(mTextureId);
                    mInputSurface.swap();
                }
            } else {
                synchronized (mSync) {
                    try {
                        mSync.wait();
                    } catch (final InterruptedException e) {
                        break;
                    }
                }
            }
        }
        synchronized (mSync) {
            mRequestRelease = true;
            internalRelease();
            mSync.notifyAll();
        }
    }

    private void internalPrepare() {
        internalRelease();

        mEgl = new EGLEnvironment(mSharedContext, false);
        mInputSurface = mEgl.createFromSurface(mSurface);
        mInputSurface.makeCurrent();

        mRecordRender = new SimpleRender();
        mSurface = null;
        mSync.notifyAll();
    }

    private void internalRelease() {
        if (mInputSurface != null) {
            mInputSurface.release();
            mInputSurface = null;
        }
        if (mRecordRender != null) {
            mRecordRender.destroy();
            mRecordRender = null;
        }
        if (mEgl != null) {
            mEgl.release();
            mEgl = null;
        }
    }

    class SimpleRender extends GLRender {

        void draw(int textureIn) {
            mTextureIn = textureIn;
            onDrawFrame();
        }
    }
}

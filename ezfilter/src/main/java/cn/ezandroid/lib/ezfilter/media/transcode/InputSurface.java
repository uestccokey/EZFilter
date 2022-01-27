package cn.ezandroid.lib.ezfilter.media.transcode;

import android.opengl.EGL14;
import android.view.Surface;

import cn.ezandroid.lib.ezfilter.core.environment.EGLEnvironment;

/**
 * 将MediaCodec.createInputSurface()创建的Surface与EGL环境绑定，
 * 这样数据写入Surface中后，MediaCodec可以进行编码
 *
 * @author like
 * @date 2017-09-23
 */
class InputSurface {

    private Surface mSurface;

    private EGLEnvironment mEgl;
    private EGLEnvironment.EglSurface mInputSurface;

    /**
     * @param surface 来自MediaCodec.createInputSurface()
     */
    public InputSurface(Surface surface) {
        if (surface == null) {
            throw new NullPointerException();
        }
        mSurface = surface;

        // 初始化EGL环境
        mEgl = new EGLEnvironment(EGL14.eglGetCurrentContext(), false);
        // 创建离屏缓冲
        mInputSurface = mEgl.createFromSurface(surface);
        // 设置渲染环境可用
        mInputSurface.makeCurrent();
    }

    /**
     * 调用swap后，会将之前OpenGL绘制的数据放到mSurface中
     */
    public void swapBuffers() {
        mInputSurface.swap();
    }

    public void setPresentationTime(long nsecs) {
        mEgl.setPresentationTime(nsecs, mInputSurface);
    }

    /**
     * 释放所有资源
     */
    public void release() {
        // 释放EGL环境
        mInputSurface.release();
        mEgl.release();

        mSurface.release();
        mSurface = null;
    }
}

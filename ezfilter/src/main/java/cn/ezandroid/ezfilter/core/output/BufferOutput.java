package cn.ezandroid.ezfilter.core.output;

import android.opengl.GLES20;

import java.nio.Buffer;

import cn.ezandroid.ezfilter.core.FBORender;

/**
 * OpenGL像素Buffer输出基类
 *
 * @author like
 * @date 2017-09-17
 */
public abstract class BufferOutput<T extends Buffer> extends FBORender {

    protected T mOutputBuffer;

    public BufferOutput() {
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        if (mOutputBuffer == null || mSizeChanged) {
            mOutputBuffer = initBuffer(getWidth(), getHeight());
        }

        GLES20.glReadPixels(0, 0, getWidth(), getHeight(), GLES20.GL_RGBA, GLES20
                .GL_UNSIGNED_BYTE, mOutputBuffer);

        bufferOutput(mOutputBuffer);
    }

    public abstract T initBuffer(int width, int height);

    public abstract void bufferOutput(T buffer);

    @Override
    public void destroy() {
        super.destroy();
        if (mOutputBuffer != null) {
            mOutputBuffer.clear();
            mOutputBuffer = null;
        }
    }
}

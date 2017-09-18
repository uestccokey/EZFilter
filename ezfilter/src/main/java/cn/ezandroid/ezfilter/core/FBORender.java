package cn.ezandroid.ezfilter.core;

import android.opengl.GLES20;

import java.util.ArrayList;
import java.util.List;

/**
 * 支持帧缓冲的渲染器
 * <p>
 * 会将图像渲染到一个输出纹理中
 *
 * @author like
 * @date 2017-09-15
 */
public class FBORender extends AbstractRender {

    protected int[] mFrameBuffer;
    protected int[] mTextureOut;
    protected int[] mDepthRenderBuffer;

    protected List<OnTextureAvailableListener> mTargets;
    private final Object mListLock = new Object();

    public FBORender() {
        super();
        mTargets = new ArrayList<>();
    }

    @Override
    public void destroy() {
        super.destroy();
        if (mFrameBuffer != null) {
            GLES20.glDeleteFramebuffers(1, mFrameBuffer, 0);
            mFrameBuffer = null;
        }
        if (mDepthRenderBuffer != null) {
            GLES20.glDeleteRenderbuffers(1, mDepthRenderBuffer, 0);
            mDepthRenderBuffer = null;
        }
        if (mTextureOut != null) {
            GLES20.glDeleteTextures(1, mTextureOut, 0);
            mTextureOut = null;
        }
    }

    @Override
    protected void drawFrame() {
        if (mTextureOut == null) {
            if (mWidth != 0 && mHeight != 0) {
                initFBO();
            } else {
                return;
            }
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer[0]);

        onDraw();

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        synchronized (mListLock) {
            for (OnTextureAvailableListener target : mTargets) {
                if (null != target && null != mTextureOut && mTextureOut.length > 0) {
                    target.onTextureAvailable(mTextureOut[0], this);
                }
            }
        }
    }

    /**
     * 为了方便子类覆盖，这里独立为一个方法
     */
    protected void onDraw() {
        super.drawFrame();
    }

    public Object getLock() {
        return mListLock;
    }

    @Override
    protected void onRenderSizeChanged() {
        initFBO();
    }

    private void initFBO() {
        // 初始化输出纹理
        if (mTextureOut != null) {
            GLES20.glDeleteTextures(1, mTextureOut, 0);
            mTextureOut = null;
        }
        mTextureOut = new int[1];
        GLES20.glGenTextures(1, mTextureOut, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureOut[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                getWidth(), getHeight(), 0, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);

        // 初始化帧缓冲和深度缓冲
        if (mFrameBuffer != null) {
            GLES20.glDeleteFramebuffers(1, mFrameBuffer, 0);
            mFrameBuffer = null;
        }
        if (mDepthRenderBuffer != null) {
            GLES20.glDeleteRenderbuffers(1, mDepthRenderBuffer, 0);
            mDepthRenderBuffer = null;
        }
        mFrameBuffer = new int[1];
        mDepthRenderBuffer = new int[1];
        GLES20.glGenFramebuffers(1, mFrameBuffer, 0);
        GLES20.glGenRenderbuffers(1, mDepthRenderBuffer, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
                GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D,
                mTextureOut[0], 0);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mDepthRenderBuffer[0]);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER,
                GLES20.GL_DEPTH_COMPONENT16, getWidth(), getHeight());
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER,
                GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER,
                mDepthRenderBuffer[0]);
    }

    public List<OnTextureAvailableListener> getTargets() {
        return mTargets;
    }

    public synchronized void addTarget(OnTextureAvailableListener target) {
        synchronized (mListLock) {
            if (!mTargets.contains(target) && target != null) {
                mTargets.add(target);
            }
        }
    }

    public void removeTarget(OnTextureAvailableListener target) {
        synchronized (mListLock) {
            mTargets.remove(target);
        }
    }

    public void clearTargets() {
        synchronized (mListLock) {
            mTargets.clear();
        }
    }
}

package cn.ezandroid.lib.ezfilter.media.transcode;

import android.graphics.SurfaceTexture;
import android.view.Surface;

/**
 * 创建一个Surface，作为参数传给MediaCodec.configure，
 * 这样MediaCodec解码出来的视频数据会直接显示在此Surface上
 *
 * @author like
 * @date 2017-09-23
 */
public class OutputSurface implements SurfaceTexture.OnFrameAvailableListener {

    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private final Object mFrameSyncObject = new Object();     // guards mFrameAvailable
    private boolean mFrameAvailable;
    private IVideoRender mTextureRender;

    public OutputSurface(IVideoRender render) {
        mTextureRender = render;
        setup();
    }

    private void setup() {
        mSurfaceTexture = mTextureRender.getSurfaceTexture();
        mSurfaceTexture.setOnFrameAvailableListener(this);
        mSurface = new Surface(mSurfaceTexture);
    }

    /**
     * 释放所有资源
     */
    public void release() {
        mSurface.release();
        mTextureRender = null;
        mSurface = null;
        mSurfaceTexture = null;
    }

    public Surface getSurface() {
        return mSurface;
    }

    public void awaitNewImage() {
        final int TIMEOUT_MS = 10000;
        synchronized (mFrameSyncObject) {
            while (!mFrameAvailable) {
                try {
                    // Wait for onFrameAvailable() to signal us.  Use a timeout to avoid
                    // stalling the test if it doesn't arrive.
                    mFrameSyncObject.wait(TIMEOUT_MS);
                    if (!mFrameAvailable) {
                        // TODO: if "spurious wakeup", continue while loop
                        throw new RuntimeException("Surface frame wait timed out");
                    }
                } catch (InterruptedException ie) {
                    // shouldn't happen
                    throw new RuntimeException(ie);
                }
            }
            mFrameAvailable = false;
        }
        // Latch the data.
        mSurfaceTexture.updateTexImage();
    }

    /**
     * 绘制当前帧
     *
     * @param time 当前帧时间（单位纳秒）
     */
    public void drawImage(long time) {
        mTextureRender.drawFrame(time);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture st) {
        synchronized (mFrameSyncObject) {
            if (mFrameAvailable) {
                throw new RuntimeException("mFrameAvailable already set, frame could be dropped");
            }
            mFrameAvailable = true;
            mFrameSyncObject.notifyAll();
        }
    }
}

package cn.ezandroid.ezfilter.core;

import android.graphics.Bitmap;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import cn.ezandroid.ezfilter.core.output.BitmapOutput;
import cn.ezandroid.ezfilter.core.output.BufferOutput;
import cn.ezandroid.ezfilter.core.util.L;
import cn.ezandroid.ezfilter.environment.Renderer;

/**
 * 渲染管道
 * <p>
 * 一个完整的渲染管道由渲染起点，滤镜列表（可以为空）和渲染终点组成
 *
 * @author like
 * @date 2017-09-15
 */
public class RenderPipeline implements Renderer {

    private boolean mIsRendering;

    private int mWidth;
    private int mHeight;

    private FBORender mStartPointRender; // 起点渲染器
    private List<FilterRender> mFilterRenders = new ArrayList<>(); // 滤镜列表
    private EndPointRender mEndPointRender = new EndPointRender(); // 终点渲染器

    private List<BufferOutput> mOutputs = new ArrayList<>(); // 输出列表

    private final List<AbstractRender> mRendersToDestroy;

    private int mCurrentRotation;

    private List<OnSurfaceListener> mOnSurfaceListeners;

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        if (L.LOG_PIPELINE_CREATE) {
            Log.e("RenderPipeline", this + " onSurfaceCreated");
        }
        for (OnSurfaceListener listener : mOnSurfaceListeners) {
            listener.onSurfaceCreated(gl10, eglConfig);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        if (L.LOG_PIPELINE_CHANGE) {
            Log.e("RenderPipeline", this + " onSurfaceChanged:" + width + "x" + height);
        }
        this.mWidth = width;
        this.mHeight = height;
        updateRendersSize();

        for (OnSurfaceListener listener : mOnSurfaceListeners) {
            listener.onSurfaceChanged(gl10, width, height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        if (L.LOG_PIPELINE_DRAW) {
            Log.e("RenderPipeline", this + " onDrawFrame:" + mWidth + "x" + mHeight + " " + isRendering());
        }
        if (isRendering()) {
            if (mStartPointRender != null) {
                mStartPointRender.onDrawFrame();
            }
            synchronized (mRendersToDestroy) {
                for (AbstractRender renderer : mRendersToDestroy) {
                    if (renderer != null) {
                        renderer.destroy();
                    }
                }
                mRendersToDestroy.clear();
            }
        }
    }

    @Override
    public void onSurfaceDestroyed() {
        if (L.LOG_PIPELINE_DESTROY) {
            Log.e("RenderPipeline", this + " onSurfaceDestroyed " + Thread.currentThread().getName());
        }
        if (mStartPointRender != null) {
            mStartPointRender.destroy();
        }
        for (FilterRender filterRender : mFilterRenders) {
            filterRender.destroy();
        }
        mEndPointRender.destroy();

        for (BufferOutput bufferOutput : mOutputs) {
            bufferOutput.destroy();
        }

        for (OnSurfaceListener listener : mOnSurfaceListeners) {
            listener.onSurfaceDestroyed();
        }
    }

    public interface OnSurfaceListener {

        void onSurfaceCreated(GL10 gl, EGLConfig config);

        void onSurfaceChanged(GL10 gl, int width, int height);

        void onSurfaceDestroyed();
    }

    public static class SimpleOnSurfaceListener implements OnSurfaceListener {

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
        }

        @Override
        public void onSurfaceDestroyed() {
        }
    }

    public RenderPipeline() {
        mRendersToDestroy = new ArrayList<>();
        mOnSurfaceListeners = new ArrayList<>();
    }

    public void clean() {
        boolean isRenders = isRendering();
        setRendering(false); // 暂时停止渲染，构建渲染链完成后再进行渲染

        if (mStartPointRender != null) {
            mStartPointRender.clearTargets();
        }
        addRenderToDestroy(mStartPointRender);
        mStartPointRender = null;

        for (FilterRender filterRender : mFilterRenders) {
            addRenderToDestroy(filterRender);
        }
        mFilterRenders.clear();

        for (BufferOutput bufferOutput : mOutputs) {
            addRenderToDestroy(bufferOutput);
        }
        mOutputs.clear();

        mCurrentRotation = 0;
        mEndPointRender.setRotate90Degrees(0);

        setRendering(isRenders);
    }

    private void updateRendersSize() {
        // 设置起点渲染器的大小
        if (mStartPointRender != null) {
            mStartPointRender.setRenderSize(mWidth, mHeight);
        }

        // 设置所有滤镜的大小
        for (FilterRender filterRender : mFilterRenders) {
            filterRender.setRenderSize(mWidth, mHeight);
        }

        // 不直接设置终点渲染器的大小，可以在外部手动设置，或者在onTextureAcceptable内自动设置
//        mEndPointRender.setRenderSize(mWidth, mHeight);

        // 设置所有缓冲输出的大小
        for (BufferOutput bufferOutput : mOutputs) {
            bufferOutput.setRenderSize(mWidth, mHeight);
        }
    }

    /**
     * 添加一个滤镜到销毁队列
     * 下一个onDrawFrame执行时，会调用销毁队列中的所有滤镜的destroy方法，并清空销毁队列
     *
     * @param render
     */
    public void addRenderToDestroy(AbstractRender render) {
        synchronized (mRendersToDestroy) {
            mRendersToDestroy.add(render);
        }
    }

    /**
     * 添加OnSurfaceListener监听
     *
     * @param listener
     */
    public void addOnSurfaceListener(OnSurfaceListener listener) {
        if (!mOnSurfaceListeners.contains(listener)) {
            mOnSurfaceListeners.add(listener);
        }
    }

    /**
     * 删除OnSurfaceListener监听
     *
     * @param listener
     */
    public void removeOnSurfaceListener(OnSurfaceListener listener) {
        if (mOnSurfaceListeners.contains(listener)) {
            mOnSurfaceListeners.remove(listener);
        }
    }

    /**
     * 清空OnSurfaceListener监听列表
     */
    public void clearOnSurfaceListener() {
        mOnSurfaceListeners.clear();
    }

    public int getHeight() {
        return mHeight;
    }

    public int getWidth() {
        return mWidth;
    }

    public void setRenderSize(int width, int height) {
        mWidth = width;
        mHeight = height;
        updateRendersSize();
    }

    /**
     * 设置顺时针旋转90度的次数
     * 取值-3~0，0~3，表示旋转-270~0，0~270度
     *
     * @param numOfTimes 旋转次数
     */
    public void setRotate90Degrees(int numOfTimes) {
        mCurrentRotation = numOfTimes;
        mEndPointRender.resetRotate();
        mEndPointRender.setRotate90Degrees(numOfTimes);

        for (BufferOutput bufferOutput : mOutputs) {
            bufferOutput.setRotate90Degrees(numOfTimes);
        }
    }

    /**
     * 是否正在渲染
     *
     * @return
     */
    public synchronized boolean isRendering() {
        return mIsRendering;
    }

    /**
     * 设置是否渲染
     *
     * @param rendering
     */
    public synchronized void setRendering(boolean rendering) {
        mIsRendering = rendering;
    }

    /**
     * 开始渲染
     */
    public synchronized void startRender() {
        mIsRendering = true;
    }

    /**
     * 暂停渲染
     */
    public synchronized void pauseRender() {
        mIsRendering = false;
    }

    /**
     * 获取渲染起点
     *
     * @return
     */
    public synchronized FBORender getStartPointRender() {
        return mStartPointRender;
    }

    /**
     * 设置渲染起点
     *
     * @param startPointRenderer
     */
    public synchronized void setStartPointRender(FBORender startPointRenderer) {
        if (mStartPointRender != null) {
            for (OnTextureAcceptableListener render : mStartPointRender.getTargets()) {
                startPointRenderer.addTarget(render);
            }
            mStartPointRender.clearTargets();
            addRenderToDestroy(mStartPointRender);
            mStartPointRender = startPointRenderer;
            mStartPointRender.setWidth(mWidth);
            mStartPointRender.setHeight(mHeight);
        } else {
            mStartPointRender = startPointRenderer;
            mStartPointRender.setWidth(mWidth);
            mStartPointRender.setHeight(mHeight);
            mStartPointRender.addTarget(mEndPointRender);
        }
        updateRendersSize();
    }

    /**
     * 设置渲染终点
     *
     * @param endPointRender
     */
    public synchronized void setEndPointRender(EndPointRender endPointRender) {
        if (mFilterRenders.isEmpty()) {
            mStartPointRender.addTarget(endPointRender);
            mStartPointRender.removeTarget(mEndPointRender);
        } else {
            FilterRender filterRender = mFilterRenders.get(mFilterRenders.size() - 1);
            filterRender.addTarget(endPointRender);
            filterRender.removeTarget(mEndPointRender);
        }
        addRenderToDestroy(mEndPointRender);
        mEndPointRender = endPointRender;

        updateRendersSize();
    }

    public synchronized void addOutput(FBORender filterRender, BufferOutput bufferOutput) {
        if (bufferOutput != null && !mOutputs.contains(bufferOutput)
                && mStartPointRender != null && mEndPointRender != null) {
            boolean isRenders = isRendering();
            setRendering(false); // 暂时停止渲染，构建渲染链完成后再进行渲染

            bufferOutput.clearTargets();
            bufferOutput.setWidth(mWidth);
            bufferOutput.setHeight(mHeight);
            bufferOutput.setRotate90Degrees(mCurrentRotation);

            filterRender.addTarget(bufferOutput);
            mOutputs.add(bufferOutput);

            setRendering(isRenders);
        }
    }

    public synchronized void removeOutput(FBORender filterRender, BufferOutput bufferOutput) {
        if (filterRender != null && mFilterRenders.contains(filterRender)
                && mStartPointRender != null && mEndPointRender != null) {
            boolean isRenders = isRendering();
            setRendering(false); // 暂时停止渲染，构建渲染链完成后再进行渲染

            mOutputs.remove(bufferOutput);

            filterRender.removeTarget(bufferOutput);
            addRenderToDestroy(bufferOutput);

            setRendering(isRenders);
        }
    }

    /**
     * 添加滤镜
     *
     * @param filterRender
     */
    public synchronized void addFilterRender(FilterRender filterRender) {
        if (filterRender != null && !mFilterRenders.contains(filterRender)
                && mStartPointRender != null && mEndPointRender != null) {
            boolean isRenders = isRendering();
            setRendering(false); // 暂时停止渲染，构建渲染链完成后再进行渲染

            filterRender.clearTargets(); // 确保要添加的滤镜是干净的
            filterRender.setRenderSize(mWidth, mHeight);

            if (mFilterRenders.isEmpty()) {
                // 添加了第一个滤镜
                mStartPointRender.removeTarget(mEndPointRender);
                mStartPointRender.addTarget(filterRender);
                filterRender.addTarget(mEndPointRender);
            } else {
                FilterRender lastFilterRender = mFilterRenders.get(mFilterRenders.size() - 1);
                lastFilterRender.removeTarget(mEndPointRender);
                lastFilterRender.addTarget(filterRender);
                filterRender.addTarget(mEndPointRender);
            }
            mFilterRenders.add(filterRender);

            setRendering(isRenders);
        }
    }

    /**
     * 删除滤镜
     *
     * @param filterRender
     */
    public synchronized void removeFilterRender(FilterRender filterRender) {
        if (filterRender != null && mFilterRenders.contains(filterRender)
                && mStartPointRender != null && mEndPointRender != null) {
            boolean isRenders = isRendering();
            setRendering(false); // 暂时停止渲染，构建渲染链完成后再进行渲染

            int index = mFilterRenders.indexOf(filterRender);
            mFilterRenders.remove(filterRender);
            if (mFilterRenders.isEmpty()) {
                // 删除了最后一个滤镜
                mStartPointRender.removeTarget(filterRender);
                filterRender.removeTarget(mEndPointRender);
                mStartPointRender.addTarget(mEndPointRender);
            } else {
                if (index == 0) {
                    FilterRender nextRender = mFilterRenders.get(0);
                    mStartPointRender.removeTarget(filterRender);
                    filterRender.removeTarget(nextRender);
                    mStartPointRender.addTarget(nextRender);
                } else if (index == mFilterRenders.size()) {
                    FilterRender prevRender = mFilterRenders.get(mFilterRenders.size() - 1);
                    prevRender.removeTarget(filterRender);
                    filterRender.removeTarget(mEndPointRender);
                    prevRender.addTarget(mEndPointRender);
                } else {
                    FilterRender prevRender = mFilterRenders.get(index - 1);
                    FilterRender nextRender = mFilterRenders.get(index);
                    prevRender.removeTarget(filterRender);
                    filterRender.removeTarget(nextRender);
                    prevRender.addTarget(nextRender);
                }
            }
            addRenderToDestroy(filterRender);

            setRendering(isRenders);
        }
    }

    /**
     * 清空滤镜列表
     */
    public synchronized void clearFilterRenders() {
        if (mStartPointRender != null && mEndPointRender != null && !mFilterRenders.isEmpty()) {
            boolean isRenders = isRendering();
            setRendering(false); // 暂时停止渲染，构建渲染链完成后再进行渲染

            if (mFilterRenders.size() == 1) {
                FilterRender filterRender = mFilterRenders.get(0);
                mStartPointRender.removeTarget(filterRender);
                filterRender.removeTarget(mEndPointRender);
                mStartPointRender.addTarget(mEndPointRender);
            } else {
                FilterRender firstFilterRender = mFilterRenders.get(0);
                FilterRender lastFilterRender = mFilterRenders.get(mFilterRenders.size() - 1);
                mStartPointRender.removeTarget(firstFilterRender);
                lastFilterRender.removeTarget(mEndPointRender);
                mStartPointRender.addTarget(mEndPointRender);
            }
            for (FilterRender filterRender : mFilterRenders) {
                addRenderToDestroy(filterRender);
            }
            mFilterRenders.clear();

            setRendering(isRenders);
        }
    }

    /**
     * 获取滤镜列表
     *
     * @return
     */
    public synchronized List<FilterRender> getFilterRenders() {
        return mFilterRenders;
    }

    /**
     * 获取渲染终点
     *
     * @return
     */
    public synchronized EndPointRender getEndPointRender() {
        return mEndPointRender;
    }

    /**
     * 异步截图
     *
     * @param callback   异步获取截图回调
     * @param withFilter 是否滤镜后的图
     */
    public void output(final BitmapOutput.BitmapOutputCallback callback, boolean withFilter) {
        output(callback, mWidth, mHeight, withFilter);
    }

    /**
     * 异步截图
     *
     * @param callback   异步获取截图回调
     * @param width      截图宽度
     * @param height     截图高度
     * @param withFilter 是否滤镜后的图
     */
    public void output(final BitmapOutput.BitmapOutputCallback callback, int width, int height, boolean withFilter) {
        FBORender render = null;
        if (withFilter && !mFilterRenders.isEmpty()) {
            render = mFilterRenders.get(mFilterRenders.size() - 1);
        } else {
            render = mStartPointRender;
        }

        final FBORender fRender = render;
        final BitmapOutput bitmapOutput = new BitmapOutput();
        bitmapOutput.setRenderSize(width, height);
        bitmapOutput.setBitmapOutputCallback(new BitmapOutput.BitmapOutputCallback() {
            @Override
            public void bitmapOutput(final Bitmap bitmap) {
                if (callback != null) {
                    callback.bitmapOutput(bitmap);
                }

                removeOutput(fRender, bitmapOutput);
            }
        });
        addOutput(render, bitmapOutput);
    }

    /**
     * 是否正在录制视频
     *
     * @return
     */
    public boolean isRecording() {
        return mEndPointRender instanceof ISupportRecord && ((ISupportRecord) mEndPointRender).isRecording();
    }

    /**
     * 开始录制
     */
    public void startRecording() {
        if (mEndPointRender instanceof ISupportRecord) {
            ((ISupportRecord) mEndPointRender).startRecording();
        } else {
            throw new UnsupportedOperationException("unsupported record");
        }
    }

    /**
     * 结束录制
     */
    public void stopRecording() {
        if (mEndPointRender instanceof ISupportRecord) {
            ((ISupportRecord) mEndPointRender).stopRecording();
        } else {
            throw new UnsupportedOperationException("unsupported record");
        }
    }


    /**
     * 拍照
     *
     * @param isFront  是否前置摄像头
     * @param degree   手机旋转角度（0~360度）
     * @param callback 回调
     */
    public void takePhoto(boolean isFront, int degree, PhotoTakenCallback callback) {
        if (mStartPointRender instanceof ISupportTakePhoto) {
            ((ISupportTakePhoto) mStartPointRender).takePhoto(isFront, degree, callback);
        } else {
            throw new UnsupportedOperationException("unsupported take photo");
        }
    }
}

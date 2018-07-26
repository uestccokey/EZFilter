package cn.ezandroid.ezfilter.core;

import android.opengl.GLES20;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import cn.ezandroid.ezfilter.core.environment.Renderer;
import cn.ezandroid.ezfilter.core.output.BitmapOutput;
import cn.ezandroid.ezfilter.core.output.BufferOutput;
import cn.ezandroid.ezfilter.core.util.L;

/**
 * 渲染管道
 * <p>
 * 一个完整的渲染管道由渲染起点，滤镜列表（可以为空）和渲染终点组成
 * <p>
 * 其中下列方法为synchronized修饰的同步方法，确保多线程操作下的渲染管道构建正确
 * clean
 * setStartPointRender
 * addEndPointRender
 * removeEndPointRender
 * addOutput
 * removeOutput
 * addFilterRender
 * removeFilterRender
 * clearFilterRenders
 * <p>
 * 另外，部分方法内部还有synchronized修饰的代码块，是因为Renderer接口的回调在GL线程，需要进行线程同步
 *
 * @author like
 * @date 2017-09-15
 */
public class RenderPipeline implements Renderer {

    private volatile boolean mIsRendering;

    private int mWidth;
    private int mHeight;

    private int mCurrentRotation;

    private FBORender mStartPointRender; // 起点渲染器
    private final List<FBORender> mFilterRenders = new ArrayList<>(); // 滤镜列表
    private final List<GLRender> mEndPointRenders = new ArrayList<>(); // 终点渲染器列表

    private final List<BufferOutput> mOutputs = new ArrayList<>(); // 输出列表

    private final List<GLRender> mRendersToDestroy = new ArrayList<>();

    private final List<OnSurfaceListener> mOnSurfaceListeners = new ArrayList<>();

    private final List<OnFilterRendersChangedListener> mRendersChangedListeners = new ArrayList<>();

    private float mBackgroundRed;
    private float mBackgroundGreen;
    private float mBackgroundBlue;
    private float mBackgroundAlpha = 1f;

    public RenderPipeline() {
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        if (L.LOG_PIPELINE_CREATE) {
            Log.e("RenderPipeline", this + " onSurfaceCreated");
        }

        synchronized (mOnSurfaceListeners) {
            for (OnSurfaceListener listener : mOnSurfaceListeners) {
                listener.onSurfaceCreated(gl10, eglConfig);
            }
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        if (L.LOG_PIPELINE_CHANGE) {
            Log.e("RenderPipeline", this + " onSurfaceChanged:" + width + "x" + height);
        }

        setRenderSize(width, height);

        synchronized (mOnSurfaceListeners) {
            for (OnSurfaceListener listener : mOnSurfaceListeners) {
                listener.onSurfaceChanged(gl10, width, height);
            }
        }
    }

    public void setBackgroundRed(float red) {
        mBackgroundRed = red;
    }

    public void setBackgroundGreen(float green) {
        mBackgroundGreen = green;
    }

    public void setBackgroundBlue(float blue) {
        mBackgroundBlue = blue;
    }

    public void setBackgroundAlpha(float alpha) {
        mBackgroundAlpha = alpha;
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        if (L.LOG_PIPELINE_DRAW) {
            Log.e("RenderPipeline", this + " onDrawFrame:" + mWidth + "x" + mHeight + " " + isRendering());
        }

        if (gl10 != null) {
            GLES20.glClearColor(mBackgroundRed, mBackgroundGreen, mBackgroundBlue, mBackgroundAlpha);
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        }

        if (isRendering()) {
            if (mStartPointRender != null) {
                mStartPointRender.onDrawFrame();
            }
            synchronized (mRendersToDestroy) {
                for (GLRender renderer : mRendersToDestroy) {
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
            mStartPointRender.clearTargets();
            mStartPointRender.destroy();
        }
        mStartPointRender = null;

        synchronized (mFilterRenders) {
            for (FBORender filterRender : mFilterRenders) {
                filterRender.destroy();
            }
            mFilterRenders.clear();
        }

        synchronized (mOutputs) {
            for (BufferOutput bufferOutput : mOutputs) {
                bufferOutput.destroy();
            }
            mOutputs.clear();
        }

        synchronized (mEndPointRenders) {
            for (GLRender endPointRender : mEndPointRenders) {
                endPointRender.destroy();
            }
            mEndPointRenders.clear();
        }

        synchronized (mOnSurfaceListeners) {
            for (OnSurfaceListener listener : mOnSurfaceListeners) {
                listener.onSurfaceDestroyed();
            }
        }
    }

    public interface OnSurfaceListener {

        default void onSurfaceCreated(GL10 gl, EGLConfig config) {
        }

        default void onSurfaceChanged(GL10 gl, int width, int height) {
        }

        default void onSurfaceDestroyed() {
        }
    }

    public interface OnFilterRendersChangedListener {

        default void onFilterRendersChanged() {
        }
    }

    /**
     * 清空渲染管道
     */
    public synchronized void clean() {
//        boolean isRenders = isRendering();
//        setRendering(false); // 暂时停止渲染，构建渲染链完成后再进行渲染

        mCurrentRotation = 0;

        if (mStartPointRender != null) {
            mStartPointRender.clearTargets();
            addRenderToDestroy(mStartPointRender);
        }
        mStartPointRender = null;

        synchronized (mFilterRenders) {
            for (FBORender filterRender : mFilterRenders) {
                addRenderToDestroy(filterRender);
            }
            mFilterRenders.clear();
        }

        synchronized (mOutputs) {
            for (BufferOutput bufferOutput : mOutputs) {
                addRenderToDestroy(bufferOutput);
            }
            mOutputs.clear();
        }

        synchronized (mEndPointRenders) {
            for (GLRender endPointRender : mEndPointRenders) {
                addRenderToDestroy(endPointRender);
            }
            mEndPointRenders.clear();
        }

        synchronized (mRendersChangedListeners) {
            for (OnFilterRendersChangedListener listener : mRendersChangedListeners) {
                listener.onFilterRendersChanged();
            }
        }

//        setRendering(isRenders);
    }

    /**
     * 添加一个滤镜到销毁队列
     * 下一个onDrawFrame执行时，会调用销毁队列中的所有滤镜的destroy方法，并清空销毁队列
     *
     * @param render
     */
    public void addRenderToDestroy(GLRender render) {
        synchronized (mRendersToDestroy) {
            mRendersToDestroy.add(render);
        }
    }

    public void addOnFilterRendersChangedListener(OnFilterRendersChangedListener listener) {
        synchronized (mRendersChangedListeners) {
            if (!mRendersChangedListeners.contains(listener)) {
                mRendersChangedListeners.add(listener);
            }
        }
    }

    public void removeOnFilterRendersChangedListener(OnFilterRendersChangedListener listener) {
        synchronized (mRendersChangedListeners) {
            if (mRendersChangedListeners.contains(listener)) {
                mRendersChangedListeners.remove(listener);
            }
        }
    }

    public void clearOnFilterRendersChangedListener() {
        synchronized (mRendersChangedListeners) {
            mRendersChangedListeners.clear();
        }
    }

    /**
     * 添加OnSurfaceListener监听
     *
     * @param listener
     */
    public void addOnSurfaceListener(OnSurfaceListener listener) {
        synchronized (mOnSurfaceListeners) {
            if (!mOnSurfaceListeners.contains(listener)) {
                mOnSurfaceListeners.add(listener);
            }
        }
    }

    /**
     * 删除OnSurfaceListener监听
     *
     * @param listener
     */
    public void removeOnSurfaceListener(OnSurfaceListener listener) {
        synchronized (mOnSurfaceListeners) {
            if (mOnSurfaceListeners.contains(listener)) {
                mOnSurfaceListeners.remove(listener);
            }
        }
    }

    /**
     * 清空OnSurfaceListener监听列表
     */
    public void clearOnSurfaceListener() {
        synchronized (mOnSurfaceListeners) {
            mOnSurfaceListeners.clear();
        }
    }

    public int getHeight() {
        return mHeight;
    }

    public int getWidth() {
        return mWidth;
    }

    /**
     * 设置顺时针旋转90度的次数
     * 取值-3~0，0~3，表示旋转-270~0，0~270度
     *
     * @param numOfTimes 旋转次数
     */
    public void setRotate90Degrees(int numOfTimes) {
        mCurrentRotation = numOfTimes;
        synchronized (mEndPointRenders) {
            for (GLRender endPointRender : mEndPointRenders) {
                endPointRender.resetRotate();
                endPointRender.setRotate90Degrees(numOfTimes);
            }
        }

        synchronized (mOutputs) {
            for (BufferOutput bufferOutput : mOutputs) {
                bufferOutput.setRotate90Degrees(numOfTimes);
            }
        }
    }

    public void setRenderSize(int width, int height) {
        mWidth = width;
        mHeight = height;

        updateEndPointRender();
    }

    private void updateEndPointRender() {
        synchronized (mEndPointRenders) {
            for (GLRender endPointRender : mEndPointRenders) {
//                if (endPointRender instanceof EndPointRender) {
                endPointRender.setRenderSize(mWidth, mHeight);
//                }
            }
        }
    }

    /**
     * 是否正在渲染
     *
     * @return
     */
    public boolean isRendering() {
        return mIsRendering;
    }

    /**
     * 设置是否渲染
     *
     * @param rendering
     */
    public void setRendering(boolean rendering) {
        mIsRendering = rendering;
    }

    /**
     * 开始渲染
     */
    public void startRender() {
        mIsRendering = true;
    }

    /**
     * 暂停渲染
     */
    public void pauseRender() {
        mIsRendering = false;
    }

    /**
     * 获取起点渲染器
     *
     * @return
     */
    public FBORender getStartPointRender() {
        return mStartPointRender;
    }

    /**
     * 获取滤镜列表
     *
     * @return
     */
    public List<FBORender> getFilterRenders() {
        return mFilterRenders;
    }

    /**
     * 获取终点渲染器列表
     *
     * @return
     */
    public List<GLRender> getEndPointRenders() {
        return mEndPointRenders;
    }

    /**
     * 设置渲染起点
     *
     * @param startPointRenderer
     */
    public synchronized void setStartPointRender(FBORender startPointRenderer) {
        if (startPointRenderer != null && mStartPointRender != startPointRenderer) {
//            boolean isRenders = isRendering();
//            setRendering(false); // 暂时停止渲染，构建渲染链完成后再进行渲染

            if (mStartPointRender != null) {
                synchronized (mStartPointRender.getTargets()) {
                    for (OnTextureAcceptableListener render : mStartPointRender.getTargets()) {
                        startPointRenderer.addTarget(render);
                    }
                }
                mStartPointRender.clearTargets();
                addRenderToDestroy(mStartPointRender);
                mStartPointRender = startPointRenderer;
            } else {
                mStartPointRender = startPointRenderer;
                synchronized (mEndPointRenders) {
                    for (GLRender endPointRender : mEndPointRenders) {
                        mStartPointRender.addTarget(endPointRender);
                    }
                }
            }

//            setRendering(isRenders);
        }
    }

    /**
     * 添加终点渲染器
     *
     * @param endPointRender
     */
    public synchronized void addEndPointRender(GLRender endPointRender) {
        synchronized (mEndPointRenders) {
            if (endPointRender != null && !mEndPointRenders.contains(endPointRender)
                    && mStartPointRender != null) {
//                boolean isRenders = isRendering();
//                setRendering(false); // 暂时停止渲染，构建渲染链完成后再进行渲染

                endPointRender.resetRotate();
                endPointRender.setRotate90Degrees(mCurrentRotation);

                if (mFilterRenders.isEmpty()) {
                    mStartPointRender.addTarget(endPointRender);
                } else {
                    FBORender filterRender = mFilterRenders.get(mFilterRenders.size() - 1);
                    filterRender.addTarget(endPointRender);
                }

                mEndPointRenders.add(endPointRender);

                updateEndPointRender();

//                setRendering(isRenders);
            }
        }
    }

    /**
     * 删除终点渲染器
     *
     * @param endPointRender
     */
    public synchronized void removeEndPointRender(GLRender endPointRender) {
        synchronized (mEndPointRenders) {
            if (endPointRender != null && mEndPointRenders.contains(endPointRender)
                    && mStartPointRender != null) {
//                boolean isRenders = isRendering();
//                setRendering(false); // 暂时停止渲染，构建渲染链完成后再进行渲染

                mEndPointRenders.remove(endPointRender);

                if (mFilterRenders.isEmpty()) {
                    mStartPointRender.removeTarget(endPointRender);
                } else {
                    FBORender filterRender = mFilterRenders.get(mFilterRenders.size() - 1);
                    filterRender.removeTarget(endPointRender);
                }
                addRenderToDestroy(endPointRender);

//                setRendering(isRenders);
            }
        }
    }

    /**
     * 清空终点着色器列表
     */
    public synchronized void clearEndPointRenders() {
        synchronized (mEndPointRenders) {
            if (mStartPointRender != null && !mEndPointRenders.isEmpty()) {
//                boolean isRenders = isRendering();
//                setRendering(false); // 暂时停止渲染，构建渲染链完成后再进行渲染

                if (mFilterRenders.isEmpty()) {
                    for (GLRender endPointRender : mEndPointRenders) {
                        mStartPointRender.removeTarget(endPointRender);
                    }
                } else {
                    FBORender filterRender = mFilterRenders.get(mFilterRenders.size() - 1);
                    for (GLRender endPointRender : mEndPointRenders) {
                        filterRender.removeTarget(endPointRender);
                    }
                }
                for (GLRender endPointRender : mEndPointRenders) {
                    addRenderToDestroy(endPointRender);
                }
                mEndPointRenders.clear();

//                setRendering(isRenders);
            }
        }
    }

    public synchronized void addOutput(FBORender filterRender, BufferOutput bufferOutput) {
        synchronized (mOutputs) {
            if (bufferOutput != null && !mOutputs.contains(bufferOutput)
                    && mStartPointRender != null) {
//                boolean isRenders = isRendering();
//                setRendering(false); // 暂时停止渲染，构建渲染链完成后再进行渲染

                bufferOutput.clearTargets();
                bufferOutput.resetRotate();
                bufferOutput.setRotate90Degrees(mCurrentRotation);

                filterRender.addTarget(bufferOutput);
                mOutputs.add(bufferOutput);

//                setRendering(isRenders);
            }
        }
    }

    public synchronized void removeOutput(FBORender filterRender, BufferOutput bufferOutput) {
        synchronized (mOutputs) {
            if (filterRender != null && mOutputs.contains(bufferOutput)
                    && mStartPointRender != null) {
//                boolean isRenders = isRendering();
//                setRendering(false); // 暂时停止渲染，构建渲染链完成后再进行渲染

                mOutputs.remove(bufferOutput);

                filterRender.removeTarget(bufferOutput);
                addRenderToDestroy(bufferOutput);

//                setRendering(isRenders);
            }
        }
    }

    /**
     * 添加滤镜
     *
     * @param index
     * @param filterRender
     */
    public synchronized void addFilterRender(int index, FBORender filterRender) {
        synchronized (mFilterRenders) {
            if (filterRender != null && !mFilterRenders.contains(filterRender)
                    && mStartPointRender != null) {
//                boolean isRenders = isRendering();
//                setRendering(false); // 暂时停止渲染，构建渲染链完成后再进行渲染

                filterRender.clearTargets(); // 确保要添加的滤镜是干净的

                if (mFilterRenders.isEmpty()) {
                    mStartPointRender.addTarget(filterRender);
                    // 添加了第一个滤镜
                    synchronized (mEndPointRenders) {
                        for (GLRender endPointRender : mEndPointRenders) {
                            mStartPointRender.removeTarget(endPointRender);
                            filterRender.addTarget(endPointRender);
                        }
                    }
                } else {
                    if (index == 0) {
                        // 添加到滤镜列表开头
                        FBORender nextRender = mFilterRenders.get(0);
                        mStartPointRender.removeTarget(nextRender);
                        filterRender.addTarget(nextRender);
                        mStartPointRender.addTarget(filterRender);
                    } else if (index > mFilterRenders.size() - 1) {
                        // 添加到滤镜列表最后
                        FBORender prevRender = mFilterRenders.get(mFilterRenders.size() - 1);
                        prevRender.addTarget(filterRender);
                        synchronized (mEndPointRenders) {
                            for (GLRender endPointRender : mEndPointRenders) {
                                prevRender.removeTarget(endPointRender);
                                filterRender.addTarget(endPointRender);
                            }
                        }
                    } else {
                        // 添加到滤镜列表中间
                        FBORender prevRender = mFilterRenders.get(index - 1);
                        FBORender nextRender = mFilterRenders.get(index);
                        prevRender.removeTarget(nextRender);
                        prevRender.addTarget(filterRender);
                        filterRender.addTarget(nextRender);
                    }
                }
                if (index > mFilterRenders.size() - 1) {
                    mFilterRenders.add(filterRender);
                } else {
                    mFilterRenders.add(index, filterRender);
                }

                synchronized (mRendersChangedListeners) {
                    for (OnFilterRendersChangedListener listener : mRendersChangedListeners) {
                        listener.onFilterRendersChanged();
                    }
                }

//                setRendering(isRenders);
            }
        }
    }

    /**
     * 添加滤镜
     *
     * @param filterRender
     */
    public synchronized void addFilterRender(FBORender filterRender) {
        addFilterRender(mFilterRenders.size(), filterRender);
    }

    /**
     * 删除滤镜
     *
     * @param filterRender
     */
    public synchronized void removeFilterRender(FBORender filterRender) {
        synchronized (mFilterRenders) {
            if (filterRender != null && mFilterRenders.contains(filterRender)
                    && mStartPointRender != null) {
//                boolean isRenders = isRendering();
//                setRendering(false); // 暂时停止渲染，构建渲染链完成后再进行渲染

                int index = mFilterRenders.indexOf(filterRender);
                mFilterRenders.remove(filterRender);
                if (mFilterRenders.isEmpty()) {
                    // 删除了最后一个滤镜
                    mStartPointRender.removeTarget(filterRender);
                    synchronized (mEndPointRenders) {
                        for (GLRender endPointRender : mEndPointRenders) {
                            filterRender.removeTarget(endPointRender);
                            mStartPointRender.addTarget(endPointRender);
                        }
                    }
                } else {
                    if (index == 0) {
                        FBORender nextRender = mFilterRenders.get(0);
                        mStartPointRender.removeTarget(filterRender);
                        filterRender.removeTarget(nextRender);
                        mStartPointRender.addTarget(nextRender);
                    } else if (index > mFilterRenders.size() - 1) {
                        FBORender prevRender = mFilterRenders.get(mFilterRenders.size() - 1);
                        prevRender.removeTarget(filterRender);
                        synchronized (mEndPointRenders) {
                            for (GLRender endPointRender : mEndPointRenders) {
                                filterRender.removeTarget(endPointRender);
                                prevRender.addTarget(endPointRender);
                            }
                        }
                    } else {
                        FBORender prevRender = mFilterRenders.get(index - 1);
                        FBORender nextRender = mFilterRenders.get(index);
                        prevRender.removeTarget(filterRender);
                        filterRender.removeTarget(nextRender);
                        prevRender.addTarget(nextRender);
                    }
                }
                addRenderToDestroy(filterRender);

                synchronized (mRendersChangedListeners) {
                    for (OnFilterRendersChangedListener listener : mRendersChangedListeners) {
                        listener.onFilterRendersChanged();
                    }
                }

//                setRendering(isRenders);
            }
        }
    }

    /**
     * 清空滤镜列表
     */
    public synchronized void clearFilterRenders() {
        synchronized (mFilterRenders) {
            if (mStartPointRender != null && !mFilterRenders.isEmpty()) {
//                boolean isRenders = isRendering();
//                setRendering(false); // 暂时停止渲染，构建渲染链完成后再进行渲染

                if (mFilterRenders.size() == 1) {
                    FBORender filterRender = mFilterRenders.get(0);
                    mStartPointRender.removeTarget(filterRender);
                    synchronized (mEndPointRenders) {
                        for (GLRender endPointRender : mEndPointRenders) {
                            filterRender.removeTarget(endPointRender);
                            mStartPointRender.addTarget(endPointRender);
                        }
                    }
                } else {
                    FBORender firstFilterRender = mFilterRenders.get(0);
                    FBORender lastFilterRender = mFilterRenders.get(mFilterRenders.size() - 1);
                    mStartPointRender.removeTarget(firstFilterRender);
                    synchronized (mEndPointRenders) {
                        for (GLRender endPointRender : mEndPointRenders) {
                            lastFilterRender.removeTarget(endPointRender);
                            mStartPointRender.addTarget(endPointRender);
                        }
                    }
                }
                for (FBORender filterRender : mFilterRenders) {
                    addRenderToDestroy(filterRender);
                }
                mFilterRenders.clear();

                synchronized (mRendersChangedListeners) {
                    for (OnFilterRendersChangedListener listener : mRendersChangedListeners) {
                        listener.onFilterRendersChanged();
                    }
                }

//                setRendering(isRenders);
            }
        }
    }

    /**
     * 异步截图
     *
     * @param callback 异步获取截图回调
     */
    public void output(final BitmapOutput.BitmapOutputCallback callback) {
        output(callback, true);
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
        FBORender render;
        if (withFilter && !mFilterRenders.isEmpty()) {
            render = mFilterRenders.get(mFilterRenders.size() - 1);
        } else {
            render = mStartPointRender;
        }

        final FBORender fRender = render;
        final BitmapOutput bitmapOutput = new BitmapOutput();
        bitmapOutput.setRenderSize(width, height);
        bitmapOutput.setBitmapOutputCallback(bitmap -> {
            if (callback != null) {
                callback.bitmapOutput(bitmap);
            }

            removeOutput(fRender, bitmapOutput);
        });
        addOutput(render, bitmapOutput);
    }
}

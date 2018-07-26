package cn.ezandroid.ezfilter.split;

import android.opengl.GLES20;

import java.util.ArrayList;
import java.util.List;

import cn.ezandroid.ezfilter.core.FBORender;
import cn.ezandroid.ezfilter.core.GLRender;
import cn.ezandroid.ezfilter.core.OnTextureAcceptableListener;
import cn.ezandroid.ezfilter.core.RenderPipeline;
import cn.ezandroid.ezfilter.extra.CropRender;

/**
 * 拆分渲染器
 *
 * @author like
 * @date 2018-07-18
 */
public class SplitInput extends FBORender {

    protected int mNumOfInputs;
    protected int[] mMultiTextureHandle;
    protected int[] mMultiTexture;
    protected List<FBORender> mStartPointRenders;
    protected List<FBORender> mEndPointRenders;

    protected FBORender mRootRender;
    protected List<CropRender> mCropRenders;

    protected List<RenderPipeline> mRenderPipelines; // 每个输入源都生成一个渲染管道，便于对输入源各自添加滤镜

    public SplitInput(List<CropRender> cropRenders) {
        super();
        this.mCropRenders = cropRenders;
        mNumOfInputs = cropRenders.size();
        mMultiTextureHandle = new int[mNumOfInputs - 1];
        mMultiTexture = new int[mNumOfInputs - 1];
        mStartPointRenders = new ArrayList<>(mNumOfInputs);
        mEndPointRenders = new ArrayList<>(mNumOfInputs);
        mRenderPipelines = new ArrayList<>(mNumOfInputs);
    }

    public void setRootRender(FBORender rootRender) {
        if (rootRender != null && mRootRender != rootRender) {
            if (mRootRender != null) {
                synchronized (mRootRender.getTargets()) {
                    for (OnTextureAcceptableListener render : mRootRender.getTargets()) {
                        rootRender.addTarget(render);
                    }
                }
                mRootRender.clearTargets();

                // 在GL线程销毁之前的根渲染器
                FBORender fboRender = mRootRender;
                runOnDraw(fboRender::destroy);

                mRootRender = rootRender;
            } else {
                mRootRender = rootRender;

                clearRegisteredFilters();
                for (CropRender cropRender : mCropRenders) {
                    registerFilter(cropRender);
                }
            }
        }
    }

    @Override
    public synchronized void onTextureAcceptable(int texture, GLRender source) {
        int pos = mEndPointRenders.lastIndexOf(source);
        if (pos <= 0) {
            mTextureIn = texture;
        } else {
            this.mMultiTexture[pos - 1] = texture;
        }
    }

    @Override
    protected void initShaderHandles() {
        super.initShaderHandles();
        for (int i = 0; i < mNumOfInputs - 1; i++) {
            // 从2开始：如inputImageTexture2，inputImageTexture3...
            mMultiTextureHandle[i] = GLES20.glGetUniformLocation(mProgramHandle, UNIFORM_TEXTURE + (i + 2));
        }
    }

    @Override
    protected void bindShaderValues() {
        super.bindShaderValues();
        for (int i = 0; i < mNumOfInputs - 1; i++) {
            if (mMultiTexture[i] != 0) {
                int tex = GLES20.GL_TEXTURE1 + i;
                GLES20.glActiveTexture(tex);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mMultiTexture[i]);
                GLES20.glUniform1i(mMultiTextureHandle[i], i + 1);
            }
        }
    }

    public void clearRegisteredFilters() {
        mRootRender.clearTargets();
        mStartPointRenders.clear();
        for (FBORender render : mEndPointRenders) {
            render.removeTarget(this);
        }
        mEndPointRenders.clear();
        mRenderPipelines.clear();
    }

    public void registerFilter(FBORender filter) {
        if (!mStartPointRenders.contains(filter)) {
            mRootRender.addTarget(filter);

            FBORender endRender = new FBORender();
            endRender.addTarget(this);
            mStartPointRenders.add(filter);
            mEndPointRenders.add(endRender);
            RenderPipeline renderPipeline = new RenderPipeline();
            renderPipeline.onSurfaceCreated(null, null);
            renderPipeline.onSurfaceChanged(null, filter.getWidth(), filter.getHeight());
            renderPipeline.setStartPointRender(filter);
            renderPipeline.addEndPointRender(endRender);
            renderPipeline.startRender();
            mRenderPipelines.add(renderPipeline);
        }
    }

    public List<FBORender> getStartPointRenders() {
        return mStartPointRenders;
    }

    public List<FBORender> getEndPointRenders() {
        return mEndPointRenders;
    }

    public List<RenderPipeline> getRenderPipelines() {
        return mRenderPipelines;
    }

    public FBORender getRootRender() {
        return mRootRender;
    }

    @Override
    public void onDrawFrame() {
        if (mRootRender != null) {
            mRootRender.onDrawFrame();
        }
        super.onDrawFrame();
    }

    @Override
    public void destroy() {
        super.destroy();
        if (mRootRender != null) {
            mRootRender.destroy();
        }
        for (RenderPipeline pipeline : mRenderPipelines) {
            pipeline.onSurfaceDestroyed();
        }
    }
}

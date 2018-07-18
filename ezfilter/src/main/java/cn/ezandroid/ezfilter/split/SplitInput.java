package cn.ezandroid.ezfilter.split;

import android.opengl.GLES20;

import java.util.ArrayList;
import java.util.List;

import cn.ezandroid.ezfilter.core.FBORender;
import cn.ezandroid.ezfilter.core.GLRender;
import cn.ezandroid.ezfilter.core.RenderPipeline;
import cn.ezandroid.ezfilter.extra.CropRender;

/**
 * 拆分渲染器
 *
 * @author like
 * @date 2018-07-18
 */
public class SplitInput extends FBORender {

    private int mNumOfInputs;
    private int[] mMultiTextureHandle;
    private int[] mMultiTexture;
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
        mRootRender = rootRender;

        clearRegisteredFilterLocations();
        for (CropRender cropRender : mCropRenders) {
            registerFilterLocation(cropRender);
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
            mMultiTextureHandle[i] = GLES20.glGetUniformLocation(mProgramHandle,
                    UNIFORM_TEXTURE + (i + 2));
            // 从2开始：如inputImageTexture2，inputImageTexture3...
        }
    }

    @Override
    protected void bindShaderValues() {
        super.bindShaderValues();
        for (int i = 0; i < mNumOfInputs - 1; i++) {
            int tex = GLES20.GL_TEXTURE1 + i;
            GLES20.glActiveTexture(tex);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mMultiTexture[i]);
            GLES20.glUniform1i(mMultiTextureHandle[i], i + 1);
        }
    }

    private void clearRegisteredFilterLocations() {
        mRootRender.clearTargets();
        for (FBORender render : mEndPointRenders) {
            render.removeTarget(this);
        }
        mStartPointRenders.clear();
        mEndPointRenders.clear();
        mRenderPipelines.clear();
    }

    private void registerFilterLocation(FBORender filter) {
        if (!mStartPointRenders.contains(filter)) {
            mRootRender.addTarget(filter);

            FBORender endRender = new FBORender();
            endRender.addTarget(this);
            mStartPointRenders.add(filter);
            mEndPointRenders.add(endRender);
            RenderPipeline renderPipeline = new RenderPipeline();
            renderPipeline.onSurfaceCreated(null, null);
            renderPipeline.onSurfaceChanged(null, getWidth(), getHeight());
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
        super.onDrawFrame();
        if (mRootRender != null) {
            mRootRender.onDrawFrame();
        }
//        for (RenderPipeline pipeline : mRenderPipelines) {
//            pipeline.onDrawFrame(null);
//        }
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

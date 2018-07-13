package cn.ezandroid.ezfilter.multi;

import android.opengl.GLES20;

import java.util.ArrayList;
import java.util.List;

import cn.ezandroid.ezfilter.core.FBORender;
import cn.ezandroid.ezfilter.core.FilterRender;
import cn.ezandroid.ezfilter.core.GLRender;

/**
 * 多输入源渲染器
 *
 * @author like
 * @date 2018-07-13
 */
public class MultiInput extends FilterRender {

    private int mNumOfInputs;
    private int[] mMultiTextureHandle;
    private int[] mMultiTexture;
    private List<FBORender> mFilterLocations;

    public MultiInput(int numOfInputs) {
        super();
        this.mNumOfInputs = numOfInputs;
        mMultiTextureHandle = new int[numOfInputs - 1];
        mMultiTexture = new int[numOfInputs - 1];
        mFilterLocations = new ArrayList<>(numOfInputs);
    }

    @Override
    public synchronized void onTextureAcceptable(int texture, GLRender source) {
        int pos = mFilterLocations.lastIndexOf(source);
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

    public void clearRegisteredFilterLocations() {
        for (FBORender render : mFilterLocations) {
            render.removeTarget(this);
        }
        mFilterLocations.clear();
    }

    public void registerFilterLocation(FBORender filter) {
        if (!mFilterLocations.contains(filter)) {
            filter.addTarget(this);
            mFilterLocations.add(filter);
        }
    }

    public void registerFilterLocation(FBORender filter, int location) {
        if (mFilterLocations.contains(filter)) {
            filter.removeTarget(this);
            mFilterLocations.remove(filter);
        }
        filter.addTarget(this);
        mFilterLocations.add(location, filter);
    }

    public List<FBORender> getFilterLocations() {
        return mFilterLocations;
    }

    @Override
    public void onDrawFrame() {
        super.onDrawFrame();
        for (FBORender render : mFilterLocations) {
            render.onDrawFrame();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        for (FBORender render : mFilterLocations) {
            render.destroy();
        }
    }
}

package cn.ezandroid.ezfilter.extra;

import android.opengl.GLES20;

import java.util.ArrayList;
import java.util.List;

import cn.ezandroid.ezfilter.core.FBORender;
import cn.ezandroid.ezfilter.core.FilterRender;

/**
 * 多输入滤镜渲染器
 * <p>
 * 支持多个纹理作为输入
 *
 * @author like
 * @date 2017-09-15
 */
public class MultiInputFilterRender extends FilterRender {

    private int mNumOfInputs;
    private int[] mMultiTextureHandle;
    private int[] mMultiTexture;
    protected List<FBORender> mTexturesReceived;
    protected List<FBORender> mFilterLocations;

    public MultiInputFilterRender(int numOfInputs) {
        super();
        this.mNumOfInputs = numOfInputs;
        mMultiTextureHandle = new int[numOfInputs - 1];
        mMultiTexture = new int[numOfInputs - 1];
        mTexturesReceived = new ArrayList<>(numOfInputs);
        mFilterLocations = new ArrayList<>(numOfInputs);
    }

    @Override
    public synchronized void onTextureAvailable(int texture, FBORender source) {
        if (!mTexturesReceived.contains(source)) {
            mTexturesReceived.add(source);
        }
        int pos = mFilterLocations.lastIndexOf(source);
        if (pos <= 0) {
            mTextureIn = texture;
        } else {
            this.mMultiTexture[pos - 1] = texture;
        }
        if (mTexturesReceived.size() == mNumOfInputs) {
            setWidth(source.getWidth());
            setHeight(source.getHeight());
            onDrawFrame();
            mTexturesReceived.clear();
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
        mFilterLocations.clear();
    }

    public void registerFilterLocation(FBORender filter) {
        if (!mFilterLocations.contains(filter)) {
            mFilterLocations.add(filter);
        }
    }

    public void registerFilterLocation(FBORender filter, int location) {
        if (mFilterLocations.contains(filter)) {
            mFilterLocations.remove(filter);
        }
        mFilterLocations.add(location, filter);
    }
}

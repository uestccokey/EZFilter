package cn.ezandroid.ezfilter.extra;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.util.Log;

import cn.ezandroid.ezfilter.core.FBORender;
import cn.ezandroid.ezfilter.core.FilterRender;
import cn.ezandroid.ezfilter.util.BitmapUtil;
import cn.ezandroid.ezfilter.util.TextureBindUtil;

/**
 * 多图片输入滤镜渲染器
 * <p>
 * 支持多个Bitmap作为输入
 *
 * @author like
 * @date 2017-09-17
 */
public class MultiBitmapInputRender extends FilterRender {

    protected int[] mTextureHandles;
    protected int[] mTextures;
    protected Bitmap[] mBitmaps;
    protected int mTextureNum;

    protected Context mContext;
    protected int[] mResources;
    protected String[] mPaths;

    public MultiBitmapInputRender(Context context, Bitmap[] bmps) {
        mContext = context;
        if (bmps != null) {
            // 输入纹理和上级Filter的纹理总数
            mTextureNum = bmps.length + 1;
            // 输入纹理
            mTextureHandles = new int[bmps.length];
            mTextures = new int[bmps.length];
            mBitmaps = bmps;
        } else {
            mTextureNum = 1;
        }
    }

    public MultiBitmapInputRender(Context context, int[] bmpIds) {
        mContext = context;
        if (bmpIds != null) {
            // 输入纹理和上级Filter的纹理总数
            mTextureNum = bmpIds.length + 1;
            // 输入纹理
            mTextureHandles = new int[bmpIds.length];
            mTextures = new int[bmpIds.length];
            mBitmaps = new Bitmap[bmpIds.length];
            mResources = bmpIds;
        } else {
            mTextureNum = 1;
        }
    }

    public MultiBitmapInputRender(Context context, String[] bmpPaths) {
        mContext = context;
        if (bmpPaths != null) {
            // 输入纹理和上级Filter的纹理总数
            mTextureNum = bmpPaths.length + 1;
            // 输入纹理
            mTextureHandles = new int[bmpPaths.length];
            mTextures = new int[bmpPaths.length];
            mBitmaps = new Bitmap[bmpPaths.length];
            mPaths = bmpPaths;
        } else {
            mTextureNum = 1;
        }
    }

    private void destroyTextures() {
        if (mTextures != null) {
//            GLES20.glDeleteTextures(mTextures.length, mTextures, 0); // 某些手机上会花屏，所以不用这种方式
            for (int i = 0; i < mTextures.length; i++) {
                if (mTextures[i] != 0) {
                    int[] tex = new int[1];
                    tex[0] = mTextures[i];
                    GLES20.glDeleteTextures(1, tex, 0);
                    mTextures[i] = 0;
                }
            }
        }
        if (mBitmaps != null) {
            for (Bitmap bitmap : mBitmaps) {
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
                bitmap = null;
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        destroyTextures();
    }

    @Override
    public void onTextureAvailable(int texture, FBORender source) {
        long time = System.currentTimeMillis();
        mTextureIn = texture;

        if (mTextureNum > 1) {
            // TODO 创建多个图片太耗时，需要优化
            try {
                for (int i = 0; i < mBitmaps.length; i++) {
                    if (mBitmaps[i] == null || mBitmaps[i].isRecycled()) {
                        if (mResources != null) {
                            final BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inScaled = false;
                            options.inDither = false;
                            options.inInputShareable = true;
                            options.inPurgeable = true;
                            mBitmaps[i] = BitmapFactory.decodeResource(mContext.getResources(),
                                    mResources[i], options);
                        } else if (mPaths != null) {
                            mBitmaps[i] = BitmapUtil.loadImage(mContext, mPaths[i]);
                        }
                    }
                }
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
            }

            // 只bind一次，避免每次都去bindBitmap耗时
            for (int i = 0; i < mBitmaps.length; i++) {
                if (mBitmaps[i] != null && !mBitmaps[i].isRecycled()) {
                    if (mTextures[i] == 0) {
                        mTextures[i] = TextureBindUtil.bindBitmap(mBitmaps[i]);
                    }
                }
            }
        }

        setWidth(source.getWidth());
        setHeight(source.getHeight());
        onDrawFrame();
        Log.e("Render", getClass() + "渲染耗时:" + (System.currentTimeMillis() - time));
    }

    @Override
    protected void initShaderHandles() {
        super.initShaderHandles();
        for (int i = 0; i < mTextureNum - 1; i++) {
            mTextureHandles[i] = GLES20.glGetUniformLocation(mProgramHandle,
                    UNIFORM_TEXTURE + (i + 2));
            // 从2开始：如inputImageTexture2，inputImageTexture3...
        }
    }

    @Override
    protected void bindShaderValues() {
        super.bindShaderValues();
        for (int i = 0; i < mTextureNum - 1; i++) {
            int tex = GLES20.GL_TEXTURE1 + i;
            GLES20.glActiveTexture(tex);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[i]);
            GLES20.glUniform1i(mTextureHandles[i], i + 1);
        }
    }
}

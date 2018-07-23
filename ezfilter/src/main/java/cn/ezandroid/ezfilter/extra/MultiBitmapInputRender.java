package cn.ezandroid.ezfilter.extra;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.text.TextUtils;

import cn.ezandroid.ezfilter.core.FilterRender;
import cn.ezandroid.ezfilter.core.GLRender;
import cn.ezandroid.ezfilter.core.util.BitmapUtil;
import cn.ezandroid.ezfilter.core.util.Path;

/**
 * 多图片输入滤镜渲染器
 * <p>
 * 支持将多个Bitmap作为输入
 *
 * @author like
 * @date 2017-09-17
 */
public class MultiBitmapInputRender extends FilterRender {

    protected int[] mTextureHandles;
    protected int[] mTextures;
    protected Bitmap[] mBitmaps;
    protected int mTextureNum = 1;

    protected Context mContext;
    protected int[] mResources;
    protected String[] mPaths;

    public MultiBitmapInputRender() {
    }

    public MultiBitmapInputRender(Context context, Bitmap[] bmps) {
        setImages(context, bmps);
    }

    public void setImages(Context context, Bitmap[] bmps) {
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
        setImages(context, bmpIds);
    }

    public void setImages(Context context, int[] bmpIds) {
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
        setImages(context, bmpPaths);
    }

    public void setImages(Context context, String[] bmpPaths) {
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
            // 某些手机上会花屏，所以不用这种方式
//            GLES20.glDeleteTextures(mTextures.length, mTextures, 0);
            for (int i = 0; i < mTextures.length; i++) {
                if (mTextures[i] != 0) {
                    int[] tex = new int[1];
                    tex[0] = mTextures[i];
                    GLES20.glDeleteTextures(1, tex, 0);
                    mTextures[i] = 0;
                }
            }
        }

        // 由于有图片缓存，这里不直接释放
//        if (mBitmaps != null) {
//            for (Bitmap bitmap : mBitmaps) {
//                if (bitmap != null && !bitmap.isRecycled()) {
//                    bitmap.recycle();
//                }
//                bitmap = null;
//            }
//        }
    }

    @Override
    public void destroy() {
        super.destroy();
        destroyTextures();
    }

    @Override
    public void onTextureAcceptable(int texture, GLRender source) {
        mTextureIn = texture;

        if (mTextureNum > 1) {
            // 只bind一次，直到destroy，避免每次都去loadBitmap和bindBitmap耗时
            for (int i = 0; i < mTextures.length; i++) {
                if (mTextures[i] == 0) {
                    String key = "";
                    if (mResources != null) {
                        int resource = mResources[i];
                        key = Path.DRAWABLE.wrap("" + resource);
                    } else if (mPaths != null) {
                        key = mPaths[i];
                    }

                    // 查找图片缓存，绑定纹理
                    if (mBitmaps[i] == null || mBitmaps[i].isRecycled()) {
                        if (mBitmapCache != null) {
                            Bitmap cachedBitmap = mBitmapCache.get(key);
                            if (cachedBitmap == null || cachedBitmap.isRecycled()) {
                                mBitmaps[i] = BitmapUtil.loadBitmap(mContext, key);
                                if (!TextUtils.isEmpty(key) && mBitmaps[i] != null && !mBitmaps[i].isRecycled()) {
                                    mBitmapCache.put(key, mBitmaps[i]);
                                }
                            } else {
                                mBitmaps[i] = cachedBitmap;
                            }
                        } else {
                            mBitmaps[i] = BitmapUtil.loadBitmap(mContext, key);
                        }
                    }
                    if (mBitmaps[i] != null && !mBitmaps[i].isRecycled()) {
                        mTextures[i] = BitmapUtil.bindBitmap(mBitmaps[i]);
                    }
                }
            }
        }

        setWidth(source.getWidth());
        setHeight(source.getHeight());
        onDrawFrame();
    }

    @Override
    protected void initShaderHandles() {
        super.initShaderHandles();
        for (int i = 0; i < mTextureNum - 1; i++) {
            // 从2开始：如inputImageTexture2，inputImageTexture3...
            mTextureHandles[i] = GLES20.glGetUniformLocation(mProgramHandle, UNIFORM_TEXTURE + (i + 2));
        }
    }

    @Override
    protected void bindShaderTextures() {
        super.bindShaderTextures();
        for (int i = 0; i < mTextureNum - 1; i++) {
            if (mTextures[i] != 0) {
                int tex = GLES20.GL_TEXTURE1 + i;
                GLES20.glActiveTexture(tex);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[i]);
                GLES20.glUniform1i(mTextureHandles[i], i + 1);
            }
        }
    }
}

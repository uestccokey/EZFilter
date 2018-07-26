package cn.ezandroid.ezfilter.image;

import android.graphics.Bitmap;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import cn.ezandroid.ezfilter.core.FBORender;
import cn.ezandroid.ezfilter.core.util.BitmapUtil;

/**
 * 图片输入
 *
 * @author like
 * @date 2017-09-15
 */
public class BitmapInput extends FBORender {

    private Bitmap mBitmap;
    private boolean mIsNewBitmap;

    public BitmapInput() {
        super();
    }

    public BitmapInput(Bitmap bitmap) {
        super();
        setBitmap(bitmap);
    }

    public void setBitmap(Bitmap bitmap) {
        loadImage(bitmap);
    }

    public Bitmap getBitmap() {
        return this.mBitmap;
    }

    /**
     * BitmapInput和AbstractRender设置的纹理顶点坐标是倒置的关系
     */
    @Override
    protected void initTextureVertices() {
        mTextureVertices = new FloatBuffer[4];

        // (0,0) -------> (1,0)
        //     ^
        //      \\
        //        \\
        //          \\
        //            \\
        // (0,1) -------> (1,1)
        float[] texData0 = new float[]{0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f,
                1.0f, 0.0f,};
        mTextureVertices[0] = ByteBuffer.allocateDirect(texData0.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTextureVertices[0].put(texData0).position(0);

        float[] texData1 = new float[]{0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f,
                1.0f, 1.0f,};
        mTextureVertices[1] = ByteBuffer.allocateDirect(texData1.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTextureVertices[1].put(texData1).position(0);

        float[] texData2 = new float[]{1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f,
                0.0f, 1.0f,};
        mTextureVertices[2] = ByteBuffer.allocateDirect(texData2.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTextureVertices[2].put(texData2).position(0);

        float[] texData3 = new float[]{1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 0.0f,};
        mTextureVertices[3] = ByteBuffer.allocateDirect(texData3.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTextureVertices[3].put(texData3).position(0);
    }

    @Override
    protected void drawFrame() {
        if (mIsNewBitmap) {
            loadTexture();
            mIsNewBitmap = false;
        }
        super.drawFrame();
    }

    private void loadImage(Bitmap bitmap) {
        this.mBitmap = bitmap;
        this.mIsNewBitmap = true;

        setRenderSize(bitmap.getWidth(), bitmap.getHeight());
    }

    private void loadTexture() {
        if (mTextureIn != 0) {
            int[] tex = new int[1];
            tex[0] = mTextureIn;
            GLES20.glDeleteTextures(1, tex, 0);
            mTextureIn = 0;
        }
        mTextureIn = BitmapUtil.bindBitmap(mBitmap);
    }

    @Override
    public void destroy() {
        super.destroy();
        if (mTextureIn != 0) {
            int[] tex = new int[1];
            tex[0] = mTextureIn;
            GLES20.glDeleteTextures(1, tex, 0);
            mTextureIn = 0;
        }
        mIsNewBitmap = true;
    }
}

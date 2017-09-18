package cn.ezandroid.ezfilter.io.output;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Bitmap输出
 *
 * @author like
 * @date 2017-09-17
 */
public class BitmapOutput extends BufferOutput<IntBuffer> {

    private BitmapOutputCallback mCallback;

    private Bitmap.Config mConfig = Bitmap.Config.ARGB_8888;

    /**
     * BitmapOutput和AbstractRender设置的纹理顶点坐标是倒置的关系
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
    public IntBuffer initBuffer(int width, int height) {
        int[] pixels = new int[width * height];
        IntBuffer intBuffer = IntBuffer.wrap(pixels);
        intBuffer.position(0);
        return intBuffer;
    }

    @Override
    public void bufferOutput(IntBuffer buffer) {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            if (mCallback != null) {
                mCallback.bitmapOutput(null);
            }
            return;
        }

        int[] pixels = buffer.array();

        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = (pixels[i] & (0xFF00FF00))
                    | ((pixels[i] >> 16) & 0x000000FF)
                    | ((pixels[i] << 16) & 0x00FF0000);
            // swap red and blue to translate back to bitmap rgb style
        }

        try {
            Bitmap image = Bitmap.createBitmap(pixels, width, height, mConfig);
            if (mCallback != null) {
                mCallback.bitmapOutput(image);
            }
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            if (mCallback != null) {
                mCallback.bitmapOutput(null);
            }
        }
    }

    public void setBitmapConfig(Bitmap.Config config) {
        this.mConfig = config;
    }

    public void setBitmapOutputCallback(BitmapOutputCallback callback) {
        this.mCallback = callback;
    }

    public interface BitmapOutputCallback {

        void bitmapOutput(Bitmap bitmap);
    }
}

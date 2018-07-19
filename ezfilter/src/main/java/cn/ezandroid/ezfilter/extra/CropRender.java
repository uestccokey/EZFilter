package cn.ezandroid.ezfilter.extra;

import android.graphics.RectF;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import cn.ezandroid.ezfilter.core.FBORender;
import cn.ezandroid.ezfilter.core.GLRender;

/**
 * 裁剪滤镜
 *
 * @author like
 * @date 2018-07-18
 */
public class CropRender extends FBORender {

    private RectF mRegion = new RectF(0, 0, 1, 1);

    /**
     * 设置裁剪区域
     *
     * @param region
     */
    public void setCropRegion(RectF region) {
        mRegion = region;

        FloatBuffer[] textureVertices = new FloatBuffer[4];

        float left = region.left;
        float right = region.right;
        float top = region.top;
        float bottom = region.bottom;

        // (left,bottom) -------> (right,bottom)
        //              ^
        //               \\
        //                \\
        //                 \\
        //                  \\
        // (left,   top) -------> (right,   top)
        // 正向纹理坐标
        float[] texData0 = new float[]{left, top, right, top, left, bottom,
                right, bottom,};
        textureVertices[0] = ByteBuffer.allocateDirect(texData0.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        textureVertices[0].put(texData0).position(0);

        // 顺时针旋转90°的纹理坐标
        float[] texData1 = new float[]{right, top, right, bottom, left, top,
                left, bottom,};
        textureVertices[1] = ByteBuffer.allocateDirect(texData1.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        textureVertices[1].put(texData1).position(0);

        // 顺时针旋转180°的纹理坐标
        float[] texData2 = new float[]{right, bottom, left, bottom, right, top,
                left, top,};
        textureVertices[2] = ByteBuffer.allocateDirect(texData2.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        textureVertices[2].put(texData2).position(0);

        // 顺时针旋转270°的纹理坐标
        float[] texData3 = new float[]{left, bottom, left, top, right, bottom,
                right, top,};
        textureVertices[3] = ByteBuffer.allocateDirect(texData3.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        textureVertices[3].put(texData3).position(0);

        setTextureVertices(textureVertices);
    }

    @Override
    public void onTextureAcceptable(int texture, GLRender source) {
        mTextureIn = texture;

        setWidth(Math.round(source.getWidth() * mRegion.width()));
        setHeight(Math.round(source.getHeight() * mRegion.height()));

        onDrawFrame();
    }
}

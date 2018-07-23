package cn.ezandroid.ezfilter.demo.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import cn.ezandroid.ezfilter.core.FBORender;
import cn.ezandroid.ezfilter.multi.MultiInput;

/**
 * HorizontalMultiInput
 *
 * @author like
 * @date 2018-07-13
 */
public class HorizontalMultiInput extends MultiInput {

    public HorizontalMultiInput() {
        super(2);
        setRenderSize(720, 640); // 固定为渲染尺寸为720x640
    }

    // 右侧是视频，保持宽高比并居中显示
    public void updateRightWorldVertices() {
        FBORender rightRender = mStartPointRenders.get(1);
        int width = rightRender.getWidth();
        int height = rightRender.getHeight();
        if (width != 0 && height != 0) {
            float aspectRatio = width * 1.0f / height;
            float widthScale = 1.0f;
            float heightScale = 1.0f;

            int renderWidth = getWidth() / 2;
            int renderHeight = getHeight();

            if (renderWidth * 1.0f / renderHeight > aspectRatio) {
                widthScale = renderHeight * 1.0f / height * width / renderWidth;
            } else {
                heightScale = renderWidth * 1.0f / width * height / renderHeight;
            }

            // (-1, 1) -------> (1,1)
            //      ^
            //       \\
            //         (0,0)
            //           \\
            //             \\
            // (-1,-1) -------> (1,-1)
            float[] vertices = new float[]{-widthScale, -heightScale,
                    widthScale, -heightScale,
                    -widthScale, heightScale,
                    widthScale, heightScale};
            FloatBuffer worldVertices = ByteBuffer.allocateDirect(vertices.length * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            worldVertices.put(vertices).position(0);

            rightRender.setWorldVertices(worldVertices);
        }
    }

    @Override
    public void registerFilter(FBORender filter) {
        filter.setRenderSize(getWidth() / 2, getHeight());
        super.registerFilter(filter);
    }

    @Override
    protected String getFragmentShader() {
        return "precision highp float;\n" +
                "varying highp vec2 textureCoordinate;\n" +
                "uniform sampler2D inputImageTexture;\n" +
                "uniform sampler2D inputImageTexture2;\n" +
                "void main()\n" +
                "{\n" +
                "     lowp vec2 newCoord = textureCoordinate;\n" +
                "     lowp vec4 outputColor;\n" +
                "     if (newCoord.x < 0.5) {\n" +
                "         newCoord.x = newCoord.x * 2.0;\n" +
                "         outputColor = texture2D(inputImageTexture, newCoord);\n" +
                "     } else {\n" +
                "         newCoord.x = (newCoord.x - 0.5) * 2.0;\n" +
                "         outputColor = texture2D(inputImageTexture2, newCoord);\n" +
                "     }\n" +
                "     gl_FragColor = outputColor;" +
                "}";
    }
}

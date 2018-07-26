package cn.ezandroid.ezfilter.demo.render;

import java.util.List;

import cn.ezandroid.ezfilter.extra.CropRender;
import cn.ezandroid.ezfilter.split.SplitInput;

/**
 * HorizontalSplitInput
 *
 * @author like
 * @date 2018-07-18
 */
public class HorizontalSplitInput extends SplitInput {

    public HorizontalSplitInput(List<CropRender> cropRenders) {
        super(cropRenders);
        setRenderSize(720, 640); // 固定为渲染尺寸为720x640
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

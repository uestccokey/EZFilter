package cn.ezandroid.ezfilter.demo.render;

import cn.ezandroid.ezfilter.core.FilterRender;

/**
 * 镜像滤镜
 *
 * @author like
 * @date 2017-09-16
 */
public class MirrorRender extends FilterRender {

    public MirrorRender() {
        setFragmentShader("precision highp float;\n" +
                "varying lowp vec2 textureCoordinate;\n" +
                "\n" +
                "uniform sampler2D inputImageTexture;\n" +
                "\n" +
                "void main(){\n" +
                "float gate = 0.01;\n" +
                "if(textureCoordinate.x < 0.5-gate)\n" +
                "{\n" +
                "   gl_FragColor = texture2D(inputImageTexture,textureCoordinate);\n" +
                "}\n" +
                "else if(textureCoordinate.x < 0.5+gate)\n" +
                "{\n" +
                "   float weight = (textureCoordinate.x + gate - 0.5) / (2.0 * gate);\n" +
                "   vec4 color1 = texture2D(inputImageTexture,textureCoordinate);\n" +
                "   vec4 color2 = texture2D(inputImageTexture,vec2(1.0 - textureCoordinate.x, " +
                "       textureCoordinate.y));\n" +
                "   gl_FragColor = mix(color1, color2, weight);\n" +
                "}\n" +
                "else\n" +
                "{\n" +
                "   gl_FragColor = texture2D(inputImageTexture,vec2(1.0 - textureCoordinate.x, " +
                "   textureCoordinate.y));\n" +
                "}\n" +
                "\n" +
                "}");
    }
}

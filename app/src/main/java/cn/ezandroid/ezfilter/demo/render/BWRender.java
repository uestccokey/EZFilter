package cn.ezandroid.ezfilter.demo.render;

import android.content.Context;
import android.opengl.GLES20;

import cn.ezandroid.ezfilter.core.util.Path;
import cn.ezandroid.ezfilter.extra.IAdjustable;
import cn.ezandroid.ezfilter.extra.MultiBitmapInputRender;

/**
 * 黑白滤镜
 *
 * @author like
 * @date 2017-09-17
 */
public class BWRender extends MultiBitmapInputRender implements IAdjustable {

    private static final String[] BITMAP_PATHS = new String[]{
            Path.ASSETS.wrap("filter/bw.png"),
//            Path.DRAWABLE.wrap("" + R.drawable.bw)
    };

    private static final String UNIFORM_MIX = "u_mix";
    private int mMixHandle;
    private float mMix = 1f; // 滤镜强度

    public BWRender(Context context) {
        super(context, BITMAP_PATHS);
        setFragmentShader("precision lowp float;\n" +
                " uniform lowp float u_mix;\n" +
                " \n" +
                " varying highp vec2 textureCoordinate;\n" +
                " \n" +
                " uniform sampler2D inputImageTexture;\n" +
                " uniform sampler2D inputImageTexture2;\n" +
                " \n" +
                " \n" +
                " void main()\n" +
                "{\n" +
                "    lowp vec4 sourceImageColor = texture2D(inputImageTexture, textureCoordinate)" +
                ";\n" +
                "    \n" +
                "    vec3 texel = texture2D(inputImageTexture, textureCoordinate).rgb;\n" +
                "    texel = vec3(dot(vec3(0.3, 0.6, 0.1), texel));\n" +
                "    texel = vec3(texture2D(inputImageTexture2, vec2(texel.r, .16666)).r);\n" +
                "    mediump vec4 fragColor = vec4(texel, 1.0);\n" +
                "    gl_FragColor = mix(sourceImageColor, fragColor, u_mix);\n" +
                "}");
    }

    @Override
    protected void initShaderHandles() {
        super.initShaderHandles();
        mMixHandle = GLES20.glGetUniformLocation(mProgramHandle, UNIFORM_MIX);
    }

    @Override
    protected void bindShaderValues() {
        super.bindShaderValues();
        GLES20.glUniform1f(mMixHandle, mMix);
    }

    @Override
    public void adjust(float progress) {
        mMix = progress;
    }
}

package cn.ezandroid.ezfilter.demo.render;

import android.opengl.GLES20;

import cn.ezandroid.ezfilter.core.FilterRender;

/**
 * WobbleRender
 *
 * @author like
 * @date 2017-09-21
 */
public class WobbleRender extends FilterRender {

    private String UNIFORM_OFFSET = "offset";
    private int mOffsetHandler;
    private float mOffset;

    private long mStartTime;

    public WobbleRender() {
        setFragmentShader("precision mediump float;\n" +
                "\n" +
                "uniform sampler2D inputImageTexture;\n" +
                "uniform float offset;\n" +
                "\n" +
                "varying vec2 textureCoordinate;\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "    vec2 texcoord = textureCoordinate;\n" +
                "    texcoord.x += sin(texcoord.y * 4.0 * 2.0 * 3.14159 + offset) / 100.0;\n" +
                "    gl_FragColor = texture2D(inputImageTexture, texcoord);\n" +
                "}");
        mStartTime = System.currentTimeMillis();
    }

    @Override
    protected void initShaderHandles() {
        super.initShaderHandles();
        mOffsetHandler = GLES20.glGetUniformLocation(mProgramHandle, UNIFORM_OFFSET);
    }

    @Override
    protected void bindShaderValues() {
        super.bindShaderValues();
        GLES20.glUniform1f(mOffsetHandler, mOffset);
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        long time = System.currentTimeMillis() - mStartTime;
        if (time > 20000) {
            mStartTime = System.currentTimeMillis();
        }

        mOffset = (time / 1000f * 2f * 3.14159f * 0.75f);
    }
}

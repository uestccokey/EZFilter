package cn.ezandroid.ezfilter.core;

import android.opengl.GLES20;
import android.text.TextUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.LinkedList;
import java.util.Queue;

import cn.ezandroid.ezfilter.core.util.L;
import cn.ezandroid.ezfilter.core.util.ShaderHelper;

/**
 * 滤镜核心类
 * <p>
 * 实现了OpenGL的渲染逻辑，是所有渲染器的父类
 *
 * @author like
 * @date 2017-09-15
 */
public class GLRender implements OnTextureAcceptableListener {

    public static final String ATTRIBUTE_POSITION = "position";
    public static final String ATTRIBUTE_TEXTURE_COORD = "inputTextureCoordinate";
    public static final String VARYING_TEXTURE_COORD = "textureCoordinate";
    public static final String UNIFORM_TEXTURE = "inputImageTexture";
    public static final String UNIFORM_TEXTURE_0 = UNIFORM_TEXTURE;

    public static final String DEFAULT_VERTEX_SHADER = "attribute vec4 " + ATTRIBUTE_POSITION + ";\n"
            + "attribute vec2 " + ATTRIBUTE_TEXTURE_COORD + ";\n"
            + "varying vec2 " + VARYING_TEXTURE_COORD + ";\n"
            + "void main() {\n"
            + "  " + VARYING_TEXTURE_COORD + " = " + ATTRIBUTE_TEXTURE_COORD + ";\n"
            + "   gl_Position = " + ATTRIBUTE_POSITION + ";\n"
            + "}\n";

    public static final String DEFAULT_FRAGMENT_SHADER = "precision mediump float;\n"
            + "uniform sampler2D " + UNIFORM_TEXTURE_0 + ";\n"
            + "varying vec2 " + VARYING_TEXTURE_COORD + ";\n"
            + "void main(){\n"
            + "   gl_FragColor = texture2D(" + UNIFORM_TEXTURE_0 + "," + VARYING_TEXTURE_COORD + ")" + ";\n"
            + "}\n";

    protected String mVertexShader = DEFAULT_VERTEX_SHADER;
    protected String mFragmentShader = DEFAULT_FRAGMENT_SHADER;

    protected int mCurrentRotation;

    protected FloatBuffer mWorldVertices;
    protected FloatBuffer[] mTextureVertices;

    protected int mProgramHandle;
    protected int mVertexShaderHandle;
    protected int mFragmentShaderHandle;
    protected int mTextureHandle;
    protected int mPositionHandle;
    protected int mTextureCoordHandle;

    protected int mTextureIn;

    protected int mWidth;
    protected int mHeight;

    private boolean mCustomSizeSet;
    private boolean mInitialized;
    protected boolean mSizeChanged;

    protected final Queue<Runnable> mRunOnDraw;
    protected final Queue<Runnable> mRunOnDrawEnd;

    protected int mFps;
    private long mLastTime;
    private int mFrameCount;

    public GLRender() {
        initWorldVertices();
        initTextureVertices();

        mRunOnDraw = new LinkedList<>();
        mRunOnDrawEnd = new LinkedList<>();
    }

    /**
     * 初始化世界坐标系顶点
     */
    protected void initWorldVertices() {
        // (-1, 1) -------> (1,1)
        //      ^
        //       \\
        //         (0,0)
        //           \\
        //             \\
        // (-1,-1) -------> (1,-1)
        float[] vertices = new float[]{-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f};
        mWorldVertices = ByteBuffer.allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mWorldVertices.put(vertices).position(0);
    }

    /**
     * 设置世界坐标系
     *
     * @param worldVertices
     */
    public void setWorldVertices(FloatBuffer worldVertices) {
        mWorldVertices = worldVertices;
    }

    /**
     * 初始化纹理坐标系顶点，默认为填充模式
     */
    protected void initTextureVertices() {
        mTextureVertices = new FloatBuffer[4];

        // (0,1) -------> (1,1)
        //     ^
        //      \\
        //        \\
        //          \\
        //            \\
        // (0,0) -------> (1,0)
        // 正向纹理坐标
        float[] texData0 = new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,
                1.0f, 1.0f,};
        mTextureVertices[0] = ByteBuffer.allocateDirect(texData0.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTextureVertices[0].put(texData0).position(0);

        // 顺时针旋转90°的纹理坐标
        float[] texData1 = new float[]{1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f,
                0.0f, 1.0f,};
        mTextureVertices[1] = ByteBuffer.allocateDirect(texData1.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTextureVertices[1].put(texData1).position(0);

        // 顺时针旋转180°的纹理坐标
        float[] texData2 = new float[]{1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f,
                0.0f, 0.0f,};
        mTextureVertices[2] = ByteBuffer.allocateDirect(texData2.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTextureVertices[2].put(texData2).position(0);

        // 顺时针旋转270°的纹理坐标
        float[] texData3 = new float[]{0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f,
                1.0f, 0.0f,};
        mTextureVertices[3] = ByteBuffer.allocateDirect(texData3.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTextureVertices[3].put(texData3).position(0);
    }

    /**
     * 设置纹理坐标系
     *
     * @param textureVertices
     */
    public void setTextureVertices(FloatBuffer[] textureVertices) {
        mTextureVertices = textureVertices;
    }

    /**
     * 获取渲染高度
     *
     * @return
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * 获取渲染宽度
     *
     * @return
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * 设置渲染宽度
     * <p>
     * 调用setRenderSize后，再调用该方法无效
     *
     * @param width
     */
    protected void setWidth(int width) {
        if (!mCustomSizeSet && this.mWidth != width) {
            this.mWidth = width;
            mSizeChanged = true;
        }
    }

    /**
     * 设置渲染高度
     * <p>
     * 调用setRenderSize后，再调用该方法无效
     *
     * @param height
     */
    protected void setHeight(int height) {
        if (!mCustomSizeSet && this.mHeight != height) {
            this.mHeight = height;
            mSizeChanged = true;
        }
    }

    /**
     * 获取顺时针旋转90度的次数
     *
     * @return
     */
    public int getRotate90Degrees() {
        return mCurrentRotation;
    }

    /**
     * 重置旋转角为0
     *
     * @return
     */
    public boolean resetRotate() {
        if (mCurrentRotation % 2 == 1) {
            mCurrentRotation = 0;
            return true;
        }
        mCurrentRotation = 0;
        return false;
    }

    /**
     * 设置顺时针旋转90度的次数
     *
     * @param numOfTimes
     */
    public void setRotate90Degrees(int numOfTimes) {
        while (numOfTimes < 0) {
            numOfTimes = 4 + numOfTimes;
        }
        mCurrentRotation += numOfTimes;
        mCurrentRotation = mCurrentRotation % 4;
    }

    /**
     * 设置渲染尺寸
     * <p>
     * 调用setRenderSize后，再调用setWidth或setHeight无效
     *
     * @param width
     * @param height
     */
    public void setRenderSize(int width, int height) {
        mCustomSizeSet = true;
        this.mWidth = width;
        this.mHeight = height;
        mSizeChanged = true;
    }

    /**
     * 交换宽高
     */
    public void swapWidthAndHeight() {
        int temp = mWidth;
        this.mWidth = mHeight;
        this.mHeight = temp;
        mSizeChanged = true;
    }

    /**
     * 初始化参数句柄
     */
    protected void initShaderHandles() {
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, ATTRIBUTE_POSITION);
        mTextureCoordHandle = GLES20.glGetAttribLocation(mProgramHandle, ATTRIBUTE_TEXTURE_COORD);

        mTextureHandle = GLES20.glGetUniformLocation(mProgramHandle, UNIFORM_TEXTURE_0);
    }

    /**
     * 绑定顶点
     */
    protected void bindShaderVertices() {
        mWorldVertices.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, 2, GLES20.GL_FLOAT, false,
                8, mWorldVertices);
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        mTextureVertices[mCurrentRotation].position(0);
        GLES20.glVertexAttribPointer(mTextureCoordHandle, 2, GLES20.GL_FLOAT, false,
                8, mTextureVertices[mCurrentRotation]);
        GLES20.glEnableVertexAttribArray(mTextureCoordHandle);
    }

    /**
     * 绑定纹理
     */
    protected void bindShaderTextures() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureIn);
        GLES20.glUniform1i(mTextureHandle, 0);
    }

    /**
     * 绑定顶点、纹理等
     */
    protected void bindShaderValues() {
        bindShaderVertices();
        bindShaderTextures();
    }

    /**
     * 重置渲染器
     */
    public void reInit() {
        mInitialized = false;
    }

    protected void logDraw() {
        Log.e("RenderDraw", toString() + " Fps:" + mFps);
    }

    @Override
    public String toString() {
        return super.toString() + "[" + mWidth + "x" + mHeight + "]";
    }

    /**
     * 必须在GL线程执行
     */
    public void onDrawFrame() {
        if (!mInitialized) {
            initGLContext();
            mInitialized = true;
        }
        if (mSizeChanged) {
            onRenderSizeChanged();
        }
        runAll(mRunOnDraw);
        drawFrame();
        runAll(mRunOnDrawEnd);

        mSizeChanged = false; // 在drawFrame执行后再重置状态，因为drawFrame中可能用到该状态

        if (L.LOG_RENDER_DRAW) {
            logDraw();
        }

        calculateFps();
    }

    /**
     * 计算FPS
     */
    private void calculateFps() {
        if (mLastTime == 0) {
            mLastTime = System.currentTimeMillis();
        }
        mFrameCount++;
        if (System.currentTimeMillis() - mLastTime >= 1000) {
            mLastTime = System.currentTimeMillis();
            mFps = mFrameCount;
            mFrameCount = 0;
        }
    }

    /**
     * 获取Fps
     *
     * @return
     */
    public int getFps() {
        return mFps;
    }

    protected void drawFrame() {
        if (mTextureIn == 0) {
            return;
        }
        if (mWidth != 0 && mHeight != 0) {
            GLES20.glViewport(0, 0, mWidth, mHeight);
        }

        GLES20.glUseProgram(mProgramHandle);

        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        bindShaderValues();

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    /**
     * 获取Attribute参数数组
     */
    protected String[] getShaderAttributes() {
        return new String[]{
                ATTRIBUTE_POSITION,
                ATTRIBUTE_TEXTURE_COORD
        };
    }

    /**
     * 初始化OpenGL上下文
     */
    protected void initGLContext() {
        final String vertexShader = getVertexShader();
        final String fragmentShader = getFragmentShader();

        if (!TextUtils.isEmpty(vertexShader) && !TextUtils.isEmpty(fragmentShader)) {
            // 初始化顶点着色器
            mVertexShaderHandle = ShaderHelper.compileShader(vertexShader, GLES20.GL_VERTEX_SHADER);
            // 初始化片元着色器
            mFragmentShaderHandle = ShaderHelper.compileShader(fragmentShader, GLES20.GL_FRAGMENT_SHADER);

            // 将顶点着色器和片元着色器链接到OpenGL渲染程序
            mProgramHandle = ShaderHelper.linkProgram(mVertexShaderHandle, mFragmentShaderHandle, getShaderAttributes());
        }

        initShaderHandles();
    }

    /**
     * 当渲染尺寸改变时调用
     */
    protected void onRenderSizeChanged() {
    }

    protected String getVertexShader() {
        return mVertexShader;
    }

    protected String getFragmentShader() {
        return mFragmentShader;
    }

    /**
     * 设置顶点着色器
     *
     * @param vertexShader
     */
    public void setVertexShader(String vertexShader) {
        mVertexShader = vertexShader;
    }

    /**
     * 设置片元着色器
     *
     * @param fragmentShader
     */
    public void setFragmentShader(String fragmentShader) {
        mFragmentShader = fragmentShader;
    }

    protected void logDestroy() {
        Log.e("RenderDestroy", toString() + " Thread:" + Thread.currentThread().getName());
    }

    /**
     * 必须在GL线程执行，释放纹理等OpenGL资源
     */
    public void destroy() {
        mInitialized = false;
        if (mProgramHandle != 0) {
            GLES20.glDeleteProgram(mProgramHandle);
            mProgramHandle = 0;
        }
        if (mVertexShaderHandle != 0) {
            GLES20.glDeleteShader(mVertexShaderHandle);
            mVertexShaderHandle = 0;
        }
        if (mFragmentShaderHandle != 0) {
            GLES20.glDeleteShader(mFragmentShaderHandle);
            mFragmentShaderHandle = 0;
        }

        if (L.LOG_RENDER_DESTROY) {
            logDestroy();
        }
    }

    protected void runAll(Queue<Runnable> queue) {
        synchronized (queue) {
            while (!queue.isEmpty()) {
                queue.poll().run();
            }
        }
    }

    public void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.add(runnable);
        }
    }

    public void runOnDrawEnd(final Runnable runnable) {
        synchronized (mRunOnDrawEnd) {
            mRunOnDrawEnd.add(runnable);
        }
    }

    @Override
    public void onTextureAcceptable(int texture, GLRender source) {
        mTextureIn = texture;
        setWidth(source.getWidth());
        setHeight(source.getHeight());
        onDrawFrame();
    }
}

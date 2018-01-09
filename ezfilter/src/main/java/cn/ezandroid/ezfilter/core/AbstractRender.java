package cn.ezandroid.ezfilter.core;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.LinkedList;
import java.util.Queue;

import cn.ezandroid.ezfilter.core.util.L;

/**
 * 滤镜核心类
 * <p>
 * 实现了OpenGL的渲染逻辑，是所有渲染器的父类
 *
 * @author like
 * @date 2017-09-15
 */
public abstract class AbstractRender {

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
            + "   gl_FragColor = texture2D(" + UNIFORM_TEXTURE_0 + "," + VARYING_TEXTURE_COORD + ")" +
            ";\n"
            + "}\n";

    protected String mVertexShader = DEFAULT_VERTEX_SHADER;
    protected String mFragmentShader = DEFAULT_FRAGMENT_SHADER;

    protected int mCurrentRotation;

    protected FloatBuffer mRenderVertices;
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

    private final Queue<Runnable> mRunOnDraw;
    private final Queue<Runnable> mRunOnDrawEnd;

    private int mFps;
    private long mLastTime;
    private int mFrameCount;

    public AbstractRender() {
        initRenderVertices(new float[]{-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f});
        initTextureVertices();

        mRunOnDraw = new LinkedList<>();
        mRunOnDrawEnd = new LinkedList<>();
    }

    protected void initRenderVertices(float[] vertices) {
        mRenderVertices = ByteBuffer.allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mRenderVertices.put(vertices).position(0);
    }

    protected void initTextureVertices() {
        mTextureVertices = new FloatBuffer[4];

        // (0,1) -------> (1,1)
        //     ^
        //      \\
        //        \\
        //          \\
        //            \\
        // (0,0) -------> (1,0)
        float[] texData0 = new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,
                1.0f, 1.0f,};
        mTextureVertices[0] = ByteBuffer.allocateDirect(texData0.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTextureVertices[0].put(texData0).position(0);

        float[] texData1 = new float[]{1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f,
                0.0f, 1.0f,};
        mTextureVertices[1] = ByteBuffer.allocateDirect(texData1.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTextureVertices[1].put(texData1).position(0);

        float[] texData2 = new float[]{1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f,
                0.0f, 0.0f,};
        mTextureVertices[2] = ByteBuffer.allocateDirect(texData2.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTextureVertices[2].put(texData2).position(0);

        float[] texData3 = new float[]{0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f,
                1.0f, 0.0f,};
        mTextureVertices[3] = ByteBuffer.allocateDirect(texData3.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTextureVertices[3].put(texData3).position(0);
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

    protected void setWidth(int width) {
        if (!mCustomSizeSet && this.mWidth != width) {
            this.mWidth = width;
            mSizeChanged = true;
        }
    }

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
//            swapWidthAndHeight();
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
//        if (numOfTimes % 2 == 1) {
//            swapWidthAndHeight();
//        }
    }

    /**
     * 设置渲染尺寸
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
     * 绑定Uniform参数
     */
    protected void bindShaderValues() {
        mRenderVertices.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, 2, GLES20.GL_FLOAT, false,
                8, mRenderVertices);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        mTextureVertices[mCurrentRotation].position(0);
        GLES20.glVertexAttribPointer(mTextureCoordHandle, 2, GLES20.GL_FLOAT, false,
                8, mTextureVertices[mCurrentRotation]);
        GLES20.glEnableVertexAttribArray(mTextureCoordHandle);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureIn);
        GLES20.glUniform1i(mTextureHandle, 0);
    }

    /***
     * 绑定Attributes参数
     */
    protected void bindShaderAttributes() {
        GLES20.glBindAttribLocation(mProgramHandle, 0, ATTRIBUTE_POSITION);
        GLES20.glBindAttribLocation(mProgramHandle, 1, ATTRIBUTE_TEXTURE_COORD);
    }

    /**
     * 初始化参数句柄
     */
    protected void initShaderHandles() {
        mTextureHandle = GLES20.glGetUniformLocation(mProgramHandle, UNIFORM_TEXTURE_0);
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, ATTRIBUTE_POSITION);
        mTextureCoordHandle = GLES20.glGetAttribLocation(mProgramHandle, ATTRIBUTE_TEXTURE_COORD);
    }

    /**
     * 重置渲染器
     */
    public void reInit() {
        mInitialized = false;
    }

    /**
     * 只应该在RenderPipeline的onDrawFrame中调用
     */
    protected void onDrawFrame() {
        if (L.LOG_RENDER_DRAW) {
            Log.e("AbstractRender", this + " onDrawFrame:" + mWidth + "x" + mHeight + " " + mCurrentRotation + " Fps:" + mFps);
        }
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

        mSizeChanged = false;

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

        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClearColor(0, 0, 0, 0);

        bindShaderValues();

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    /**
     * 初始化GL上下文
     */
    protected void initGLContext() {
        final String vertexShader = getVertexShader();
        final String fragmentShader = getFragmentShader();

        String errorInfo = "none";

        // 初始化顶点着色器
        mVertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        if (mVertexShaderHandle != 0) {
            GLES20.glShaderSource(mVertexShaderHandle, vertexShader);
            GLES20.glCompileShader(mVertexShaderHandle);
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(mVertexShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
            if (compileStatus[0] == 0) {
                errorInfo = GLES20.glGetShaderInfoLog(mVertexShaderHandle);
                GLES20.glDeleteShader(mVertexShaderHandle);
                mVertexShaderHandle = 0;
            }
        }
        if (mVertexShaderHandle == 0) {
            throw new RuntimeException(this + ": Could not create vertex shader. Reason: "
                    + errorInfo);
        }

        // 初始化片元着色器
        mFragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        if (mFragmentShaderHandle != 0) {
            GLES20.glShaderSource(mFragmentShaderHandle, fragmentShader);
            GLES20.glCompileShader(mFragmentShaderHandle);
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(mFragmentShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
            if (compileStatus[0] == 0) {
                errorInfo = GLES20.glGetShaderInfoLog(mFragmentShaderHandle);
                GLES20.glDeleteShader(mFragmentShaderHandle);
                mFragmentShaderHandle = 0;
            }
        }
        if (mFragmentShaderHandle == 0) {
            throw new RuntimeException(this + ": Could not create fragment shader. Reason: "
                    + errorInfo);
        }

        // 将顶点着色器和片元着色器链接到OpenGL渲染程序
        mProgramHandle = GLES20.glCreateProgram();
        if (mProgramHandle != 0) {
            GLES20.glAttachShader(mProgramHandle, mVertexShaderHandle);
            GLES20.glAttachShader(mProgramHandle, mFragmentShaderHandle);

            bindShaderAttributes();

            GLES20.glLinkProgram(mProgramHandle);
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(mProgramHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] == 0) {
                GLES20.glDeleteProgram(mProgramHandle);
                mProgramHandle = 0;
            }
        }
        if (mProgramHandle == 0) {
            throw new RuntimeException(this + ":Could not create program.");
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

    /**
     * 必须在GL线程执行，释放纹理等OpenGL资源
     */
    public void destroy() {
        if (L.LOG_RENDER_DESTROY) {
            Log.e("AbstractRender", this + " destroy " + Thread.currentThread().getName());
        }
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
    }

    private void runAll(Queue<Runnable> queue) {
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
}

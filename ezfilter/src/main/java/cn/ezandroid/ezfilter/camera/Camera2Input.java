package cn.ezandroid.ezfilter.camera;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;

import java.util.Arrays;

import javax.microedition.khronos.opengles.GL10;

import cn.ezandroid.ezfilter.core.FBORender;
import cn.ezandroid.ezfilter.environment.IGLEnvironment;

/**
 * 5.0以上支持CameraDevice输入
 *
 * @author like
 * @date 2017-09-19
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2Input extends FBORender implements SurfaceTexture.OnFrameAvailableListener {

    private static final String UNIFORM_CAM_MATRIX = "u_Matrix";

    private CameraDevice mCamera;
    private SurfaceTexture mSurfaceTexture;

    private int mMatrixHandle;
    private float[] mMatrix = new float[16];

    private IGLEnvironment mRender;

    private Size mPreviewSize;
    private Handler mPreviewHandler;
    private CaptureRequest.Builder mPreviewRequestBuilder;

    public Camera2Input(IGLEnvironment render, CameraDevice camera, Size previewSize) {
        super();
        this.mRender = render;
        this.mCamera = camera;
        this.mPreviewSize = previewSize;
    }

    @Override
    protected void drawFrame() {
        try {
            mSurfaceTexture.updateTexImage();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.drawFrame();
    }

    @Override
    protected String getVertexShader() {
        return "uniform mat4 " + UNIFORM_CAM_MATRIX + ";\n"
                + "attribute vec4 " + ATTRIBUTE_POSITION + ";\n"
                + "attribute vec2 " + ATTRIBUTE_TEXTURE_COORD + ";\n"
                + "varying vec2 " + VARYING_TEXTURE_COORD + ";\n"
                + "void main() {\n"
                + "   vec4 texPos = " + UNIFORM_CAM_MATRIX + " * vec4(" + ATTRIBUTE_TEXTURE_COORD + ", 1, 1);\n"
                + "   " + VARYING_TEXTURE_COORD + " = texPos.xy;\n"
                + "   gl_Position = " + ATTRIBUTE_POSITION + ";\n"
                + "}\n";
    }

    @Override
    protected String getFragmentShader() {
        return "#extension GL_OES_EGL_image_external : require\n"
                + "precision mediump float;\n"
                + "uniform samplerExternalOES " + UNIFORM_TEXTURE_0 + ";\n"
                + "varying vec2 " + VARYING_TEXTURE_COORD + ";\n"
                + "void main() {\n"
                + "   gl_FragColor = texture2D(" + UNIFORM_TEXTURE_0 + ", " + VARYING_TEXTURE_COORD + ");\n"
                + "}\n";
    }

    @Override
    protected void initShaderHandles() {
        super.initShaderHandles();
        mMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, UNIFORM_CAM_MATRIX);
    }

    @Override
    protected void initGLContext() {
        super.initGLContext();

        if (mTextureIn != 0) {
            int[] tex = new int[1];
            tex[0] = mTextureIn;
            GLES20.glDeleteTextures(1, tex, 0);
            mTextureIn = 0;
        }
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        mTextureIn = textures[0];

        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        mSurfaceTexture = new SurfaceTexture(mTextureIn);
        // 修复预览画面不清晰的问题
        mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        mSurfaceTexture.setOnFrameAvailableListener(this);

        try {
            HandlerThread handlerThread = new HandlerThread("Camera2");
            handlerThread.start();
            mPreviewHandler = new Handler(handlerThread.getLooper());

            Surface surface = new Surface(mSurfaceTexture);
            mPreviewRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            mCamera.createCaptureSession(Arrays.asList(surface), mSessionPreviewStateCallback, mPreviewHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }

        setRenderSize(mPreviewSize.getHeight(), mPreviewSize.getWidth());
    }

    private CameraCaptureSession.StateCallback mSessionPreviewStateCallback = new
            CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        // 自动对焦
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        // 自动闪光灯
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                        CaptureRequest previewRequest = mPreviewRequestBuilder.build();
                        session.setRepeatingRequest(previewRequest, null, mPreviewHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            };

    @Override
    public void onFrameAvailable(SurfaceTexture arg0) {
        mRender.requestRender();
    }

    @Override
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
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureIn);
        GLES20.glUniform1i(mTextureHandle, 0);

        mSurfaceTexture.getTransformMatrix(mMatrix);
        GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mMatrix, 0);
    }

    @Override
    public void destroy() {
        super.destroy();
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        if (mTextureIn != 0) {
            int[] tex = new int[1];
            tex[0] = mTextureIn;
            GLES20.glDeleteTextures(1, tex, 0);
            mTextureIn = 0;
        }
    }
}

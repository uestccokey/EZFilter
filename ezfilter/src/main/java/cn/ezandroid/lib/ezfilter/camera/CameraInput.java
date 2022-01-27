package cn.ezandroid.lib.ezfilter.camera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import javax.microedition.khronos.opengles.GL10;

import cn.ezandroid.lib.ezfilter.camera.util.CameraUtil;
import cn.ezandroid.lib.ezfilter.core.FBORender;
import cn.ezandroid.lib.ezfilter.core.environment.IGLEnvironment;

/**
 * 摄像头输入
 *
 * @author like
 * @date 2017-09-16
 */
public class CameraInput extends FBORender implements SurfaceTexture.OnFrameAvailableListener, ISupportTakePhoto {

    private static final String UNIFORM_CAM_MATRIX = "u_Matrix";

    private Camera mCamera;
    private SurfaceTexture mSurfaceTexture;

    private int mMatrixHandle;
    private float[] mMatrix = new float[16];

    private IGLEnvironment mRender;

    private Camera.Size mPreviewSize;

    public CameraInput(IGLEnvironment render, Camera camera, Camera.Size previewSize) {
        super();
        this.mRender = render;
        this.mCamera = camera;
        this.mPreviewSize = previewSize;

        setVertexShader("uniform mat4 " + UNIFORM_CAM_MATRIX + ";\n"
                + "attribute vec4 " + ATTRIBUTE_POSITION + ";\n"
                + "attribute vec2 " + ATTRIBUTE_TEXTURE_COORD + ";\n"
                + "varying vec2 " + VARYING_TEXTURE_COORD + ";\n"
                + "void main() {\n"
                + "   vec4 texPos = " + UNIFORM_CAM_MATRIX + " * vec4(" + ATTRIBUTE_TEXTURE_COORD + ", 1, 1);\n"
                + "   " + VARYING_TEXTURE_COORD + " = texPos.xy;\n"
                + "   gl_Position = " + ATTRIBUTE_POSITION + ";\n"
                + "}\n");
        setFragmentShader("#extension GL_OES_EGL_image_external : require\n"
                + "precision mediump float;\n"
                + "uniform samplerExternalOES " + UNIFORM_TEXTURE_0 + ";\n"
                + "varying vec2 " + VARYING_TEXTURE_COORD + ";\n"
                + "void main() {\n"
                + "   gl_FragColor = texture2D(" + UNIFORM_TEXTURE_0 + ", " + VARYING_TEXTURE_COORD + ");\n"
                + "}\n");
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
        mSurfaceTexture.setDefaultBufferSize(mPreviewSize.width, mPreviewSize.height);
        mSurfaceTexture.setOnFrameAvailableListener(this);

        try {
            mCamera.setPreviewTexture(mSurfaceTexture);
            mCamera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }

        setRenderSize(mPreviewSize.height, mPreviewSize.width);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture arg0) {
        mRender.requestRender();
    }

    @Override
    protected void bindShaderValues() {
        super.bindShaderVertices();

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

    @Override
    public void takePhoto(final boolean isFront, final int degree, final PhotoTakenCallback callback) {
        mCamera.takePicture(new Camera.ShutterCallback() {
            @Override
            public void onShutter() {
                // ShutterCallback传入不为空时，会有快门声
            }
        }, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(final byte[] data, Camera camera) {
                new Thread() {
                    public void run() {
                        // 1.读取原始图片旋转信息
                        int originalDegree = CameraUtil.getExifDegree(data);
                        // 2.加载原始图片
                        Bitmap bitmap0 = BitmapFactory.decodeByteArray(data, 0, data.length);
                        // 3.旋转及镜像原始图片
                        Matrix matrix = new Matrix();
                        if (isFront) {
                            matrix.postScale(-1, 1);
                        }
                        matrix.postRotate(degree - originalDegree);
                        Bitmap bitmap1 = Bitmap.createBitmap(bitmap0, 0, 0,
                                bitmap0.getWidth(), bitmap0.getHeight(), matrix, true);

                        // 由于bitmap1可能与bitmap0是同一个对象，这里进行判断
                        if (bitmap1 != bitmap0) {
                            bitmap0.recycle();
                        }

                        if (callback != null) {
                            callback.onPhotoTaken(bitmap1);
                        }
                    }
                }.start();
            }
        });
    }
}

package cn.ezandroid.lib.ezfilter.camera;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.microedition.khronos.opengles.GL10;

import cn.ezandroid.lib.ezfilter.camera.util.CameraUtil;
import cn.ezandroid.lib.ezfilter.core.FBORender;
import cn.ezandroid.lib.ezfilter.core.environment.IGLEnvironment;

/**
 * CameraDevice输入
 * <p>
 * API21及以上支持
 *
 * @author like
 * @date 2017-09-19
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2Input extends FBORender implements SurfaceTexture.OnFrameAvailableListener, ISupportTakePhoto {

    private static final String UNIFORM_CAM_MATRIX = "u_Matrix";

    private CameraDevice mCamera;
    private SurfaceTexture mSurfaceTexture;

    private int mMatrixHandle;
    private float[] mMatrix = new float[16];

    private IGLEnvironment mRender;

    private Size mPreviewSize;
    private Handler mPreviewHandler;
    private CaptureRequest.Builder mPreviewRequestBuilder;

    private CameraCaptureSession mCameraCaptureSession;
    private ImageReader mImageReader;

    private boolean mIsFront;
    private int mDegree;
    private PhotoTakenCallback mPhotoTakenCallback;

    public Camera2Input(IGLEnvironment render, CameraDevice camera, Size previewSize) {
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

        mImageReader = ImageReader.newInstance(previewSize.getHeight(), previewSize.getWidth(), ImageFormat.JPEG, 1);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(final ImageReader reader) {
                new Thread() {
                    public void run() {
                        // 1.读取原始图片旋转信息
                        Image image = reader.acquireNextImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] data = new byte[buffer.remaining()];
                        buffer.get(data);
                        image.close();
                        int originalDegree = CameraUtil.getExifDegree(data);
                        // 2.加载原始图片
                        final Bitmap bitmap0 = BitmapFactory.decodeByteArray(data, 0, data.length);
                        // 3.旋转及镜像原始图片
                        Matrix matrix = new Matrix();
                        if (mIsFront) {
                            matrix.postScale(-1, 1);
                        }
                        matrix.postRotate(mDegree - originalDegree);
                        final Bitmap bitmap1 = Bitmap.createBitmap(bitmap0, 0, 0,
                                bitmap0.getWidth(), bitmap0.getHeight(), matrix, true);

                        // 由于bitmap1可能与bitmap0是同一个对象，这里进行判断
                        if (bitmap1 != bitmap0) {
                            bitmap0.recycle();
                        }

                        if (mPhotoTakenCallback != null) {
                            mPhotoTakenCallback.onPhotoTaken(bitmap1);
                        }
                    }
                }.start();
            }
        }, mPreviewHandler);
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
            mCamera.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), mSessionPreviewStateCallback, mPreviewHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }

        setRenderSize(mPreviewSize.getHeight(), mPreviewSize.getWidth());
    }

    private CameraCaptureSession.StateCallback mSessionPreviewStateCallback = new
            CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(CameraCaptureSession session) {
                    mCameraCaptureSession = session;
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
    public void takePhoto(boolean isFront, int degree, PhotoTakenCallback callback) {
        mIsFront = isFront;
        mDegree = degree;
        mPhotoTakenCallback = callback;

        CaptureRequest.Builder captureRequestBuilder;
        try {
            captureRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            // 将ImageReader的surface作为CaptureRequest.Builder的目标
            captureRequestBuilder.addTarget(mImageReader.getSurface());
            // 自动对焦
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 自动曝光
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            // 设置照片方向
            if (mIsFront) {
                captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 270);
            } else {
                captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 90);
            }
            // 拍照
            CaptureRequest captureRequest = captureRequestBuilder.build();
            mCameraCaptureSession.capture(captureRequest, null, mPreviewHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}

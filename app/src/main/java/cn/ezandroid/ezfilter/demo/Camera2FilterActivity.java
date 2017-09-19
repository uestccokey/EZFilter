package cn.ezandroid.ezfilter.demo;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cn.ezandroid.ezfilter.EZFilter;
import cn.ezandroid.ezfilter.core.RenderPipeline;
import cn.ezandroid.ezfilter.demo.render.BWRender;
import cn.ezandroid.ezfilter.view.SurfaceRenderView;

/**
 * Camera2FilterActivity
 *
 * @author like
 * @date 2017-09-19
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2FilterActivity extends BaseActivity {

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    ///为了使照片竖直显示
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private SurfaceRenderView mRenderView;

    private CameraManager mCameraManager;

    private Handler mMainHandler;
    private String mCameraID; // 摄像头Id 0 为后  1 为前

    private CameraDevice mCameraDevice;

    private Size mPreviewSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2_filter);
        mRenderView = findViewById(R.id.render_view);

        openCamera();
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        mMainHandler = new Handler(getMainLooper());
        mCameraID = "" + CameraCharacteristics.LENS_FACING_FRONT;
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        mPreviewSize = new Size(1080, 1920);
        try {
            mCameraManager.openCamera(mCameraID, mStateCallback, mMainHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        List<Size> bigEnough = new ArrayList<Size>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            return choices[0];
        }
    }

    class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    /**
     * 摄像头创建监听
     */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) { // 打开摄像头
            mCameraDevice = camera;

            final RenderPipeline renderPipeline = new EZFilter.Camera2Builder()
                    .setCamera2(mCameraDevice, mPreviewSize)
                    .addFilter(new BWRender(Camera2FilterActivity.this))
                    .into(mRenderView);
        }

        @Override
        public void onDisconnected(CameraDevice camera) { // 关闭摄像头
            if (null != mCameraDevice) {
                mCameraDevice.close();
            }
        }

        @Override
        public void onError(CameraDevice camera, int error) { // 发生错误
            Toast.makeText(Camera2FilterActivity.this, "摄像头开启失败", Toast.LENGTH_SHORT).show();
        }
    };

    private void releaseCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
        }
        mCameraDevice = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
    }
}
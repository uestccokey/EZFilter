package cn.ezandroid.ezfilter.demo;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cn.ezandroid.ezfilter.EZFilter;
import cn.ezandroid.ezfilter.core.FilterRender;
import cn.ezandroid.ezfilter.core.RenderPipeline;
import cn.ezandroid.ezfilter.core.environment.FitViewHelper;
import cn.ezandroid.ezfilter.core.environment.SurfaceFitView;
import cn.ezandroid.ezfilter.core.output.BitmapOutput;
import cn.ezandroid.ezfilter.demo.render.BWRender;
import cn.ezandroid.ezfilter.demo.render.WobbleRender;

/**
 * Camera2FilterActivity
 *
 * @author like
 * @date 2017-09-19
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2FilterActivity extends BaseActivity {

    // Orientation hysteresis amount used in rounding, in degrees
    private static final int ORIENTATION_HYSTERESIS = 5;

    private static final int MIN_PREVIEW_WIDTH = 1280;
    private static final int MIN_PREVIEW_HEIGHT = 720;

    private static final int MAX_PREVIEW_WIDTH = 1920;
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private SurfaceFitView mRenderView;
    private ImageView mPreviewImage;
    private Button mRecordButton;

    private CameraManager mCameraManager;

    private Handler mMainHandler = new Handler(Looper.getMainLooper());
    private int mCurrentCameraId = 1; // 前置摄像头一般为1

    private CameraDevice mCameraDevice;

    private Size mPreviewSize;

    private RenderPipeline mRenderPipeline;

    private FilterRender mBWRender;
    private FilterRender mWobbleRender;

    private FilterRender mCurrentRender;

    private MyOrientationEventListener mOrientationEventListener;

    private int mOrientation;

    private class MyOrientationEventListener extends OrientationEventListener {

        MyOrientationEventListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            if (orientation == ORIENTATION_UNKNOWN) return;
            mOrientation = roundOrientation(orientation, mOrientation);
        }
    }

    private int roundOrientation(int orientation, int orientationHistory) {
        boolean changeOrientation;
        if (orientationHistory == OrientationEventListener.ORIENTATION_UNKNOWN) {
            changeOrientation = true;
        } else {
            int dist = Math.abs(orientation - orientationHistory);
            dist = Math.min(dist, 360 - dist);
            changeOrientation = (dist >= 45 + ORIENTATION_HYSTERESIS);
        }
        if (changeOrientation) {
            return ((orientation + 45) / 90 * 90) % 360;
        }
        return orientationHistory;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2_filter);
        mRenderView = findViewById(R.id.render_view);
        mPreviewImage = $(R.id.preview_image);
        mRecordButton = $(R.id.record);

        mRenderView.setScaleType(FitViewHelper.ScaleType.CENTER_CROP);

        mBWRender = new BWRender(this);
        mWobbleRender = new WobbleRender();

        mCurrentRender = mBWRender;

        mOrientationEventListener = new MyOrientationEventListener(this);

        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        $(R.id.switch_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchCamera();
            }
        });

//        $(R.id.take_photo).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                boolean isFront = false;
//                try {
//                    CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(String.valueOf(mCurrentCameraId));
//                    Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
//                    isFront = facing != null && CameraCharacteristics.LENS_FACING_FRONT == facing;
//                } catch (CameraAccessException e) {
//                    e.printStackTrace();
//                }
//                mRenderPipeline.takePhoto(isFront, mOrientation,
//                        new PhotoTakenCallback() {
//                            @Override
//                            public void onPhotoTaken(Bitmap bitmap) {
//                                saveBitmap(bitmap);
//
//                                final Bitmap bitmap2 = EZFilter.input(bitmap)
//                                        .addFilter(new BWRender(Camera2FilterActivity.this))
//                                        .output(); // 添加滤镜
//
//                                saveBitmap(bitmap2);  // 保存滤镜后的图
//
//                                releaseCamera();
//                                openCamera(mCurrentCameraId);
//                            }
//                        });
//            }
//        });

        $(R.id.capture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRenderPipeline.output(new BitmapOutput.BitmapOutputCallback() {
                    @Override
                    public void bitmapOutput(final Bitmap bitmap) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mPreviewImage.setImageBitmap(bitmap);
                            }
                        });
                    }
                }, true);
            }
        });

        $(R.id.change_filter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentRender == mBWRender) {
                    mCurrentRender = mWobbleRender;
                    mRenderPipeline.removeFilterRender(mBWRender);
                    mRenderPipeline.addFilterRender(mWobbleRender);
                } else {
                    mCurrentRender = mBWRender;
                    mRenderPipeline.removeFilterRender(mWobbleRender);
                    mRenderPipeline.addFilterRender(mBWRender);
                }
            }
        });

//        mRecordButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (mRenderPipeline.isRecording()) {
//                    stopRecording();
//                } else {
//                    startRecording();
//                }
//            }
//        });
    }

    private void saveBitmap(Bitmap bitmap) {
        ByteArrayOutputStream output = null;
        FileOutputStream fos = null;
        try {
            File file = new File("/sdcard/" + System.currentTimeMillis() + ".jpg");
            if (!file.exists()) {
                file.createNewFile();
            }
            output = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);

            fos = new FileOutputStream(file);
            fos.write(output.toByteArray());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

//    private void startRecording() {
//        mRecordButton.setText("停止");
//        mRenderPipeline.startRecording();
//    }
//
//    private void stopRecording() {
//        mRecordButton.setText("录制");
//        mRenderPipeline.stopRecording();
//    }

    @SuppressLint("MissingPermission")
    private void openCamera(int id) {
        try {
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics("" + id);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizes = map.getOutputSizes(android.graphics.ImageFormat.JPEG);
            Size largest = Collections.max(Arrays.asList(sizes), new CompareSizesByArea());

            // Find out if we need to swap dimension to get the preview size relative to sensor
            // coordinate.
            int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
            // noinspection ConstantConditions
            int mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            boolean swappedDimensions = false;
            switch (displayRotation) {
                case Surface.ROTATION_0:
                case Surface.ROTATION_180:
                    if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                        swappedDimensions = true;
                    }
                    break;
                case Surface.ROTATION_90:
                case Surface.ROTATION_270:
                    if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                        swappedDimensions = true;
                    }
                    break;
            }

            Point displaySize = new Point();
            getWindowManager().getDefaultDisplay().getSize(displaySize);
            int rotatedPreviewWidth = MIN_PREVIEW_WIDTH;
            int rotatedPreviewHeight = MIN_PREVIEW_HEIGHT;
            int maxPreviewWidth = displaySize.x;
            int maxPreviewHeight = displaySize.y;
            if (swappedDimensions) {
                rotatedPreviewWidth = MIN_PREVIEW_HEIGHT;
                rotatedPreviewHeight = MIN_PREVIEW_WIDTH;
                maxPreviewWidth = displaySize.y;
                maxPreviewHeight = displaySize.x;
            }

            if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                maxPreviewWidth = MAX_PREVIEW_WIDTH;
            }
            if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                maxPreviewHeight = MAX_PREVIEW_HEIGHT;
            }

            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                    maxPreviewHeight, largest);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        try {
            mCameraManager.openCamera("" + id, mStateCallback, mMainHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void switchCamera() {
        try {
            mCurrentCameraId = (mCurrentCameraId + 1) % mCameraManager.getCameraIdList().length;
            releaseCamera();
            openCamera(mCurrentCameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight,
                                   int maxWidth, int maxHeight, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
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
            mRenderPipeline = EZFilter.input(mCameraDevice, mPreviewSize)
                    .addFilter(mCurrentRender)
                    .enableRecord("/sdcard/recordCamera2.mp4", true, true) // 支持录制为视频
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
        mOrientationEventListener.enable();
        openCamera(mCurrentCameraId);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mOrientationEventListener.disable();
        releaseCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
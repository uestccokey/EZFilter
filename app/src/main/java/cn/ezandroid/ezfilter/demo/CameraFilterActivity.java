package cn.ezandroid.ezfilter.demo;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import cn.ezandroid.ezfilter.EZFilter;
import cn.ezandroid.ezfilter.core.PhotoTakenCallback;
import cn.ezandroid.ezfilter.core.RenderPipeline;
import cn.ezandroid.ezfilter.core.output.BitmapOutput;
import cn.ezandroid.ezfilter.demo.render.BWRender;
import cn.ezandroid.ezfilter.demo.render.WobbleRender;
import cn.ezandroid.ezfilter.environment.FitViewHelper;
import cn.ezandroid.ezfilter.environment.TextureFitView;

/**
 * CameraFilterActivity
 *
 * @author like
 * @date 2017-09-16
 */
public class CameraFilterActivity extends BaseActivity {

    // Orientation hysteresis amount used in rounding, in degrees
    private static final int ORIENTATION_HYSTERESIS = 5;

    private TextureFitView mRenderView;
    private ImageView mPreviewImage;
    private Button mRecordButton;

    private Camera mCamera;

    private int mCurrentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;

    private RenderPipeline mRenderPipeline;

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
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_filter);
        mRenderView = $(R.id.render_view);
        mPreviewImage = $(R.id.preview_image);
        mRecordButton = $(R.id.record);

        mRenderView.setScaleType(FitViewHelper.ScaleType.CENTER_CROP);

        mOrientationEventListener = new MyOrientationEventListener(this);

        $(R.id.switch_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchCamera();
            }
        });

        $(R.id.take_photo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRenderPipeline.takePhoto(mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT, mOrientation,
                        new PhotoTakenCallback() {
                            @Override
                            public void onPhotoTaken(Bitmap bitmap) {
                                saveBitmap(bitmap);

                                final Bitmap bitmap2 = EZFilter.input(bitmap)
                                        .addFilter(new BWRender(CameraFilterActivity.this), 0.5f)
                                        .addFilter(new WobbleRender())
                                        .output(); // 添加滤镜

                                saveBitmap(bitmap2);  // 保存滤镜后的图

                                releaseCamera();
                                openCamera(mCurrentCameraId);
                            }
                        });
            }
        });

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

        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRenderPipeline.isRecording()) {
                    stopRecording();
                } else {
                    startRecording();
                }
            }
        });
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

    private void startRecording() {
        mRecordButton.setText("停止");
        mRenderPipeline.startRecording();
    }

    private void stopRecording() {
        mRecordButton.setText("录制");
        mRenderPipeline.stopRecording();
    }

    private void setCameraParameters() {
        Camera.Parameters parameters = mCamera.getParameters();
        if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(mCurrentCameraId, cameraInfo);

            parameters.set("orientation", "portrait");
            int orientation;
            if (mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                parameters.set("rotation", 270);
                orientation = 360 - cameraInfo.orientation;
            } else {
                parameters.set("rotation", 90);
                orientation = cameraInfo.orientation;
            }
            mCamera.setDisplayOrientation(orientation);
        } else {
            parameters.set("orientation", "landscape");
            mCamera.setDisplayOrientation(0);
        }

        // 输出分辨率设置为1920*1080，质量100%
        parameters.setPictureSize(1920, 1080);
        parameters.setJpegQuality(100);

        // 设置自动闪光灯
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);

        // 设置自动对焦
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

        mCamera.setParameters(parameters);
    }

    private void switchCamera() {
        mCurrentCameraId = (mCurrentCameraId + 1) % Camera.getNumberOfCameras();
        releaseCamera();
        openCamera(mCurrentCameraId);
    }

    private void openCamera(int id) {
        mCamera = Camera.open(id);
        setCameraParameters();

        mRenderPipeline = EZFilter.input(mCamera)
                .addFilter(new BWRender(this), 0.5f)
                .addFilter(new WobbleRender())
                .enableRecord("/sdcard/recordCamera.mp4", true, true) // 支持录制为视频
                .into(mRenderView);
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
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

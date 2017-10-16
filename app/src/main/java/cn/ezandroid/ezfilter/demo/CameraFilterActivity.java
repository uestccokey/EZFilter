package cn.ezandroid.ezfilter.demo;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import cn.ezandroid.ezfilter.EZFilter;
import cn.ezandroid.ezfilter.core.RenderPipeline;
import cn.ezandroid.ezfilter.core.output.BitmapOutput;
import cn.ezandroid.ezfilter.demo.render.BWRender;
import cn.ezandroid.ezfilter.environment.FitViewHelper;
import cn.ezandroid.ezfilter.environment.TextureFitView;

/**
 * CameraFilterActivity
 *
 * @author like
 * @date 2017-09-16
 */
public class CameraFilterActivity extends BaseActivity {

    private TextureFitView mRenderView;
    private ImageView mPreviewImage;
    private Button mRecordButton;

    private Camera mCamera;

    private int mCurrentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;

    private RenderPipeline mRenderPipeline;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_filter);
        mRenderView = $(R.id.render_view);
        mPreviewImage = $(R.id.preview_image);
        mRecordButton = $(R.id.record);

        mRenderView.setScaleType(FitViewHelper.ScaleType.CENTER_CROP);

        openCamera(mCurrentCameraId);

        $(R.id.switch_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchCamera();
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
            parameters.set("rotation", 90);
            // Front camera will display mirrored on the preview
            int orientation = 0;
            if (mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT)
                orientation = 360 - cameraInfo.orientation;
            else {
                orientation = cameraInfo.orientation;
            }
            mCamera.setDisplayOrientation(orientation);
        } else {
            parameters.set("orientation", "landscape");
            mCamera.setDisplayOrientation(0);
        }
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

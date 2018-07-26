package cn.ezandroid.ezfilter.demo;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

import cn.ezandroid.ezfilter.EZFilter;
import cn.ezandroid.ezfilter.core.GLRender;
import cn.ezandroid.ezfilter.core.RenderPipeline;
import cn.ezandroid.ezfilter.core.environment.SurfaceFitView;
import cn.ezandroid.ezfilter.demo.render.HorizontalMultiInput;
import cn.ezandroid.ezfilter.media.record.RecordableRender;
import cn.ezandroid.ezfilter.video.player.IMediaPlayer;

/**
 * MultiInputActivity
 *
 * @author like
 * @date 2018-07-13
 */
public class MultiInputActivity extends BaseActivity {

    // Orientation hysteresis amount used in rounding, in degrees
    private static final int ORIENTATION_HYSTERESIS = 5;

    private SurfaceFitView mRenderView;
    private Button mRecordButton;

    private RenderPipeline mRenderPipeline;

    private RecordableRender mSupportRecord;

    private Camera mCamera;

    private int mCurrentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;

    private MyOrientationEventListener mOrientationEventListener;

    private int mOrientation;

    private HorizontalMultiInput mTwoInput;
    private EZFilter.Builder mRightBuilder;

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
        setContentView(R.layout.activity_multi_input);

        mRenderView = $(R.id.render_view);
        mRecordButton = $(R.id.record);

        mOrientationEventListener = new MyOrientationEventListener(this);

        $(R.id.switch_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchCamera();
            }
        });

        mRightBuilder = EZFilter.input(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.test2))
                .setLoop(true)
                .setPreparedListener(new IMediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(IMediaPlayer var1) {
                        Log.e("MultiInputActivity", "onPrepared");
                        mTwoInput.updateRightWorldVertices();
                    }
                })
                .setCompletionListener(new IMediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(IMediaPlayer var1) {
                        Log.e("MultiInputActivity", "onCompletion");
                    }
                });

        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSupportRecord != null) {
                    if (mSupportRecord.isRecording()) {
                        stopRecording();
                    } else {
                        startRecording();
                    }
                }
            }
        });
    }

    private void startRecording() {
        mRecordButton.setText("停止");
        if (mSupportRecord != null) {
            mSupportRecord.startRecording();
        }
    }

    private void stopRecording() {
        mRecordButton.setText("录制");
        if (mSupportRecord != null) {
            mSupportRecord.stopRecording();
        }
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

            mRenderView.setRotate90Degrees(0);
        } else {
            parameters.set("orientation", "landscape");
            mCamera.setDisplayOrientation(0);

            mRenderView.setRotate90Degrees(1);
        }

        // 预览分辨率设置为1280*720
        parameters.setPreviewSize(1280, 720);

        // 输出分辨率设置为1280*720，质量100%
        parameters.setPictureSize(1280, 720);
        parameters.setJpegQuality(100);

        // 设置自动闪光灯
        List<String> flashModes = parameters.getSupportedFlashModes();
        if (flashModes != null && flashModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FLASH_MODE_AUTO);
        }

        // 设置自动对焦
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

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

        EZFilter.Builder leftBuilder = EZFilter.input(mCamera, mCamera.getParameters().getPreviewSize());
        if (mTwoInput == null) {
            List<EZFilter.Builder> builders = new ArrayList<>();
            builders.add(leftBuilder);
            builders.add(mRightBuilder);

            mTwoInput = new HorizontalMultiInput();
            mRenderPipeline = EZFilter.input(builders, mTwoInput)
                    .enableRecord("/sdcard/recordMultiple.mp4", true, false)
                    .into(mRenderView, false);

            for (GLRender render : mRenderPipeline.getEndPointRenders()) {
                if (render instanceof RecordableRender) {
                    mSupportRecord = (RecordableRender) render;
                }
            }
        } else {
            mTwoInput.getRenderPipelines().get(0).setStartPointRender(leftBuilder.getStartPointRender(mRenderView));
        }
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
}

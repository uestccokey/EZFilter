package cn.ezandroid.ezfilter.demo;

import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;

import cn.ezandroid.ezfilter.EZFilter;
import cn.ezandroid.ezfilter.demo.render.BWRender;
import cn.ezandroid.ezfilter.environment.RenderViewHelper;
import cn.ezandroid.ezfilter.environment.TextureRenderView;

/**
 * CameraFilterActivity
 *
 * @author like
 * @date 2017-09-16
 */
public class CameraFilterActivity extends BaseActivity {

    private TextureRenderView mRenderView;
    private ImageView mPreviewImage;

    private Camera mCamera;

    private int mCurrentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_filter);
        mRenderView = $(R.id.render_view);
        mPreviewImage = $(R.id.preview_image);

        mCamera = Camera.open(mCurrentCameraId);
        setCameraParameters();

        EZFilter.setCamera(mCamera)
                .setScaleType(RenderViewHelper.ScaleType.CENTER_CROP)
                .addFilter(new BWRender(this))
                .into(mRenderView);

        $(R.id.switch_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchCamera();
            }
        });
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

        EZFilter.setCamera(mCamera)
                .setScaleType(RenderViewHelper.ScaleType.CENTER_CROP)
                .addFilter(new BWRender(this))
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

package cn.ezandroid.ezfilter.camera;

import android.annotation.TargetApi;
import android.hardware.camera2.CameraDevice;
import android.os.Build;
import android.util.Size;

import cn.ezandroid.ezfilter.EZFilter;
import cn.ezandroid.ezfilter.core.FBORender;
import cn.ezandroid.ezfilter.core.FilterRender;
import cn.ezandroid.ezfilter.core.environment.IFitView;
import cn.ezandroid.ezfilter.extra.IAdjustable;

/**
 * 摄像头处理构造器
 *
 * @author like
 * @date 2017-09-15
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2Builder extends EZFilter.Builder {

    private CameraDevice mCameraDevice;
    private Size mPreviewSize;

    private Camera2Input mCamera2Input;

    public Camera2Builder(CameraDevice camera2, Size size) {
        mCameraDevice = camera2;
        mPreviewSize = size;
    }

    @Override
    public FBORender getStartPointRender(IFitView view) {
        if (mCamera2Input == null) {
            mCamera2Input = new Camera2Input(view, mCameraDevice, mPreviewSize);
        }
        return mCamera2Input;
    }

    @Override
    public float getAspectRatio(IFitView view) {
        return mPreviewSize.getHeight() * 1.0f / mPreviewSize.getWidth();
    }

    @Override
    public Camera2Builder addFilter(FilterRender filterRender) {
        return (Camera2Builder) super.addFilter(filterRender);
    }

    @Override
    public <T extends FilterRender & IAdjustable> Camera2Builder addFilter(T filterRender, float progress) {
        return (Camera2Builder) super.addFilter(filterRender, progress);
    }

    @Override
    public Camera2Builder enableRecord(String outputPath, boolean recordVideo, boolean recordAudio) {
        return (Camera2Builder) super.enableRecord(outputPath, recordVideo, recordAudio);
    }
}

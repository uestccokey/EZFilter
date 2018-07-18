package cn.ezandroid.ezfilter.camera;

import android.hardware.Camera;

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
public class CameraBuilder extends EZFilter.Builder {

    private Camera mCamera;
    private Camera.Size mPreviewSize;

    private CameraInput mCameraInput;

    public CameraBuilder(Camera camera, Camera.Size size) {
        mCamera = camera;
        mPreviewSize = size;
    }

    @Override
    public FBORender getStartPointRender(IFitView view) {
        if (mCameraInput == null) {
            mCameraInput = new CameraInput(view, mCamera, mPreviewSize);
        }
        return mCameraInput;
    }

    @Override
    public float getAspectRatio(IFitView view) {
        return mPreviewSize.height * 1.0f / mPreviewSize.width;
    }

    @Override
    public CameraBuilder addFilter(FilterRender filterRender) {
        return (CameraBuilder) super.addFilter(filterRender);
    }

    @Override
    public <T extends FilterRender & IAdjustable> CameraBuilder addFilter(T filterRender, float progress) {
        return (CameraBuilder) super.addFilter(filterRender, progress);
    }

    @Override
    public CameraBuilder enableRecord(String outputPath, boolean recordVideo, boolean recordAudio) {
        return (CameraBuilder) super.enableRecord(outputPath, recordVideo, recordAudio);
    }
}

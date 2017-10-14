package cn.ezandroid.ezfilter.camera;

import android.hardware.Camera;

import cn.ezandroid.ezfilter.EZFilter;
import cn.ezandroid.ezfilter.core.FBORender;
import cn.ezandroid.ezfilter.core.FilterRender;
import cn.ezandroid.ezfilter.environment.IFitView;
import cn.ezandroid.ezfilter.extra.IAdjustable;

/**
 * 摄像头处理构造器
 */
public class CameraBuilder extends EZFilter.Builder {

    private Camera mCamera;

    public CameraBuilder(Camera camera) {
        mCamera = camera;
    }

    @Override
    protected FBORender getStartPointRender(IFitView view) {
        return new CameraInput(view, mCamera);
    }

    @Override
    protected float getAspectRatio(IFitView view) {
        Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
        return previewSize.height * 1.0f / previewSize.width;
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

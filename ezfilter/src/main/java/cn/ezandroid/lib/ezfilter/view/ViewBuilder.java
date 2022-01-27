package cn.ezandroid.lib.ezfilter.view;

import cn.ezandroid.lib.ezfilter.EZFilter;
import cn.ezandroid.lib.ezfilter.core.FBORender;
import cn.ezandroid.lib.ezfilter.core.FilterRender;
import cn.ezandroid.lib.ezfilter.core.RenderPipeline;
import cn.ezandroid.lib.ezfilter.core.environment.IFitView;
import cn.ezandroid.lib.ezfilter.extra.IAdjustable;
import cn.ezandroid.lib.ezfilter.view.glview.IGLView;

/**
 * 视图处理构造器
 *
 * @author like
 * @date 2017-09-15
 */
public class ViewBuilder extends EZFilter.Builder {

    private IGLView mGLView;

    private ViewInput mViewInput;

    public ViewBuilder(IGLView view) {
        mGLView = view;
    }

    @Override
    public FBORender getStartPointRender(IFitView view) {
        if (mViewInput == null) {
            mViewInput = new ViewInput(mGLView);
        }
        return mViewInput;
    }

    @Override
    public float getAspectRatio(IFitView view) {
        if (mGLView.getHeight() != 0) {
            return mGLView.getWidth() * 1.0f / mGLView.getHeight();
        }
        return 1;
    }

    @Override
    public ViewBuilder addFilter(FilterRender filterRender) {
        return (ViewBuilder) super.addFilter(filterRender);
    }

    @Override
    public <T extends FilterRender & IAdjustable> ViewBuilder addFilter(T filterRender, float progress) {
        return (ViewBuilder) super.addFilter(filterRender, progress);
    }

    @Override
    public ViewBuilder enableRecord(String outputPath, boolean recordVideo, boolean recordAudio) {
        return (ViewBuilder) super.enableRecord(outputPath, recordVideo, recordAudio);
    }

    public RenderPipeline into(IFitView view) {
        mGLView.setGLEnvironment(view);
        return super.into(view);
    }
}

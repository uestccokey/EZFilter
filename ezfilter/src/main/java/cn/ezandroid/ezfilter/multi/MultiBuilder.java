package cn.ezandroid.ezfilter.multi;

import java.util.List;

import cn.ezandroid.ezfilter.EZFilter;
import cn.ezandroid.ezfilter.core.FBORender;
import cn.ezandroid.ezfilter.core.FilterRender;
import cn.ezandroid.ezfilter.core.environment.IFitView;
import cn.ezandroid.ezfilter.extra.IAdjustable;

/**
 * 多输入源构造器
 *
 * @author like
 * @date 2018-07-13
 */
public class MultiBuilder extends EZFilter.Builder {

    private List<EZFilter.Builder> mBuilders;
    private MultiInput mMultiInput;

    public MultiBuilder(List<EZFilter.Builder> builders, MultiInput multiInput) {
        mBuilders = builders;
        mMultiInput = multiInput;
    }

    @Override
    public FBORender getStartPointRender(IFitView view) {
        mMultiInput.clearRegisteredFilterLocations();
        for (EZFilter.Builder builder : mBuilders) {
            FBORender render = builder.getStartPointRender(view);
            mMultiInput.registerFilterLocation(render);
        }
        return mMultiInput;
    }

    @Override
    protected float getAspectRatio(IFitView view) {
        return mMultiInput.getWidth() * 1.0f / mMultiInput.getHeight();
    }

    @Override
    public MultiBuilder addFilter(FilterRender filterRender) {
        return (MultiBuilder) super.addFilter(filterRender);
    }

    @Override
    public <T extends FilterRender & IAdjustable> MultiBuilder addFilter(T filterRender, float progress) {
        return (MultiBuilder) super.addFilter(filterRender, progress);
    }

    @Override
    public MultiBuilder enableRecord(String outputPath, boolean recordVideo, boolean recordAudio) {
        return (MultiBuilder) super.enableRecord(outputPath, recordVideo, recordAudio);
    }
}

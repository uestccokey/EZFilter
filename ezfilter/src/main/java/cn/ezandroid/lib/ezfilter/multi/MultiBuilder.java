package cn.ezandroid.lib.ezfilter.multi;

import java.util.List;

import cn.ezandroid.lib.ezfilter.EZFilter;
import cn.ezandroid.lib.ezfilter.core.FBORender;
import cn.ezandroid.lib.ezfilter.core.FilterRender;
import cn.ezandroid.lib.ezfilter.core.environment.IFitView;
import cn.ezandroid.lib.ezfilter.extra.IAdjustable;

/**
 * 多输入源构造器
 * <p>
 * 支持将多个输入源，比如两个视频、一个视频一个图片等，组合到一个界面中进行显示，支持对各输入源分别添加滤镜
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
        mMultiInput.clearRegisteredFilters();
        for (EZFilter.Builder builder : mBuilders) {
            FBORender render = builder.getStartPointRender(view);
            mMultiInput.registerFilter(render);
        }
        return mMultiInput;
    }

    @Override
    public float getAspectRatio(IFitView view) {
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

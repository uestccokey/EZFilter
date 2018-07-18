package cn.ezandroid.ezfilter.split;

import cn.ezandroid.ezfilter.EZFilter;
import cn.ezandroid.ezfilter.core.FBORender;
import cn.ezandroid.ezfilter.core.FilterRender;
import cn.ezandroid.ezfilter.core.environment.IFitView;
import cn.ezandroid.ezfilter.extra.IAdjustable;

/**
 * 拆分输入源构造器
 * <p>
 * 支持将单个输入源画面进行裁剪后再进行组合显示，支持对拆分后的子画面分别添加滤镜
 *
 * @author like
 * @date 2018-07-18
 */
public class SplitBuilder extends EZFilter.Builder {

    private EZFilter.Builder mOriginalBuilder;
    private SplitInput mSplitInput;

    public SplitBuilder(EZFilter.Builder builder, SplitInput splitInput) {
        mOriginalBuilder = builder;
        mSplitInput = splitInput;
    }

    @Override
    public FBORender getStartPointRender(IFitView view) {
        FBORender startRender = mOriginalBuilder.getStartPointRender(view);
        mSplitInput.setRootRender(startRender);
        return mSplitInput;
    }

    @Override
    public float getAspectRatio(IFitView view) {
        return mOriginalBuilder.getAspectRatio(view);
    }

    @Override
    public SplitBuilder addFilter(FilterRender filterRender) {
        return (SplitBuilder) super.addFilter(filterRender);
    }

    @Override
    public <T extends FilterRender & IAdjustable> SplitBuilder addFilter(T filterRender, float progress) {
        return (SplitBuilder) super.addFilter(filterRender, progress);
    }

    @Override
    public SplitBuilder enableRecord(String outputPath, boolean recordVideo, boolean recordAudio) {
        return (SplitBuilder) super.enableRecord(outputPath, recordVideo, recordAudio);
    }
}

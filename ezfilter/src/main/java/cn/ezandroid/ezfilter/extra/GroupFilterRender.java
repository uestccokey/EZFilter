package cn.ezandroid.ezfilter.extra;

import java.util.ArrayList;
import java.util.List;

import cn.ezandroid.ezfilter.core.FBORender;
import cn.ezandroid.ezfilter.core.FilterRender;
import cn.ezandroid.ezfilter.core.OnTextureAvailableListener;

/**
 * 组合滤镜渲染器
 * <p>
 * 支持将多个滤镜串起来作为一个滤镜进行使用
 *
 * @author like
 * @date 2017-09-15
 */
public class GroupFilterRender extends FilterRender {

    protected List<FilterRender> mInitialFilters;
    protected List<FilterRender> mFilters;
    protected List<FilterRender> mTerminalFilters;

    public GroupFilterRender() {
        mInitialFilters = new ArrayList<>();
        mTerminalFilters = new ArrayList<>();
        mFilters = new ArrayList<>();
    }

    @Override
    public void destroy() {
        super.destroy();
        for (FilterRender filter : mFilters) {
            filter.destroy();
        }
    }

    @Override
    public void reInit() {
        for (FilterRender filter : mFilters) {
            filter.reInit();
        }
    }

    @Override
    public void setRenderSize(int width, int height) {
        for (FilterRender filter : mFilters) {
            filter.setRenderSize(width, height);
        }
    }

    @Override
    public void onTextureAvailable(int texture, FBORender source) {
        if (mTerminalFilters.contains(source)) {
            setWidth(source.getWidth());
            setHeight(source.getHeight());
            synchronized (getLock()) {
                for (OnTextureAvailableListener target : getTargets()) {
                    target.onTextureAvailable(texture, this);
                }
            }
        } else {
            for (FilterRender initialFilter : mInitialFilters) {
                initialFilter.onTextureAvailable(texture, source);
            }
        }
    }

    public void registerFilter(FilterRender filter) {
        if (!mFilters.contains(filter)) {
            mFilters.add(filter);
        }
    }

    public void registerInitialFilter(FilterRender filter) {
        mInitialFilters.add(filter);
        registerFilter(filter);
    }

    public void registerTerminalFilter(FilterRender filter) {
        mTerminalFilters.add(filter);
        registerFilter(filter);
    }

    public List<FilterRender> getFilters() {
        return mFilters;
    }

    public List<FilterRender> getInitialFilters() {
        return mInitialFilters;
    }

    public List<FilterRender> getTerminalFilters() {
        return mTerminalFilters;
    }
}

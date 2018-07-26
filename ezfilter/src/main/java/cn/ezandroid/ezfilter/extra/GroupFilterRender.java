package cn.ezandroid.ezfilter.extra;

import java.util.ArrayList;
import java.util.List;

import cn.ezandroid.ezfilter.core.FilterRender;
import cn.ezandroid.ezfilter.core.GLRender;
import cn.ezandroid.ezfilter.core.OnTextureAcceptableListener;

/**
 * 组合滤镜渲染器
 * <p>
 * 支持将多个滤镜串起来作为一个滤镜进行使用
 *
 * @author like
 * @date 2017-09-15
 */
public class GroupFilterRender extends FilterRender {

    protected final List<GLRender> mFilters = new ArrayList<>();

    protected final List<GLRender> mInitialFilters = new ArrayList<>();
    protected final List<GLRender> mTerminalFilters = new ArrayList<>();

    public GroupFilterRender() {
    }

    @Override
    public void destroy() {
        super.destroy();
        synchronized (mFilters) {
            for (GLRender filter : mFilters) {
                filter.destroy();
            }
        }
    }

    @Override
    public void reInit() {
        synchronized (mFilters) {
            for (GLRender filter : mFilters) {
                filter.reInit();
            }
        }
    }

    @Override
    public void setRenderSize(int width, int height) {
        synchronized (mFilters) {
            for (GLRender filter : mFilters) {
                filter.setRenderSize(width, height);
            }
        }
    }

    @Override
    public void onTextureAcceptable(int texture, GLRender source) {
        runAll(mRunOnDraw);
        if (mTerminalFilters.contains(source)) {
            setWidth(source.getWidth());
            setHeight(source.getHeight());
            synchronized (mTargets) {
                for (OnTextureAcceptableListener target : mTargets) {
                    target.onTextureAcceptable(texture, this);
                }
            }
        } else {
            synchronized (mInitialFilters) {
                for (GLRender initialFilter : mInitialFilters) {
                    initialFilter.onTextureAcceptable(texture, source);
                }
            }
        }
        runAll(mRunOnDrawEnd);
        logDraw();
    }

    public void registerFilter(GLRender filter) {
        synchronized (mFilters) {
            if (!mFilters.contains(filter)) {
                mFilters.add(filter);
            }
        }
    }

    public void registerInitialFilter(GLRender filter) {
        synchronized (mInitialFilters) {
            mInitialFilters.add(filter);
            registerFilter(filter);
        }
    }

    public void registerTerminalFilter(GLRender filter) {
        synchronized (mTerminalFilters) {
            mTerminalFilters.add(filter);
            registerFilter(filter);
        }
    }

    public List<GLRender> getFilters() {
        return mFilters;
    }

    public List<GLRender> getInitialFilters() {
        return mInitialFilters;
    }

    public List<GLRender> getTerminalFilters() {
        return mTerminalFilters;
    }
}

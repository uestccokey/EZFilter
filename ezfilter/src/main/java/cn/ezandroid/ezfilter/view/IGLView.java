package cn.ezandroid.ezfilter.view;

import android.view.Surface;

import cn.ezandroid.ezfilter.environment.IGLEnvironment;

/**
 * 支持OpenGL渲染的View的接口
 *
 * @author like
 * @date 2017-09-20
 */
public interface IGLView {

    /**
     * 获取高度
     *
     * @return
     */
    int getWidth();

    /**
     * 获取宽度
     *
     * @return
     */
    int getHeight();

    /**
     * 设置要渲染到的Surface
     *
     * @param surface
     */
    void setSurface(Surface surface);

    /**
     * 设置GL环境
     *
     * @param render
     */
    void setGLEnvironment(IGLEnvironment render);
}

package cn.ezandroid.ezfilter.view;

import android.content.Context;

import cn.ezandroid.ezfilter.core.FBORender;
import cn.ezandroid.ezfilter.core.RenderPipeline;

/**
 * 渲染视图接口
 *
 * @author like
 * @date 2017-03-27
 */
public interface IRenderView extends IRender {

    /**
     * 初始化渲染管道
     *
     * @param startPointRender 渲染起点
     */
    void initRenderPipeline(FBORender startPointRender);

    /**
     * 获取渲染管道
     *
     * @return
     */
    RenderPipeline getRenderPipeline();

    /**
     * 设置缩放规则
     *
     * @param scaleType 缩放规则
     */
    void setScaleType(RenderViewHelper.ScaleType scaleType);

    /**
     * 设置宽高比及最大宽度最大高度
     * maxWidth和maxHeight有一个设置为0时表示MatchParent
     *
     * @param ratio     宽高比
     * @param maxWidth  最大宽度
     * @param maxHeight 最大高度
     * @return 是否渲染尺寸有变化
     */
    boolean setAspectRatio(float ratio, int maxWidth, int maxHeight);

    /**
     * 设置顺时针旋转90度的次数
     * 取值-3~0，0~3，表示旋转-270~0，0~270度
     *
     * @param numOfTimes 旋转次数
     * @return 是否渲染尺寸有变化
     */
    boolean setRotate90Degrees(int numOfTimes);


    /**
     * 刷新视图
     */
    void requestLayout();

    /**
     * 获取预览宽度
     *
     * @return
     */
    int getPreviewWidth();

    /**
     * 获取预览高度
     *
     * @return
     */
    int getPreviewHeight();

    Context getContext();
}

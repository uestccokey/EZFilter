package cn.ezandroid.lib.ezfilter.core.environment;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.View;

import cn.ezandroid.lib.ezfilter.core.FBORender;
import cn.ezandroid.lib.ezfilter.core.RenderPipeline;

/**
 * 支持自适应布局的GLTextureView
 *
 * @author like
 * @date 2017-09-15
 */
public class TextureFitView extends GLTextureView implements IFitView {

    private RenderPipeline mPipeline;

    private FitViewHelper mHelper;

    public TextureFitView(final Context context) {
        this(context, null);
        init();
    }

    public TextureFitView(final Context context, AttributeSet attr) {
        super(context, attr);
        init();
    }

    private void init() {
        setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR
                | GLSurfaceView.DEBUG_LOG_GL_CALLS);
        setEGLContextClientVersion(2);
        mHelper = new FitViewHelper();

        mPipeline = new RenderPipeline();
        setRenderer(mPipeline);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    /**
     * 初始化渲染管道
     *
     * @param startPointRender 渲染起点
     */
    @Override
    public void initRenderPipeline(FBORender startPointRender) {
//        mPipeline.pauseRender();
        if (startPointRender != null) {
            mPipeline.setStartPointRender(startPointRender);
        }
    }

    /**
     * 获取渲染管道
     *
     * @return
     */
    @Override
    public RenderPipeline getRenderPipeline() {
        return mPipeline;
    }

    /**
     * 设置缩放规则
     *
     * @param scaleType 缩放规则
     */
    @Override
    public void setScaleType(FitViewHelper.ScaleType scaleType) {
        mHelper.setScaleType(scaleType);
    }

    /**
     * 设置宽高比及最大宽度最大高度
     * maxWidth和maxHeight有一个设置为0时表示MatchParent
     *
     * @param ratio     宽高比
     * @param maxWidth  最大宽度
     * @param maxHeight 最大高度
     * @return 是否应该调用requestLayout刷新视图
     */
    @Override
    public boolean setAspectRatio(float ratio, int maxWidth, int maxHeight) {
        boolean change = mHelper.setAspectRatio(ratio, maxWidth, maxHeight);
        if (mPipeline != null) {
            mPipeline.setRenderSize(getPreviewWidth(), getPreviewHeight());
        }
        return change;
    }

    /**
     * 设置顺时针旋转90度的次数
     * 取值-3~0，0~3，表示旋转-270~0，0~270度
     *
     * @param numOfTimes 旋转次数
     * @return 是否应该调用requestLayout刷新视图
     */
    @Override
    public boolean setRotate90Degrees(int numOfTimes) {
        boolean change = mHelper.setRotate90Degrees(numOfTimes);
        if (mPipeline != null) {
            mPipeline.setRotate90Degrees(mHelper.getRotation90Degrees());
            mPipeline.setRenderSize(getPreviewWidth(), getPreviewHeight());
        }
        return change;
    }

    /**
     * 获取宽高比
     *
     * @return
     */
    public float getAspectRatio() {
        return mHelper.getAspectRatio();
    }

    /**
     * 获取顺时针旋转90度的次数
     *
     * @return
     */
    public int getRotation90Degrees() {
        return mHelper.getRotation90Degrees();
    }

    /**
     * 获取预览宽度
     *
     * @return
     */
    @Override
    public int getPreviewWidth() {
        return mHelper.getPreviewWidth();
    }

    /**
     * 获取预览高度
     *
     * @return
     */
    @Override
    public int getPreviewHeight() {
        return mHelper.getPreviewHeight();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int previewWidth = MeasureSpec.getSize(widthSpec);
        int previewHeight = MeasureSpec.getSize(heightSpec);

        // 计算预览区域大小
        mHelper.calculatePreviewSize(previewWidth, previewHeight);
        super.onMeasure(MeasureSpec.makeMeasureSpec(mHelper.getPreviewWidth(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mHelper.getPreviewHeight(), MeasureSpec.EXACTLY));
    }
}

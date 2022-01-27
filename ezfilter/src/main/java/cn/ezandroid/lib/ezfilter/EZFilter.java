package cn.ezandroid.lib.ezfilter;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.camera2.CameraDevice;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Size;

import java.util.ArrayList;
import java.util.List;

import cn.ezandroid.lib.ezfilter.camera.Camera2Builder;
import cn.ezandroid.lib.ezfilter.camera.CameraBuilder;
import cn.ezandroid.lib.ezfilter.core.FBORender;
import cn.ezandroid.lib.ezfilter.core.FilterRender;
import cn.ezandroid.lib.ezfilter.core.GLRender;
import cn.ezandroid.lib.ezfilter.core.RenderPipeline;
import cn.ezandroid.lib.ezfilter.core.cache.IBitmapCache;
import cn.ezandroid.lib.ezfilter.core.cache.LruBitmapCache;
import cn.ezandroid.lib.ezfilter.core.environment.IFitView;
import cn.ezandroid.lib.ezfilter.extra.IAdjustable;
import cn.ezandroid.lib.ezfilter.image.BitmapBuilder;
import cn.ezandroid.lib.ezfilter.media.record.RecordableRender;
import cn.ezandroid.lib.ezfilter.multi.MultiBuilder;
import cn.ezandroid.lib.ezfilter.multi.MultiInput;
import cn.ezandroid.lib.ezfilter.split.SplitBuilder;
import cn.ezandroid.lib.ezfilter.split.SplitInput;
import cn.ezandroid.lib.ezfilter.video.VideoBuilder;
import cn.ezandroid.lib.ezfilter.view.ViewBuilder;
import cn.ezandroid.lib.ezfilter.view.glview.IGLView;

/**
 * 滤镜处理
 *
 * @author like
 * @date 2017-09-15
 */
public class EZFilter {

    private EZFilter() {
    }

    /**
     * 默认的图片缓存 4分支1最大内存
     */
    private static IBitmapCache sBitmapCache = new LruBitmapCache((int) (Runtime.getRuntime().maxMemory() / 4));

    /**
     * 设置图片缓存
     *
     * @param bitmapCache
     */
    public static void setBitmapCache(IBitmapCache bitmapCache) {
        sBitmapCache = bitmapCache;
    }

    /**
     * 获取图片缓存
     *
     * @return
     */
    public static IBitmapCache getBitmapCache() {
        return sBitmapCache;
    }

    /**
     * 设置图片输入
     *
     * @param bitmap 图片
     * @return
     */
    public static BitmapBuilder input(Bitmap bitmap) {
        return new BitmapBuilder(bitmap);
    }

    /**
     * 设置视频输入
     *
     * @param uri 视频Uri
     * @return
     */
    public static VideoBuilder input(Uri uri) {
        return new VideoBuilder(uri);
    }

    /**
     * 设置Camera输入
     *
     * @param camera 摄像头
     * @param size   预览尺寸
     * @return
     */
    public static CameraBuilder input(Camera camera, Camera.Size size) {
        return new CameraBuilder(camera, size);
    }

    /**
     * 设置Camera2输入
     *
     * @param camera2 摄像头
     * @param size    预览尺寸
     * @return
     */
    public static Camera2Builder input(CameraDevice camera2, Size size) {
        return new Camera2Builder(camera2, size);
    }

    /**
     * 设置View输入
     *
     * @param view 视图
     * @return
     */
    public static ViewBuilder input(IGLView view) {
        return new ViewBuilder(view);
    }

    /**
     * 设置多输入源
     *
     * @param builders   输入源列表
     * @param multiInput 多输入源渲染器
     * @return
     */
    public static MultiBuilder input(List<Builder> builders, MultiInput multiInput) {
        return new MultiBuilder(builders, multiInput);
    }

    /**
     * 设置拆分输入源
     *
     * @param builder    输入源
     * @param splitInput 拆分渲染器
     * @return
     */
    public static SplitBuilder input(Builder builder, SplitInput splitInput) {
        return new SplitBuilder(builder, splitInput);
    }

    /**
     * 构造器基类
     * <p>
     * 支持链式操作
     */
    public abstract static class Builder {

        protected List<FilterRender> mFilterRenders = new ArrayList<>();

        protected boolean mEnableRecordVideo;
        protected boolean mEnableRecordAudio;

        protected String mOutputPath;

        protected Handler mMainHandler = new Handler(Looper.getMainLooper());

        protected Builder() {
        }

        /**
         * 获取渲染起点
         *
         * @return 渲染起点
         */
        public abstract FBORender getStartPointRender(IFitView view);

        /**
         * 获取渲染视图宽高比
         *
         * @return 宽高比
         */
        public abstract float getAspectRatio(IFitView view);

        /**
         * 添加滤镜
         *
         * @param filterRender
         * @return
         */
        protected Builder addFilter(FilterRender filterRender) {
            if (filterRender != null && !mFilterRenders.contains(filterRender)) {
                filterRender.setBitmapCache(sBitmapCache);
                mFilterRenders.add(filterRender);
            }
            return this;
        }

        /**
         * 添加滤镜，并设置强度
         *
         * @param filterRender
         * @param progress
         * @param <T>
         * @return
         */
        protected <T extends FilterRender & IAdjustable> Builder addFilter(T filterRender, float progress) {
            if (filterRender != null && !mFilterRenders.contains(filterRender)) {
                filterRender.setBitmapCache(sBitmapCache);
                // 调节强度
                filterRender.adjust(progress);
                mFilterRenders.add(filterRender);
            }
            return this;
        }

        /**
         * 支持录制开关
         *
         * @param outputPath  输出路径
         * @param recordVideo 录制影像
         * @param recordAudio 录制音频
         * @return
         */
        protected Builder enableRecord(String outputPath, boolean recordVideo, boolean recordAudio) {
            mOutputPath = outputPath;

            mEnableRecordVideo = recordVideo;
            mEnableRecordAudio = recordAudio;
            return this;
        }

        /**
         * 渲染到View中
         *
         * @param view  要渲染到的View
         * @param clean 是否要清空渲染管道
         * @return 渲染管道
         */
        public RenderPipeline into(IFitView view, boolean clean) {
            RenderPipeline pipeline = view.getRenderPipeline();
            if (pipeline != null && clean) {
                pipeline.clean();
            }

            view.initRenderPipeline(getStartPointRender(view));

            pipeline = view.getRenderPipeline();

            float aspectRatio = getAspectRatio(view);
            boolean change = view.setAspectRatio(aspectRatio, 0, 0);
            view.requestRender();

            if (pipeline != null) {
                pipeline.clearEndPointRenders();

                // 添加用于显示的终点渲染器
                pipeline.addEndPointRender(new GLRender());

                // 要支持录制时，添加用于录制的终点渲染器
                if (mEnableRecordVideo || mEnableRecordAudio) {
                    pipeline.addEndPointRender(new RecordableRender(mOutputPath,
                            mEnableRecordVideo, mEnableRecordAudio));
                }

                for (FilterRender filterRender : mFilterRenders) {
                    pipeline.addFilterRender(filterRender);
                }
                pipeline.startRender();
            }

            if (change) {
                // 确保requestLayout()在主线程调用
                mMainHandler.post(view::requestLayout);
            }
            return pipeline;
        }

        /**
         * 渲染到View中
         *
         * @param view 要渲染到的View
         * @return 渲染管道
         */
        public RenderPipeline into(IFitView view) {
            return into(view, true);
        }
    }
}

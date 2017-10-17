package cn.ezandroid.ezfilter;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.camera2.CameraDevice;
import android.net.Uri;
import android.util.Size;

import java.util.ArrayList;
import java.util.List;

import cn.ezandroid.ezfilter.camera.Camera2Builder;
import cn.ezandroid.ezfilter.camera.CameraBuilder;
import cn.ezandroid.ezfilter.camera.record.RecordableEndPointRender;
import cn.ezandroid.ezfilter.core.FBORender;
import cn.ezandroid.ezfilter.core.FilterRender;
import cn.ezandroid.ezfilter.core.RenderPipeline;
import cn.ezandroid.ezfilter.core.cache.IBitmapCache;
import cn.ezandroid.ezfilter.core.cache.LruBitmapCache;
import cn.ezandroid.ezfilter.environment.IFitView;
import cn.ezandroid.ezfilter.extra.IAdjustable;
import cn.ezandroid.ezfilter.image.BitmapBuilder;
import cn.ezandroid.ezfilter.video.VideoBuilder;
import cn.ezandroid.ezfilter.view.ViewBuilder;
import cn.ezandroid.ezfilter.view.glview.IGLView;

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
     * @return
     */
    public static CameraBuilder input(Camera camera) {
        return new CameraBuilder(camera);
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
     * 构造器基类
     * <p>
     * 支持链式操作
     */
    public abstract static class Builder {

        protected List<FilterRender> mFilterRenders = new ArrayList<>();

        protected boolean mEnableRecordVideo;
        protected boolean mEnableRecordAudio;

        protected String mOutputPath;

        protected Builder() {
        }

        /**
         * 获取渲染起点
         *
         * @return 渲染起点
         */
        protected abstract FBORender getStartPointRender(IFitView view);

        /**
         * 获取渲染视图宽高比
         *
         * @return 宽高比
         */
        protected abstract float getAspectRatio(IFitView view);

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
         * @param recordVideo 录制视频
         * @param recordAudio 录制音频
         * @return
         */
        protected Builder enableRecord(String outputPath, boolean recordVideo, boolean recordAudio) {
            mOutputPath = outputPath;

            mEnableRecordVideo = recordVideo;
            mEnableRecordAudio = recordAudio;
            return this;
        }

        public RenderPipeline into(IFitView view) {
            RenderPipeline pipeline = view.getRenderPipeline();
            // 如果渲染管道不为空，确保渲染管道是干净的
            if (pipeline != null) {
                pipeline.clean();
            }

            view.initRenderPipeline(getStartPointRender(view));

            pipeline = view.getRenderPipeline();
            if (pipeline != null) {
                if (mEnableRecordVideo || mEnableRecordAudio) {
                    if (!(pipeline.getEndPointRender() instanceof RecordableEndPointRender)) {
                        pipeline.setEndPointRender(new RecordableEndPointRender(mOutputPath,
                                mEnableRecordVideo, mEnableRecordAudio));
                    }
                }

                for (FilterRender filterRender : mFilterRenders) {
                    pipeline.addFilterRender(filterRender);
                }
                pipeline.startRender();
            }

            boolean change = view.setAspectRatio(getAspectRatio(view), 0, 0);
            view.requestRender();
            if (change) {
                view.requestLayout();
            }
            return pipeline;
        }
    }
}

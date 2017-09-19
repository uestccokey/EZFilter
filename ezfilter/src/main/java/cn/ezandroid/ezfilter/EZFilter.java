package cn.ezandroid.ezfilter;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

import cn.ezandroid.ezfilter.cache.IBitmapCache;
import cn.ezandroid.ezfilter.cache.LruBitmapCache;
import cn.ezandroid.ezfilter.core.FilterRender;
import cn.ezandroid.ezfilter.core.RenderPipeline;
import cn.ezandroid.ezfilter.io.input.BitmapInput;
import cn.ezandroid.ezfilter.io.input.CameraInput;
import cn.ezandroid.ezfilter.io.input.VideoInput;
import cn.ezandroid.ezfilter.offscreen.OffscreenHelper;
import cn.ezandroid.ezfilter.view.IRenderView;

/**
 * 滤镜处理
 *
 * @author like
 * @date 2017-09-15
 */
public class EZFilter {

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
     * 图片处理构造器
     * <p>
     * 支持链式操作
     * 比如 Bitmap output = new EZFilter.BitmapBuilder().setBitmap(input).addFilter(filter).capture();
     * 比如 new EZFilter.BitmapBuilder().setBitmap(input).addFilter(filter).into(view)
     */
    public static class BitmapBuilder extends Builder {

        private Bitmap mBitmap;

        public BitmapBuilder setBitmap(Bitmap bitmap) {
            mBitmap = bitmap;
            return this;
        }

        public Bitmap capture() {
            // 离屏渲染
            OffscreenHelper helper = new OffscreenHelper(mBitmap);
            for (FilterRender filterRender : mFilterRenders) {
                helper.addFilterRender(filterRender);
            }
            return helper.capture();
        }

        public Bitmap capture(int width, int height) {
            // 离屏渲染
            OffscreenHelper helper = new OffscreenHelper(mBitmap);
            for (FilterRender filterRender : mFilterRenders) {
                helper.addFilterRender(filterRender);
            }
            return helper.capture(width, height);
        }

        @Override
        protected float setRenderPipeline(IRenderView view) {
            BitmapInput bitmapInput = new BitmapInput(mBitmap);
            view.setRenderPipeline(bitmapInput);
            return mBitmap.getWidth() * 1.0f / mBitmap.getHeight();
        }

        @Override
        public BitmapBuilder setRotation(int rotation) {
            return (BitmapBuilder) super.setRotation(rotation);
        }

        @Override
        public BitmapBuilder addFilter(FilterRender filterRender) {
            return (BitmapBuilder) super.addFilter(filterRender);
        }
    }

    /**
     * 视频处理构造器
     */
    public static class VideoBuilder extends Builder {

        private Uri mVideo;
        private boolean mVideoLoop;

        public VideoBuilder setVideo(Uri uri) {
            mVideo = uri;
            return this;
        }

        public VideoBuilder setVideoLoop(boolean loop) {
            mVideoLoop = loop;
            return this;
        }

        @Override
        protected float setRenderPipeline(IRenderView view) {
            VideoInput videoInput = new VideoInput(view, mVideo);
            videoInput.setLoop(mVideoLoop);
            videoInput.start();
            view.setRenderPipeline(videoInput);
            MediaMetadataRetriever metadata = new MediaMetadataRetriever();
            metadata.setDataSource(view.getContext(), mVideo);
            String width = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String height = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            return Integer.parseInt(width) * 1.0f / Integer.parseInt(height);
        }

        @Override
        public VideoBuilder setRotation(int rotation) {
            return (VideoBuilder) super.setRotation(rotation);
        }

        @Override
        public VideoBuilder addFilter(FilterRender filterRender) {
            return (VideoBuilder) super.addFilter(filterRender);
        }
    }

    /**
     * 拍照处理构造器
     */
    public static class CameraBuilder extends Builder {

        private Camera mCamera;

        public CameraBuilder setCamera(Camera camera) {
            mCamera = camera;
            return this;
        }

        @Override
        protected float setRenderPipeline(IRenderView view) {
            CameraInput cameraInput = new CameraInput(view, mCamera);
            view.setRenderPipeline(cameraInput);
            Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
            return previewSize.height * 1.0f / previewSize.width;
        }

        @Override
        public CameraBuilder setRotation(int rotation) {
            return (CameraBuilder) super.setRotation(rotation);
        }

        @Override
        public CameraBuilder addFilter(FilterRender filterRender) {
            return (CameraBuilder) super.addFilter(filterRender);
        }
    }

    /**
     * 构造器基类
     */
    public abstract static class Builder {

        int mRotation;

        List<FilterRender> mFilterRenders = new ArrayList<>();

        private Builder() {
        }

        Builder setRotation(int rotation) {
            mRotation = rotation;
            return this;
        }

        Builder addFilter(FilterRender filterRender) {
            if (filterRender != null && !mFilterRenders.contains(filterRender)) {
                filterRender.setBitmapCache(sBitmapCache);
                mFilterRenders.add(filterRender);
            }
            return this;
        }

        /**
         * 设置渲染管道，返回宽高比
         *
         * @param view
         * @return 宽高比
         */
        abstract float setRenderPipeline(IRenderView view);

        public RenderPipeline into(IRenderView view) {
            RenderPipeline pipeline = view.getRenderPipeline();
            // 如果渲染管道不为空，确保渲染管道是干净的
            if (pipeline != null) {
                pipeline.clean();
            }

            float ratio = setRenderPipeline(view);

            pipeline = view.getRenderPipeline();
            if (pipeline != null) {
                for (FilterRender filterRender : mFilterRenders) {
                    pipeline.addFilterRender(filterRender);
                }
                pipeline.startRender();
            }

            boolean change = view.setAspectRatio(ratio, 0, 0);
            change = view.setRotate90Degrees(mRotation) || change;
            view.requestRender();
            if (change) {
                view.requestLayout();
            }
            return pipeline;
        }
    }
}

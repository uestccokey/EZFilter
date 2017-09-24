package cn.ezandroid.ezfilter;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.camera2.CameraDevice;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.util.Size;

import java.util.ArrayList;
import java.util.List;

import cn.ezandroid.ezfilter.cache.IBitmapCache;
import cn.ezandroid.ezfilter.cache.LruBitmapCache;
import cn.ezandroid.ezfilter.core.FBORender;
import cn.ezandroid.ezfilter.core.FilterRender;
import cn.ezandroid.ezfilter.core.RenderPipeline;
import cn.ezandroid.ezfilter.environment.IRenderView;
import cn.ezandroid.ezfilter.environment.RenderViewHelper;
import cn.ezandroid.ezfilter.io.input.BitmapInput;
import cn.ezandroid.ezfilter.io.input.Camera2Input;
import cn.ezandroid.ezfilter.io.input.CameraInput;
import cn.ezandroid.ezfilter.io.input.VideoInput;
import cn.ezandroid.ezfilter.io.input.ViewInput;
import cn.ezandroid.ezfilter.offscreen.OffscreenHelper;
import cn.ezandroid.ezfilter.view.IGLView;

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
     * @param bitmap
     * @return
     */
    public static BitmapBuilder setBitmap(Bitmap bitmap) {
        return new BitmapBuilder(bitmap);
    }

    /**
     * 设置视频输入
     *
     * @param uri
     * @return
     */
    public static VideoBuilder setVideo(Uri uri) {
        return new VideoBuilder(uri);
    }

    /**
     * 设置Camera输入
     *
     * @param camera
     * @return
     */
    public static CameraBuilder setCamera(Camera camera) {
        return new CameraBuilder(camera);
    }

    /**
     * 设置Camera2输入
     *
     * @param camera2
     * @param size
     * @return
     */
    public static Camera2Builder setCamera2(CameraDevice camera2, Size size) {
        return new Camera2Builder(camera2, size);
    }

    /**
     * 设置View输入
     *
     * @param view
     * @return
     */
    public static ViewBuilder setView(IGLView view) {
        return new ViewBuilder(view);
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

        private BitmapBuilder(Bitmap bitmap) {
            mBitmap = bitmap;
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
        FBORender getStartPointRender(IRenderView view) {
            return new BitmapInput(mBitmap);
        }

        @Override
        float getAspectRatio(IRenderView view) {
            return mBitmap.getWidth() * 1.0f / mBitmap.getHeight();
        }

        @Override
        public BitmapBuilder setScaleType(RenderViewHelper.ScaleType scaleType) {
            return (BitmapBuilder) super.setScaleType(scaleType);
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

    public static class VideoBuilder extends Builder {

        private Uri mVideo;
        private boolean mVideoLoop;

        private VideoBuilder(Uri uri) {
            mVideo = uri;
        }

        public VideoBuilder setVideoLoop(boolean loop) {
            mVideoLoop = loop;
            return this;
        }

        @Override
        FBORender getStartPointRender(IRenderView view) {
            VideoInput videoInput = new VideoInput(view.getContext(), view, mVideo);
            videoInput.setLoop(mVideoLoop);
            videoInput.start();
            return videoInput;
        }

        @Override
        float getAspectRatio(IRenderView view) {
            MediaMetadataRetriever metadata = new MediaMetadataRetriever();
            metadata.setDataSource(view.getContext(), mVideo);
            String width = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String height = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            metadata.release();
            return Integer.parseInt(width) * 1.0f / Integer.parseInt(height);
        }

        @Override
        public VideoBuilder setScaleType(RenderViewHelper.ScaleType scaleType) {
            return (VideoBuilder) super.setScaleType(scaleType);
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

    public static class CameraBuilder extends Builder {

        private Camera mCamera;

        private CameraBuilder(Camera camera) {
            mCamera = camera;
        }

        @Override
        FBORender getStartPointRender(IRenderView view) {
            return new CameraInput(view, mCamera);
        }

        @Override
        float getAspectRatio(IRenderView view) {
            Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
            return previewSize.height * 1.0f / previewSize.width;
        }

        @Override
        public CameraBuilder setScaleType(RenderViewHelper.ScaleType scaleType) {
            return (CameraBuilder) super.setScaleType(scaleType);
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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static class Camera2Builder extends Builder {

        private CameraDevice mCameraDevice;
        private Size mPreviewSize;

        private Camera2Builder(CameraDevice camera2, Size size) {
            mCameraDevice = camera2;
            mPreviewSize = size;
        }

        @Override
        FBORender getStartPointRender(IRenderView view) {
            return new Camera2Input(view, mCameraDevice, mPreviewSize);
        }

        @Override
        float getAspectRatio(IRenderView view) {
            return mPreviewSize.getHeight() * 1.0f / mPreviewSize.getWidth();
        }

        @Override
        public Camera2Builder setScaleType(RenderViewHelper.ScaleType scaleType) {
            return (Camera2Builder) super.setScaleType(scaleType);
        }

        @Override
        public Camera2Builder setRotation(int rotation) {
            return (Camera2Builder) super.setRotation(rotation);
        }

        @Override
        public Camera2Builder addFilter(FilterRender filterRender) {
            return (Camera2Builder) super.addFilter(filterRender);
        }
    }

    public static class ViewBuilder extends Builder {

        private IGLView mGLView;

        private ViewBuilder(IGLView view) {
            mGLView = view;
        }

        @Override
        FBORender getStartPointRender(IRenderView view) {
            return new ViewInput(mGLView);
        }

        @Override
        float getAspectRatio(IRenderView view) {
            if (mGLView.getHeight() != 0) {
                return mGLView.getWidth() * 1.0f / mGLView.getHeight();
            }
            return 1;
        }

        @Override
        public ViewBuilder setScaleType(RenderViewHelper.ScaleType scaleType) {
            return (ViewBuilder) super.setScaleType(scaleType);
        }

        @Override
        public ViewBuilder setRotation(int rotation) {
            return (ViewBuilder) super.setRotation(rotation);
        }

        @Override
        public ViewBuilder addFilter(FilterRender filterRender) {
            return (ViewBuilder) super.addFilter(filterRender);
        }

        public RenderPipeline into(IRenderView view) {
            mGLView.setGLEnvironment(view);
            return super.into(view);
        }
    }

    /**
     * 构造器基类
     */
    public abstract static class Builder {

        int mRotation;

        List<FilterRender> mFilterRenders = new ArrayList<>();

        RenderViewHelper.ScaleType mScaleType = RenderViewHelper.ScaleType.CENTER_INSIDE;

        private Builder() {
        }

        /**
         * 获取渲染起点
         *
         * @return 宽高比
         */
        abstract FBORender getStartPointRender(IRenderView view);

        /**
         * 获取渲染视图宽高比
         *
         * @return
         */
        abstract float getAspectRatio(IRenderView view);

        Builder setScaleType(RenderViewHelper.ScaleType scaleType) {
            mScaleType = scaleType;
            return this;
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

        public RenderPipeline into(IRenderView view) {
            RenderPipeline pipeline = view.getRenderPipeline();
            // 如果渲染管道不为空，确保渲染管道是干净的
            if (pipeline != null) {
                pipeline.clean();
            }

            view.initRenderPipeline(getStartPointRender(view));

            pipeline = view.getRenderPipeline();
            if (pipeline != null) {
                for (FilterRender filterRender : mFilterRenders) {
                    pipeline.addFilterRender(filterRender);
                }
                pipeline.startRender();
            }

            view.setScaleType(mScaleType);
            boolean change = view.setAspectRatio(getAspectRatio(view), 0, 0);
            change = view.setRotate90Degrees(mRotation) || change;
            view.requestRender();
            if (change) {
                view.requestLayout();
            }
            return pipeline;
        }
    }
}

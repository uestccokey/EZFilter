package cn.ezandroid.ezfilter;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.camera2.CameraDevice;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.util.Size;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.ezandroid.ezfilter.camera.Camera2Input;
import cn.ezandroid.ezfilter.camera.CameraInput;
import cn.ezandroid.ezfilter.camera.record.RecordableEndPointRender;
import cn.ezandroid.ezfilter.core.FBORender;
import cn.ezandroid.ezfilter.core.FilterRender;
import cn.ezandroid.ezfilter.core.RenderPipeline;
import cn.ezandroid.ezfilter.core.cache.IBitmapCache;
import cn.ezandroid.ezfilter.core.cache.LruBitmapCache;
import cn.ezandroid.ezfilter.environment.IFitView;
import cn.ezandroid.ezfilter.extra.IAdjustable;
import cn.ezandroid.ezfilter.image.BitmapInput;
import cn.ezandroid.ezfilter.image.offscreen.OffscreenImage;
import cn.ezandroid.ezfilter.video.VideoInput;
import cn.ezandroid.ezfilter.video.offscreen.OffscreenVideo;
import cn.ezandroid.ezfilter.video.player.IMediaPlayer;
import cn.ezandroid.ezfilter.view.ViewInput;
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

        List<FilterRender> mFilterRenders = new ArrayList<>();

        boolean mEnableRecordVideo;
        boolean mEnableRecordAudio;

        String mOutputPath;

        private Builder() {
        }

        /**
         * 获取渲染起点
         *
         * @return 渲染起点
         */
        abstract FBORender getStartPointRender(IFitView view);

        /**
         * 获取渲染视图宽高比
         *
         * @return 宽高比
         */
        abstract float getAspectRatio(IFitView view);

        /**
         * 添加滤镜
         *
         * @param filterRender
         * @return
         */
        Builder addFilter(FilterRender filterRender) {
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
        <T extends FilterRender & IAdjustable> Builder addFilter(T filterRender, float progress) {
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
        Builder enableRecord(String outputPath, boolean recordVideo, boolean recordAudio) {
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
                    pipeline.setEndPointRender(new RecordableEndPointRender(mOutputPath,
                            mEnableRecordVideo, mEnableRecordAudio));
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

    /**
     * 图片处理构造器
     */
    public static class BitmapBuilder extends Builder {

        private Bitmap mBitmap;

        private BitmapBuilder(Bitmap bitmap) {
            mBitmap = bitmap;
        }

        public Bitmap output() {
            // 离屏渲染
            OffscreenImage offscreenImage = new OffscreenImage(mBitmap);
            for (FilterRender filterRender : mFilterRenders) {
                offscreenImage.addFilterRender(filterRender);
            }
            return offscreenImage.capture();
        }

        public Bitmap output(int width, int height) {
            // 离屏渲染
            OffscreenImage offscreenImage = new OffscreenImage(mBitmap);
            for (FilterRender filterRender : mFilterRenders) {
                offscreenImage.addFilterRender(filterRender);
            }
            return offscreenImage.capture(width, height);
        }

        @Override
        FBORender getStartPointRender(IFitView view) {
            return new BitmapInput(mBitmap);
        }

        @Override
        float getAspectRatio(IFitView view) {
            return mBitmap.getWidth() * 1.0f / mBitmap.getHeight();
        }

        @Override
        public BitmapBuilder addFilter(FilterRender filterRender) {
            return (BitmapBuilder) super.addFilter(filterRender);
        }

        @Override
        public <T extends FilterRender & IAdjustable> BitmapBuilder addFilter(T filterRender, float progress) {
            return (BitmapBuilder) super.addFilter(filterRender, progress);
        }

        @Override
        public BitmapBuilder enableRecord(String outputPath, boolean recordVideo, boolean recordAudio) {
            return (BitmapBuilder) super.enableRecord(outputPath, recordVideo, recordAudio);
        }
    }

    /**
     * 视频处理构造器
     */
    public static class VideoBuilder extends Builder {

        private Uri mVideo;
        private boolean mVideoLoop;
        private float mVideoVolume;
        private IMediaPlayer.OnPreparedListener mPreparedListener;
        private IMediaPlayer.OnCompletionListener mCompletionListener;

        private VideoBuilder(Uri uri) {
            mVideo = uri;
        }

        public VideoBuilder setLoop(boolean loop) {
            mVideoLoop = loop;
            return this;
        }

        public VideoBuilder setVolume(float volume) {
            mVideoVolume = volume;
            return this;
        }

        public VideoBuilder setPreparedListener(IMediaPlayer.OnPreparedListener listener) {
            mPreparedListener = listener;
            return this;
        }

        public VideoBuilder setCompletionListener(IMediaPlayer.OnCompletionListener listener) {
            mCompletionListener = listener;
            return this;
        }

        public void output(String output) {
            // 离屏渲染
            OffscreenVideo offscreenVideo = new OffscreenVideo(mVideo.getPath());
            try {
                for (FilterRender filterRender : mFilterRenders) {
                    offscreenVideo.addFilterRender(filterRender);
                }
                offscreenVideo.save(output);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void output(String output, int width, int height) {
            // 离屏渲染
            OffscreenVideo offscreenVideo = new OffscreenVideo(mVideo.getPath());
            try {
                for (FilterRender filterRender : mFilterRenders) {
                    offscreenVideo.addFilterRender(filterRender);
                }
                offscreenVideo.save(output, width, height);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        FBORender getStartPointRender(IFitView view) {
            VideoInput videoInput = new VideoInput(view.getContext(), view, mVideo);
            videoInput.setLoop(mVideoLoop);
            videoInput.setVolume(mVideoVolume, mVideoVolume);
            videoInput.setOnPreparedListener(mPreparedListener);
            videoInput.setOnCompletionListener(mCompletionListener);
            videoInput.start();
            return videoInput;
        }

        @Override
        float getAspectRatio(IFitView view) {
            MediaMetadataRetriever metadata = new MediaMetadataRetriever();
            try {
                metadata.setDataSource(view.getContext(), mVideo);
                String width = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                String height = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                return Integer.parseInt(width) * 1.0f / Integer.parseInt(height);
            } catch (Exception e) {
                e.printStackTrace();
                return 1;
            } finally {
                metadata.release();
            }
        }

        @Override
        public VideoBuilder addFilter(FilterRender filterRender) {
            return (VideoBuilder) super.addFilter(filterRender);
        }

        @Override
        public <T extends FilterRender & IAdjustable> VideoBuilder addFilter(T filterRender, float progress) {
            return (VideoBuilder) super.addFilter(filterRender, progress);
        }

        @Override
        public VideoBuilder enableRecord(String outputPath, boolean recordVideo, boolean recordAudio) {
            return (VideoBuilder) super.enableRecord(outputPath, recordVideo, recordAudio);
        }
    }

    /**
     * 摄像头处理构造器
     */
    public static class CameraBuilder extends Builder {

        private Camera mCamera;

        private CameraBuilder(Camera camera) {
            mCamera = camera;
        }

        @Override
        FBORender getStartPointRender(IFitView view) {
            return new CameraInput(view, mCamera);
        }

        @Override
        float getAspectRatio(IFitView view) {
            Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
            return previewSize.height * 1.0f / previewSize.width;
        }

        @Override
        public CameraBuilder addFilter(FilterRender filterRender) {
            return (CameraBuilder) super.addFilter(filterRender);
        }

        @Override
        public <T extends FilterRender & IAdjustable> CameraBuilder addFilter(T filterRender, float progress) {
            return (CameraBuilder) super.addFilter(filterRender, progress);
        }

        @Override
        public CameraBuilder enableRecord(String outputPath, boolean recordVideo, boolean recordAudio) {
            return (CameraBuilder) super.enableRecord(outputPath, recordVideo, recordAudio);
        }
    }

    /**
     * 摄像头处理构造器
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static class Camera2Builder extends Builder {

        private CameraDevice mCameraDevice;
        private Size mPreviewSize;

        private Camera2Builder(CameraDevice camera2, Size size) {
            mCameraDevice = camera2;
            mPreviewSize = size;
        }

        @Override
        FBORender getStartPointRender(IFitView view) {
            return new Camera2Input(view, mCameraDevice, mPreviewSize);
        }

        @Override
        float getAspectRatio(IFitView view) {
            return mPreviewSize.getHeight() * 1.0f / mPreviewSize.getWidth();
        }

        @Override
        public Camera2Builder addFilter(FilterRender filterRender) {
            return (Camera2Builder) super.addFilter(filterRender);
        }

        @Override
        public <T extends FilterRender & IAdjustable> Camera2Builder addFilter(T filterRender, float progress) {
            return (Camera2Builder) super.addFilter(filterRender, progress);
        }

        @Override
        public Camera2Builder enableRecord(String outputPath, boolean recordVideo, boolean recordAudio) {
            return (Camera2Builder) super.enableRecord(outputPath, recordVideo, recordAudio);
        }
    }

    /**
     * 视图处理构造器
     */
    public static class ViewBuilder extends Builder {

        private IGLView mGLView;

        private ViewBuilder(IGLView view) {
            mGLView = view;
        }

        @Override
        FBORender getStartPointRender(IFitView view) {
            return new ViewInput(mGLView);
        }

        @Override
        float getAspectRatio(IFitView view) {
            if (mGLView.getHeight() != 0) {
                return mGLView.getWidth() * 1.0f / mGLView.getHeight();
            }
            return 1;
        }

        @Override
        public ViewBuilder addFilter(FilterRender filterRender) {
            return (ViewBuilder) super.addFilter(filterRender);
        }

        @Override
        public <T extends FilterRender & IAdjustable> ViewBuilder addFilter(T filterRender, float progress) {
            return (ViewBuilder) super.addFilter(filterRender, progress);
        }

        @Override
        public ViewBuilder enableRecord(String outputPath, boolean recordVideo, boolean recordAudio) {
            return (ViewBuilder) super.enableRecord(outputPath, recordVideo, recordAudio);
        }

        public RenderPipeline into(IFitView view) {
            mGLView.setGLEnvironment(view);
            return super.into(view);
        }
    }
}

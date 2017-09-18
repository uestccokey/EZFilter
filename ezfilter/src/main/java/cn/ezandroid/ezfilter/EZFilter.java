package cn.ezandroid.ezfilter;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

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
     * 构造器
     * <p>
     * 支持链式操作
     * 比如 Bitmap output = new EZFilter.Builder().setBitmap(input).addFilter(filter).capture();
     * 比如 new EZFilter.Builder().setBitmap(input).addFilter(filter).into(view)
     */
    public static class Builder {

        private Bitmap mBitmap;
        private Uri mVideo;
        private Camera mCamera;

        private boolean mVideoLoop;

        private int mRotation;

        private List<FilterRender> mFilterRenders = new ArrayList<>();

        public Builder() {
        }

        public Builder setBitmap(Bitmap bitmap) {
            mBitmap = bitmap;
            return this;
        }

        public Builder setVideo(Uri uri) {
            mVideo = uri;
            return this;
        }

        public Builder setVideoLoop(boolean loop) {
            mVideoLoop = loop;
            return this;
        }

        public Builder setRotation(int rotation) {
            mRotation = rotation;
            return this;
        }

        public Builder setCamera(Camera camera) {
            mCamera = camera;
            return this;
        }

        public Builder addFilter(FilterRender filterRender) {
            if (filterRender != null && !mFilterRenders.contains(filterRender)) {
                mFilterRenders.add(filterRender);
            }
            return this;
        }

        public Bitmap capture() {
            if (mBitmap != null) {
                OffscreenHelper helper = new OffscreenHelper(mBitmap);
                for (FilterRender filterRender : mFilterRenders) {
                    helper.addFilterRender(filterRender);
                }
                return helper.capture();
            } else {
                throw new InvalidParameterException("暂时只支持图片截图");
            }
        }

        public RenderPipeline into(IRenderView view) {
            RenderPipeline pipeline = view.getRenderPipeline();
            // 如果渲染管道不为空，确保渲染管道是干净的
            if (pipeline != null) {
                pipeline.clean();
            }

            float ratio = 1f;
            if (mBitmap != null) {
                BitmapInput bitmapInput = new BitmapInput(mBitmap);
                view.setRenderPipeline(bitmapInput);
                ratio = mBitmap.getWidth() * 1.0f / mBitmap.getHeight();
            } else if (mVideo != null) {
                VideoInput videoInput = new VideoInput(view, mVideo);
                videoInput.setLoop(mVideoLoop);
                videoInput.start();
                view.setRenderPipeline(videoInput);
                MediaMetadataRetriever metadata = new MediaMetadataRetriever();
                metadata.setDataSource(view.getContext(), mVideo);
                String width = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                String height = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                ratio = Integer.parseInt(width) * 1.0f / Integer.parseInt(height);
            } else if (mCamera != null) {
                CameraInput cameraInput = new CameraInput(view, mCamera);
                view.setRenderPipeline(cameraInput);
                Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
                ratio = previewSize.height * 1.0f / previewSize.width;
            }

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

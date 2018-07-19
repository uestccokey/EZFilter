package cn.ezandroid.ezfilter.video;

import android.media.MediaMetadataRetriever;
import android.net.Uri;

import java.io.IOException;
import java.util.HashMap;

import cn.ezandroid.ezfilter.EZFilter;
import cn.ezandroid.ezfilter.core.FBORender;
import cn.ezandroid.ezfilter.core.FilterRender;
import cn.ezandroid.ezfilter.core.environment.IFitView;
import cn.ezandroid.ezfilter.core.util.NumberUtil;
import cn.ezandroid.ezfilter.extra.IAdjustable;
import cn.ezandroid.ezfilter.video.offscreen.OffscreenVideo;
import cn.ezandroid.ezfilter.video.player.DefaultMediaPlayer;
import cn.ezandroid.ezfilter.video.player.IMediaPlayer;

/**
 * 视频处理构造器
 *
 * @author like
 * @date 2017-09-15
 */
public class VideoBuilder extends EZFilter.Builder {

    private Uri mVideo;
    private boolean mVideoLoop = true;
    private float mVideoVolume = 1.0f;
    private IMediaPlayer.OnPreparedListener mPreparedListener;
    private IMediaPlayer.OnCompletionListener mCompletionListener;
    private IMediaPlayer.OnErrorListener mErrorListener;
    private IMediaPlayer mMediaPlayer = new DefaultMediaPlayer();
    private boolean mStartWhenReady = true;

    private VideoInput mVideoInput;

    public VideoBuilder(Uri uri) {
        mVideo = uri;
    }

    /**
     * 设置使用的媒体播放器
     *
     * @param mediaPlayer
     * @return
     */
    public VideoBuilder setMediaPlayer(IMediaPlayer mediaPlayer) {
        mMediaPlayer = mediaPlayer;
        return this;
    }

    /**
     * 设置是否循环播放
     *
     * @param loop
     * @return
     */
    public VideoBuilder setLoop(boolean loop) {
        mVideoLoop = loop;
        return this;
    }

    /**
     * 设置音量
     * 取值0~1
     *
     * @param volume
     * @return
     */
    public VideoBuilder setVolume(float volume) {
        mVideoVolume = volume;
        return this;
    }

    public VideoBuilder setStartWhenReady(boolean start) {
        mStartWhenReady = start;
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

    public VideoBuilder setErrorListener(IMediaPlayer.OnErrorListener listener) {
        mErrorListener = listener;
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
    public FBORender getStartPointRender(IFitView view) {
        if (mVideoInput == null) {
            mVideoInput = new VideoInput(view.getContext(), view, mVideo, mMediaPlayer);
            mVideoInput.setStartWhenReady(mStartWhenReady);
            mVideoInput.setLoop(mVideoLoop);
            mVideoInput.setVolume(mVideoVolume, mVideoVolume);
            mVideoInput.setOnPreparedListener(mPreparedListener);
            mVideoInput.setOnCompletionListener(mCompletionListener);
            mVideoInput.setOnErrorListener(mErrorListener);
        }
        return mVideoInput;
    }

    @Override
    public float getAspectRatio(IFitView view) {
        MediaMetadataRetriever metadata = new MediaMetadataRetriever();
        try {
            String scheme = mVideo.getScheme();
            if (scheme != null && (scheme.equals("http") || scheme.equals("https"))) {
                // 在线视频
                metadata.setDataSource(mVideo.toString(), new HashMap<>());
            } else {
                // 本地视频（SD卡或Assets目录）
                metadata.setDataSource(view.getContext(), mVideo);
            }
            String width = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String height = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String rotation = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            if ((NumberUtil.parseInt(rotation) / 90) % 2 != 0) {
                return NumberUtil.parseInt(height) * 1.0f / NumberUtil.parseInt(width);
            } else {
                return NumberUtil.parseInt(width) * 1.0f / NumberUtil.parseInt(height);
            }
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

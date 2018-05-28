package cn.ezandroid.ezfilter.video.player;

import android.content.Context;
import android.net.Uri;
import android.view.Surface;

import java.io.IOException;

/**
 * MediaPlayer接口
 * 支持自定义MediaPlayer播放视频
 *
 * @author like
 * @date 2017-08-16
 */
public interface IMediaPlayer {

    void setDataSource(Context var1, Uri var2) throws IOException, IllegalArgumentException,
            SecurityException, IllegalStateException;

    void prepareAsync() throws IllegalStateException;

    void start() throws IllegalStateException;

    void stop() throws IllegalStateException;

    void pause() throws IllegalStateException;

    void reset();

    int getVideoWidth();

    int getVideoHeight();

    boolean isPlaying();

    void seekTo(int var1) throws IllegalStateException;

    int getCurrentPosition();

    int getDuration();

    void release();

    void setVolume(float var1, float var2);

    void setOnPreparedListener(OnPreparedListener var1);

    void setOnCompletionListener(OnCompletionListener var1);

    void setLooping(boolean var1);

    boolean isLooping();

    void setSurface(Surface var1);

    interface OnCompletionListener {
        void onCompletion(IMediaPlayer var1);
    }

    interface OnPreparedListener {
        void onPrepared(IMediaPlayer var1);
    }
}

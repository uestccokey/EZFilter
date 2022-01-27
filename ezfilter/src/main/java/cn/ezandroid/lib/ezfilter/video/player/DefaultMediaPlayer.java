package cn.ezandroid.lib.ezfilter.video.player;

import android.media.MediaPlayer;

/**
 * 默认的视频播放器，采用系统MediaPlayer
 *
 * @author like
 * @date 2017-08-16
 */
public class DefaultMediaPlayer extends MediaPlayer implements IMediaPlayer {

    @Override
    public void setOnPreparedListener(final IMediaPlayer.OnPreparedListener var1) {
        setOnPreparedListener((MediaPlayer.OnPreparedListener) mediaPlayer ->
                var1.onPrepared(DefaultMediaPlayer.this));
    }

    @Override
    public void setOnCompletionListener(final IMediaPlayer.OnCompletionListener var1) {
        setOnCompletionListener((MediaPlayer.OnCompletionListener) mediaPlayer ->
                var1.onCompletion(DefaultMediaPlayer.this));
    }

    @Override
    public void setOnErrorListener(IMediaPlayer.OnErrorListener var1) {
        setOnErrorListener((MediaPlayer.OnErrorListener) (mp, what, extra) -> var1.onError(DefaultMediaPlayer.this, what, extra));
    }
}

package cn.ezandroid.ezfilter.io.player;

import android.media.MediaPlayer;

/**
 * 默认的视频播放器，采用系统MediaPlayer
 *
 * @author like
 * @date 2017-08-16
 */
public class SystemMediaPlayer extends MediaPlayer implements IMediaPlayer {

    @Override
    public void setOnPreparedListener(final IMediaPlayer.OnPreparedListener var1) {
        setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                var1.onPrepared(SystemMediaPlayer.this);
            }
        });
    }

    @Override
    public void setOnCompletionListener(final IMediaPlayer.OnCompletionListener var1) {
        setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                var1.onCompletion(SystemMediaPlayer.this);
            }
        });
    }
}

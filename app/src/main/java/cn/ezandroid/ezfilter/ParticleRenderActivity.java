package cn.ezandroid.ezfilter;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.Timer;
import java.util.TimerTask;

import cn.ezandroid.lib.ezfilter.EZFilter;
import cn.ezandroid.lib.ezfilter.core.RenderPipeline;
import cn.ezandroid.lib.ezfilter.core.environment.TextureFitView;
import cn.ezandroid.ezfilter.render.particle.ParticleRender;
import cn.ezandroid.ezfilter.render.particle.util.Geometry;
import cn.ezandroid.lib.ezfilter.video.VideoInput;
import cn.ezandroid.lib.ezfilter.video.player.IMediaPlayer;

/**
 * ParticleRenderActivity
 *
 * @author like
 * @date 2018-05-29
 */
public class ParticleRenderActivity extends BaseActivity {

    private TextureFitView mRenderView;
    private TextView mPlayButton;
    private SeekBar mSeekBar;

    private Timer mTimer;

    private RenderPipeline mRenderPipeline;
    private VideoInput mVideoInput;

    private boolean mTouchingSeekBar;
    private boolean mTouchingTextureView;

    private ParticleRender mParticleRender;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_particle_render);

        initView();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initView() {
        mParticleRender = new ParticleRender(this, () -> mVideoInput.getMediaPlayer().getCurrentPosition() / 1000f);

        mRenderView = $(R.id.render_view);
        mRenderView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mTouchingTextureView = true;
                    startVideo();
                    if (!mRenderPipeline.getFilterRenders().contains(mParticleRender)) {
                        mRenderPipeline.addFilterRender(mParticleRender);
                    }
                    mParticleRender.start();
                    mParticleRender.setPosition(new Geometry.Point(event.getX() * 2f / mRenderView.getWidth() - 1,
                            1 - event.getY() * 2f / mRenderView.getHeight(), 0f));
                    break;
                case MotionEvent.ACTION_MOVE:
                    mParticleRender.setPosition(new Geometry.Point(event.getX() * 2f / mRenderView.getWidth() - 1,
                            1 - event.getY() * 2f / mRenderView.getHeight(), 0f));
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mTouchingTextureView = false;
                    pauseVideo();
                    mParticleRender.pause();
                    break;
            }
            return true;
        });

        mPlayButton = $(R.id.play);
        mPlayButton.setOnClickListener(v -> {
            if (mVideoInput.isPlaying()) {
                pauseVideo();
            } else {
                startVideo();
            }
        });

        mSeekBar = $(R.id.seek_bar);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mVideoInput.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mTouchingSeekBar = true;
                pauseVideo();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mTouchingSeekBar = false;
                pauseVideo();
                mVideoInput.seekTo(seekBar.getProgress());
            }
        });

        new Thread() {
            public void run() {
                mRenderPipeline = EZFilter.input(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.test3))
                        .setLoop(false)
                        .setPreparedListener(var1 -> {
                            mSeekBar.setMax(var1.getDuration());
                            mVideoInput.seekTo(1);
                            pauseVideo();
                        })
                        .setCompletionListener(var1 -> {
                            if (!mTouchingSeekBar && !mTouchingTextureView) {
                                mVideoInput.seekTo(0);
                                startVideo();
                            }
                        })
                        .into(mRenderView);

                mVideoInput = (VideoInput) mRenderPipeline.getStartPointRender();
            }
        }.start();
    }

    private void startTimer() {
        if (mTimer != null) {
            mTimer.cancel();
        }
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    mSeekBar.setProgress(mVideoInput.getMediaPlayer().getCurrentPosition());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 50);
    }

    private void cancelTimer() {
        if (mTimer != null) {
            mTimer.cancel();
        }
        mTimer = null;
    }

    private void startVideo() {
        mVideoInput.start();
        startTimer();
    }

    private void pauseVideo() {
        mVideoInput.pause();
        cancelTimer();
    }

    private void releaseVideo() {
        mVideoInput.release();
        cancelTimer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        pauseVideo();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseVideo();
    }
}

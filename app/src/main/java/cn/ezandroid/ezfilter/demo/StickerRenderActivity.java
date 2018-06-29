package cn.ezandroid.ezfilter.demo;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import cn.ezandroid.ezfilter.EZFilter;
import cn.ezandroid.ezfilter.core.RenderPipeline;
import cn.ezandroid.ezfilter.core.environment.TextureFitView;
import cn.ezandroid.ezfilter.demo.render.GraffitiStickerRender;
import cn.ezandroid.ezfilter.video.VideoInput;
import cn.ezandroid.ezfilter.video.player.IMediaPlayer;

/**
 * StickerRenderActivity
 *
 * @author like
 * @date 2018-05-30
 */
public class StickerRenderActivity extends BaseActivity {

    private TextureFitView mRenderView;
    private TextView mPlayButton;
    private SeekBar mSeekBar;

    private Timer mTimer;

    private RenderPipeline mRenderPipeline;
    private VideoInput mVideoInput;

    private boolean mTouchingSeekBar;
    private boolean mTouchingTextureView;

    private GraffitiStickerRender mStickerRender;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sticker_render);

        initView();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initView() {
        mRenderView = $(R.id.render_view);
        mRenderView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mTouchingTextureView = true;
                        startVideo();
                        mStickerRender = new GraffitiStickerRender(StickerRenderActivity.this, new GraffitiStickerRender.IStickerTimeController() {
                            @Override
                            public float getCurrentTime() {
                                return mVideoInput.getMediaPlayer().getCurrentPosition() / 1000f;
                            }
                        });
                        mStickerRender.start();
                        mStickerRender.setPosition(Math.round(event.getX() * mStickerRender.getWidth() * 1f / mRenderView.getWidth()),
                                Math.round(event.getY() * mStickerRender.getHeight() * 1f / mRenderView.getHeight()));
                        mRenderPipeline.addFilterRender(mStickerRender);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        mStickerRender.setPosition(Math.round(event.getX() * mStickerRender.getWidth() * 1f / mRenderView.getWidth()),
                                Math.round(event.getY() * mStickerRender.getHeight() * 1f / mRenderView.getHeight()));
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        mTouchingTextureView = false;
                        pauseVideo();
                        mStickerRender.pause();
                        break;
                }
                return true;
            }
        });

        mPlayButton = $(R.id.play);
        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mVideoInput.isPlaying()) {
                    pauseVideo();
                } else {
                    startVideo();
                }
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
                        .setPreparedListener(new IMediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(IMediaPlayer var1) {
                                mSeekBar.setMax(var1.getDuration());
                                mVideoInput.seekTo(1);
                                pauseVideo();
                            }
                        })
                        .setCompletionListener(new IMediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(IMediaPlayer var1) {
                                if (!mTouchingSeekBar && !mTouchingTextureView) {
                                    mVideoInput.seekTo(0);
                                    startVideo();
                                }
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

package cn.ezandroid.ezfilter;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import cn.ezandroid.lib.ezfilter.EZFilter;
import cn.ezandroid.lib.ezfilter.core.GLRender;
import cn.ezandroid.lib.ezfilter.core.RenderPipeline;
import cn.ezandroid.lib.ezfilter.core.environment.SurfaceFitView;
import cn.ezandroid.lib.ezfilter.core.output.BitmapOutput;
import cn.ezandroid.ezfilter.render.BWRender;
import cn.ezandroid.lib.ezfilter.media.record.ISupportRecord;
import cn.ezandroid.lib.ezfilter.video.VideoInput;
import cn.ezandroid.lib.ezfilter.video.player.IMediaPlayer;

/**
 * VideoFilterActivity
 *
 * @author like
 * @date 2017-09-16
 */
public class VideoFilterActivity extends BaseActivity {

    private SurfaceFitView mRenderView;
    private ImageView mPreviewImage;
    private Button mRecordButton;

    private Uri uri1;
    private Uri uri2;

    private Uri mCurrentUri;

    private RenderPipeline mRenderPipeline;

    private ISupportRecord mSupportRecord;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_filter);
        mRenderView = $(R.id.render_view);
        mPreviewImage = $(R.id.preview_image);
        mRecordButton = $(R.id.record);

        uri1 = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.test3);
        uri2 = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.test2);

        changeVideo();

        $(R.id.change_video).setOnClickListener(view -> changeVideo());

        $(R.id.capture).setOnClickListener(view -> mRenderPipeline.output(bitmap -> runOnUiThread(() -> mPreviewImage.setImageBitmap(bitmap)), true));

        mRecordButton.setOnClickListener(v -> {
            if (mSupportRecord != null) {
                if (mSupportRecord.isRecording()) {
                    stopRecording();
                } else {
                    startRecording();
                }
            }
        });
    }

    private void startRecording() {
        mRecordButton.setText("停止");
        if (mSupportRecord != null) {
            mSupportRecord.startRecording();
        }
    }

    private void stopRecording() {
        mRecordButton.setText("录制");
        if (mSupportRecord != null) {
            mSupportRecord.stopRecording();
        }
    }

    private void changeVideo() {
        if (mCurrentUri == uri1) {
            mCurrentUri = uri2;
        } else {
            mCurrentUri = uri1;
        }

        new Thread() {
            public void run() {
                mRenderPipeline = EZFilter.input(mCurrentUri)
                        .setLoop(false)
                        .addFilter(new BWRender(VideoFilterActivity.this))
                        .enableRecord("/sdcard/recordVideo.mp4", true, false)
                        .setPreparedListener(var1 -> Log.e("VideoFilterActivity", "onPrepared"))
                        .setCompletionListener(var1 -> Log.e("VideoFilterActivity", "onCompletion"))
                        .into(mRenderView);

                for (GLRender render : mRenderPipeline.getEndPointRenders()) {
                    if (render instanceof ISupportRecord) {
                        mSupportRecord = (ISupportRecord) render;
                    }
                }
            }
        }.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mRenderPipeline != null) {
            ((VideoInput) mRenderPipeline.getStartPointRender()).start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mRenderPipeline != null) {
            ((VideoInput) mRenderPipeline.getStartPointRender()).pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

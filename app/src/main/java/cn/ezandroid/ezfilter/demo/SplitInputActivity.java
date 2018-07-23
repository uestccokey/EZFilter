package cn.ezandroid.ezfilter.demo;

import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import cn.ezandroid.ezfilter.EZFilter;
import cn.ezandroid.ezfilter.core.GLRender;
import cn.ezandroid.ezfilter.core.RenderPipeline;
import cn.ezandroid.ezfilter.core.environment.SurfaceFitView;
import cn.ezandroid.ezfilter.core.util.L;
import cn.ezandroid.ezfilter.demo.render.HorizontalSplitInput;
import cn.ezandroid.ezfilter.demo.render.SnowStickerRender;
import cn.ezandroid.ezfilter.extra.CropRender;
import cn.ezandroid.ezfilter.media.record.ISupportRecord;
import cn.ezandroid.ezfilter.split.SplitInput;
import cn.ezandroid.ezfilter.video.player.IMediaPlayer;

/**
 * SplitInputActivity
 *
 * @author like
 * @date 2018-07-18
 */
public class SplitInputActivity extends BaseActivity {

    private SurfaceFitView mRenderView;

    private RenderPipeline mRenderPipeline;

    private SplitInput mSplitInput;
    private ISupportRecord mSupportRecord;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_split_input);

        mRenderView = $(R.id.render_view);

        $(R.id.record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        loadVideo(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.test4));
    }

    private void loadVideo(Uri uri) {
        L.LOG_RENDER_DRAW = true;
        L.LOG_RENDER_DESTROY = true;
        EZFilter.Builder videoBuilder = EZFilter.input(uri)
                .setLoop(false)
                .enableRecord("/sdcard/recordSplit.mp4", true, true)
                .setPreparedListener(new IMediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(IMediaPlayer var1) {
                        Log.e("SplitInputActivity", "onPrepared");
                    }
                })
                .setCompletionListener(new IMediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(IMediaPlayer var1) {
                        Log.e("SplitInputActivity", "onCompletion");
                    }
                });

        if (mSplitInput == null) {
            CropRender leftCropRender = new CropRender();
            leftCropRender.setCropRegion(new RectF(0, 0, 0.5f, 1));
            leftCropRender.setRenderSize(360, 640);
            CropRender rightCropRender = new CropRender();
            rightCropRender.setCropRegion(new RectF(0.5f, 0, 1, 1));
            rightCropRender.setRenderSize(360, 640);
            List<CropRender> cropRenders = new ArrayList<>();
            cropRenders.add(leftCropRender);
            cropRenders.add(rightCropRender);
            mSplitInput = new HorizontalSplitInput(cropRenders);

            mRenderPipeline = EZFilter.input(videoBuilder, mSplitInput)
                    .enableRecord("/sdcard/recordSplit.mp4", true, true)
                    .into(mRenderView);

            mSplitInput.getRenderPipelines().get(0).addFilterRender(new SnowStickerRender(this));

            for (GLRender render : mRenderPipeline.getEndPointRenders()) {
                if (render instanceof ISupportRecord) {
                    mSupportRecord = (ISupportRecord) render;
                }
            }
        } else {
            mSplitInput.setRootRender(videoBuilder.getStartPointRender(mRenderView));
        }
    }
}

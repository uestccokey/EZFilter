package cn.ezandroid.ezfilter.demo;

import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import cn.ezandroid.ezfilter.EZFilter;
import cn.ezandroid.ezfilter.core.GLRender;
import cn.ezandroid.ezfilter.core.RenderPipeline;
import cn.ezandroid.ezfilter.core.environment.SurfaceFitView;
import cn.ezandroid.ezfilter.core.util.L;
import cn.ezandroid.ezfilter.demo.render.TwoSplitInput;
import cn.ezandroid.ezfilter.demo.render.WobbleRender;
import cn.ezandroid.ezfilter.extra.CropRender;
import cn.ezandroid.ezfilter.media.record.ISupportRecord;
import cn.ezandroid.ezfilter.split.SplitInput;
import cn.ezandroid.ezfilter.video.player.IMediaPlayer;

/**
 * SplitFilterActivity
 *
 * @author like
 * @date 2018-07-18
 */
public class SplitFilterActivity extends BaseActivity {

    private SurfaceFitView mRenderView;

    private RenderPipeline mRenderPipeline;

    private ISupportRecord mSupportRecord;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_split_filter);

        mRenderView = $(R.id.render_view);

        mRenderPipeline = EZFilter.input(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.test4))
                .setLoop(false)
                .enableRecord("/sdcard/recordSplit.mp4", true, true)
                .setPreparedListener(new IMediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(IMediaPlayer var1) {
                        Log.e("SplitFilterActivity", "onPrepared");
                    }
                })
                .setCompletionListener(new IMediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(IMediaPlayer var1) {
                        Log.e("SplitFilterActivity", "onCompletion");
                    }
                })
                .into(mRenderView);

        EZFilter.Builder videoBuilder = EZFilter.input(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.test4))
                .setLoop(false)
                .enableRecord("/sdcard/recordSplit.mp4", true, true)
                .setPreparedListener(new IMediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(IMediaPlayer var1) {
                        Log.e("SplitFilterActivity", "onPrepared");
                    }
                })
                .setCompletionListener(new IMediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(IMediaPlayer var1) {
                        Log.e("SplitFilterActivity", "onCompletion");
                    }
                });

        CropRender leftCropRender = new CropRender();
        leftCropRender.setCropRegion(new RectF(0, 0, 0.5f, 1));
        CropRender rightCropRender = new CropRender();
        rightCropRender.setCropRegion(new RectF(0.5f, 0, 1, 1));
        List<CropRender> cropRenders = new ArrayList<>();
        cropRenders.add(leftCropRender);
        cropRenders.add(rightCropRender);
        SplitInput splitInput = new TwoSplitInput(cropRenders);
        mRenderPipeline = EZFilter.input(videoBuilder, splitInput)
                .into(mRenderView);

        splitInput.getRenderPipelines().get(0).addFilterRender(new WobbleRender());

        L.LOG_RENDER_DRAW = true;

        for (GLRender render : mRenderPipeline.getEndPointRenders()) {
            if (render instanceof ISupportRecord) {
                mSupportRecord = (ISupportRecord) render;
            }
        }
    }
}

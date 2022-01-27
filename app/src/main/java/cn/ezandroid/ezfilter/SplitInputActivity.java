package cn.ezandroid.ezfilter;

import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import cn.ezandroid.lib.ezfilter.EZFilter;
import cn.ezandroid.lib.ezfilter.core.GLRender;
import cn.ezandroid.lib.ezfilter.core.RenderPipeline;
import cn.ezandroid.lib.ezfilter.core.environment.SurfaceFitView;
import cn.ezandroid.lib.ezfilter.core.util.L;
import cn.ezandroid.ezfilter.render.HorizontalSplitInput;
import cn.ezandroid.ezfilter.render.SnowStickerRender;
import cn.ezandroid.lib.ezfilter.extra.CropRender;
import cn.ezandroid.lib.ezfilter.media.record.ISupportRecord;
import cn.ezandroid.lib.ezfilter.split.SplitInput;

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

        $(R.id.record).setOnClickListener(v -> {
        });

        loadVideo(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.test4));
    }

    private void loadVideo(Uri uri) {
        L.LOG_RENDER_DRAW = true;
        L.LOG_RENDER_DESTROY = true;
        EZFilter.Builder videoBuilder = EZFilter.input(uri)
                .setLoop(false)
                .enableRecord("/sdcard/recordSplit.mp4", true, true)
                .setPreparedListener(var1 -> Log.e("SplitInputActivity", "onPrepared"))
                .setCompletionListener(var1 -> Log.e("SplitInputActivity", "onCompletion"));

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

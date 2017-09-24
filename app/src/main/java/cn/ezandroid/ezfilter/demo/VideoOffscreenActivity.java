package cn.ezandroid.ezfilter.demo;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.PicassoEngine;

import java.util.List;

import cn.ezandroid.ezfilter.EZFilter;
import cn.ezandroid.ezfilter.demo.render.BWRender;
import cn.ezandroid.ezfilter.demo.render.WobbleRender;
import cn.ezandroid.ezfilter.environment.SurfaceRenderView;

/**
 * VideoOffscreenActivity
 *
 * @author like
 * @date 2017-09-24
 */
public class VideoOffscreenActivity extends BaseActivity {

    private static final int REQUEST_CODE_CHOOSE = 1;

    private SurfaceRenderView mRenderView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_offscreen);
        mRenderView = $(R.id.render_view);

        $(R.id.choose_video).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Matisse.from(VideoOffscreenActivity.this)
                        .choose(MimeType.of(MimeType.MP4), false)
                        .showSingleMediaType(true)
                        .maxSelectable(1)
                        .countable(false)
                        .gridExpectedSize(getResources().getDimensionPixelSize(R.dimen.grid_expected_size))
                        .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                        .thumbnailScale(0.85f)
                        .imageEngine(new PicassoEngine())
                        .forResult(REQUEST_CODE_CHOOSE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CHOOSE && resultCode == RESULT_OK) {
            final List<String> paths = Matisse.obtainPathResult(data);
            if (!paths.isEmpty()) {
                new Thread() {
                    public void run() {
                        final String output = "/sdcard/render.mp4";
                        // save是耗时操作，需要在异步线程调用
                        EZFilter.setVideo(Uri.parse(paths.get(0)))
                                .addFilter(new BWRender(VideoOffscreenActivity.this))
                                .addFilter(new WobbleRender())
                                .save(output);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                EZFilter.setVideo(Uri.parse(output))
                                        .setVideoLoop(true)
                                        .into(mRenderView);
                            }
                        });
                    }
                }.start();
            }
        }
    }
}

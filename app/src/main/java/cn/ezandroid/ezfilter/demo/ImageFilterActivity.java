package cn.ezandroid.ezfilter.demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import cn.ezandroid.ezfilter.EZFilter;
import cn.ezandroid.ezfilter.core.GLRender;
import cn.ezandroid.ezfilter.core.RenderPipeline;
import cn.ezandroid.ezfilter.core.environment.GLTextureView;
import cn.ezandroid.ezfilter.core.environment.TextureFitView;
import cn.ezandroid.ezfilter.core.output.BitmapOutput;
import cn.ezandroid.ezfilter.demo.render.LookupRender;
import cn.ezandroid.ezfilter.demo.render.WobbleRender;
import cn.ezandroid.ezfilter.media.record.ISupportRecord;

/**
 * ImageFilterActivity
 *
 * @author like
 * @date 2017-09-16
 */
public class ImageFilterActivity extends BaseActivity {

    private TextureFitView mRenderView;
    private ImageView mPreviewImage;
    private Button mRecordButton;

    private Bitmap mBitmap1;
    private Bitmap mBitmap2;

    private Bitmap mCurrentBitmap;

    private RenderPipeline mRenderPipeline;

    private ISupportRecord mSupportRecord;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_filter);
        mRenderView = $(R.id.render_view);
        mRenderView.setRenderMode(GLTextureView.RENDERMODE_CONTINUOUSLY);
        mPreviewImage = $(R.id.preview_image);

        mRecordButton = $(R.id.record);

        mBitmap1 = BitmapFactory.decodeResource(getResources(), R.drawable.preview);
        mBitmap2 = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

        changeBitmap();

        $(R.id.change_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeBitmap();
            }
        });

        $(R.id.capture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 截图方式一：不需要渲染在到View上便可以滤镜处理输出图像，图像默认宽高为输入图像的宽高
//                Bitmap bitmap = EZFilter.input(mCurrentBitmap)
//                        .addFilter(new LookupRender(ImageFilterActivity.this, R.drawable.langman))
//                        .output();
//                mPreviewImage.setImageBitmap(bitmap);

                // 截图方式二：需要渲染到View上后才能进行截图，图像默认宽高为View显示的宽高
                mRenderPipeline.output(new BitmapOutput.BitmapOutputCallback() {
                    @Override
                    public void bitmapOutput(final Bitmap bitmap) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mPreviewImage.setImageBitmap(bitmap);
                            }
                        });
                    }
                }, true);
                mRenderView.requestRender();
            }
        });

        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSupportRecord != null) {
                    if (mSupportRecord.isRecording()) {
                        stopRecording();
                    } else {
                        startRecording();
                    }
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

    private void changeBitmap() {
        if (mCurrentBitmap == mBitmap1) {
            mCurrentBitmap = mBitmap2;
        } else {
            mCurrentBitmap = mBitmap1;
        }

        mRenderPipeline = EZFilter.input(mCurrentBitmap)
                .addFilter(new LookupRender(ImageFilterActivity.this, R.drawable.langman))
                .addFilter(new WobbleRender())
                .enableRecord("/sdcard/recordBitmap.mp4", true, false)
                .into(mRenderView);

        for (GLRender render : mRenderPipeline.getEndPointRenders()) {
            if (render instanceof ISupportRecord) {
                mSupportRecord = (ISupportRecord) render;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

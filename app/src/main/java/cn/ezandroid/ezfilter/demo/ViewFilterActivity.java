package cn.ezandroid.ezfilter.demo;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;

import cn.ezandroid.ezfilter.EZFilter;
import cn.ezandroid.ezfilter.core.GLRender;
import cn.ezandroid.ezfilter.core.RenderPipeline;
import cn.ezandroid.ezfilter.core.environment.GLTextureView;
import cn.ezandroid.ezfilter.core.environment.TextureFitView;
import cn.ezandroid.ezfilter.core.output.BitmapOutput;
import cn.ezandroid.ezfilter.demo.render.WobbleRender;
import cn.ezandroid.ezfilter.media.record.ISupportRecord;
import cn.ezandroid.ezfilter.view.glview.GLLinearLayout;

/**
 * ViewFilterActivity
 *
 * @author like
 * @date 2017-09-20
 */
public class ViewFilterActivity extends BaseActivity {

    private TextureFitView mRenderView;
    private ImageView mPreviewImage;
    private GLLinearLayout mLinearLayout;
    private WebView mWebView;
    private Button mRecordButton;

    private RenderPipeline mRenderPipeline;

    private ISupportRecord mSupportRecord;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_filter);
        mRenderView = $(R.id.render_view);
        mRenderView.setRenderMode(GLTextureView.RENDERMODE_CONTINUOUSLY);
        mPreviewImage = $(R.id.preview_image);
        mLinearLayout = $(R.id.gl_layout);
        mWebView = $(R.id.web_view);

        mRecordButton = $(R.id.record);

        mWebView.setWebViewClient(new WebViewClient());
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.loadUrl("https://github.com/uestccokey/EZFilter");

        // 为了确保mLinearLayout已经初始化完成，宽高不为0
        mLinearLayout.post(new Runnable() {
            @Override
            public void run() {
                mRenderPipeline = EZFilter.input(mLinearLayout)
                        .addFilter(new WobbleRender())
                        .enableRecord("/sdcard/recordView.mp4", true, false)
                        .into(mRenderView);

                for (GLRender render : mRenderPipeline.getEndPointRenders()) {
                    if (render instanceof ISupportRecord) {
                        mSupportRecord = (ISupportRecord) render;
                    }
                }
            }
        });

        $(R.id.capture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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

    @Override
    protected void onResume() {
        super.onResume();
        mWebView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mWebView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

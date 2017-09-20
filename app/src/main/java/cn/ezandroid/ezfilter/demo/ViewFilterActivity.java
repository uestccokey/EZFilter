package cn.ezandroid.ezfilter.demo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

import cn.ezandroid.ezfilter.EZFilter;
import cn.ezandroid.ezfilter.demo.render.BWRender;
import cn.ezandroid.ezfilter.environment.TextureRenderView;
import cn.ezandroid.ezfilter.view.GLLinearLayout;

/**
 * ViewFilterActivity
 *
 * @author like
 * @date 2017-09-20
 */
public class ViewFilterActivity extends BaseActivity {

    private TextureRenderView mRenderView;
    private ImageView mPreviewImage;
    private GLLinearLayout mLinearLayout;
    private WebView mWebView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_filter);
        mRenderView = $(R.id.render_view);
        mPreviewImage = $(R.id.preview_image);
        mLinearLayout = $(R.id.gl_layout);
        mWebView = $(R.id.web_view);

        mWebView.setWebViewClient(new WebViewClient());
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.loadUrl("https://github.com/uestccokey/EZFilter");

        // 为了确保mLinearLayout已经初始化完成，宽高不为0
        mLinearLayout.post(new Runnable() {
            @Override
            public void run() {
                new EZFilter.ViewBuilder()
                        .setView(mLinearLayout)
                        .addFilter(new BWRender(ViewFilterActivity.this))
                        .into(mRenderView);
            }
        });
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

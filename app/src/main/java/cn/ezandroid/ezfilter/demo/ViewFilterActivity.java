package cn.ezandroid.ezfilter.demo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

import cn.ezandroid.ezfilter.EZFilter;
import cn.ezandroid.ezfilter.demo.render.BWRender;
import cn.ezandroid.ezfilter.view.TextureRenderView;

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
        mWebView.loadUrl("http://www.golem.de");

        mLinearLayout.setRender(mRenderView);
        new EZFilter.ViewBuilder()
                .setView(mLinearLayout)
                .addFilter(new BWRender(this))
                .into(mRenderView);
    }
}

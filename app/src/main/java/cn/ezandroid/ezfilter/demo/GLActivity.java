package cn.ezandroid.ezfilter.demo;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.TextureView;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import cn.ezandroid.ezfilter.core.RenderPipeline;
import cn.ezandroid.ezfilter.environment.GLEnvironment;
import cn.ezandroid.ezfilter.io.input.ViewInput;
import cn.ezandroid.ezfilter.view.GLLinearLayout;

/**
 * 演示GLEnvironment的用法，用来配合SurfaceView和TextureView
 *
 * @author like
 * @date 2017-09-20
 */
public class GLActivity extends BaseActivity {

    private TextureView mRenderView;
    private GLLinearLayout mGLLinearLayout;
    private WebView mWebView;

    private GLEnvironment mGLEnvironment;
    private SurfaceTexture mTexture;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gl);
        mRenderView = $(R.id.render_view);
        mGLLinearLayout = $(R.id.gl_layout);
        mWebView = $(R.id.web_view);

        mWebView.setWebViewClient(new WebViewClient());
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.loadUrl("http://www.golem.de");

        mGLEnvironment = new GLEnvironment();
        mGLEnvironment.setEGLWindowSurfaceFactory(new GLEnvironment.EGLWindowSurfaceFactory() {
            @Override
            public EGLSurface createSurface(EGL10 egl, EGLDisplay display, EGLConfig config, Object nativeWindow) {
                return egl.eglCreateWindowSurface(display, config, mTexture, null);
            }

            @Override
            public void destroySurface(EGL10 egl, EGLDisplay display, EGLSurface surface) {
                egl.eglDestroySurface(display, surface);
            }
        });
        RenderPipeline pipeline = new RenderPipeline();
//        BitmapInput bitmapInput = new BitmapInput(BitmapFactory.decodeResource(getResources(), R.drawable.preview));
//        pipeline.setStartPointRender(bitmapInput);
//        VideoInput videoInput = new VideoInput(this, mGLEnvironment,
//                Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.test2));
//        videoInput.start();
        mGLLinearLayout.setGLEnvironment(mGLEnvironment);
        ViewInput viewInput = new ViewInput(mGLLinearLayout);
        pipeline.setStartPointRender(viewInput);
        pipeline.startRender();
        mGLEnvironment.setRenderer(pipeline);
        mRenderView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
                mTexture = surfaceTexture;
                mGLEnvironment.surfaceCreated(null);
                mGLEnvironment.surfaceChanged(null, 0, i, i1);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
                mGLEnvironment.surfaceChanged(null, 0, i, i1);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                mTexture = null;
                mGLEnvironment.surfaceDestroyed(null);
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

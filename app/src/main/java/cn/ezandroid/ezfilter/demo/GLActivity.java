package cn.ezandroid.ezfilter.demo;

import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.TextureView;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import cn.ezandroid.ezfilter.core.RenderPipeline;
import cn.ezandroid.ezfilter.io.input.VideoInput;
import cn.ezandroid.ezfilter.view.GLEnvironment;

/**
 * GLActivity
 *
 * @author like
 * @date 2017-09-20
 */
public class GLActivity extends BaseActivity {

    private TextureView mRenderView;

    private GLEnvironment mGLEnvironment;
    private SurfaceTexture mTexture;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gl);
        mRenderView = $(R.id.render_view);

        mGLEnvironment = new GLEnvironment();
        mGLEnvironment.setEGLConfigChooser(8, 8, 8, 8, 16, 8);
        mGLEnvironment.setPreserveEGLContextOnPause(true);
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
        mGLEnvironment.setEGLContextClientVersion(2);
        RenderPipeline pipeline = new RenderPipeline();
//        BitmapInput bitmapInput = new BitmapInput(BitmapFactory.decodeResource(getResources(), R.drawable.preview));
//        pipeline.setStartPointRender(bitmapInput);
        VideoInput videoInput = new VideoInput(this, mGLEnvironment,
                Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.test2));
        videoInput.start();
        pipeline.setStartPointRender(videoInput);
        pipeline.startRender();
        mGLEnvironment.setRenderer(pipeline);
        mGLEnvironment.setRenderMode(GLEnvironment.RENDERMODE_WHEN_DIRTY);
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

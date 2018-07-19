package cn.ezandroid.ezfilter.video;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;

import java.io.IOException;

import javax.microedition.khronos.opengles.GL10;

import cn.ezandroid.ezfilter.core.FBORender;
import cn.ezandroid.ezfilter.core.environment.IGLEnvironment;
import cn.ezandroid.ezfilter.video.player.DefaultMediaPlayer;
import cn.ezandroid.ezfilter.video.player.IMediaPlayer;

/**
 * 视频输入
 *
 * @author like
 * @date 2017-09-16
 */
public class VideoInput extends FBORender implements SurfaceTexture.OnFrameAvailableListener {

    private static final String UNIFORM_CAM_MATRIX = "u_Matrix";

    private IMediaPlayer mPlayer;
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;

    private Uri mVideoUri;

    private int mMatrixHandle;
    private float[] mMatrix = new float[16];

    private IGLEnvironment mRender;

    private boolean mStartWhenReady = true;
    private boolean mReady;

    private float mVideoVolumeLeft = 1.0f;
    private float mVideoVolumeRight = 1.0f;

    private boolean mIsLoop;

    private IMediaPlayer.OnPreparedListener mPreparedListener;
    private IMediaPlayer.OnCompletionListener mCompletionListener;
    private IMediaPlayer.OnErrorListener mErrorListener;

    public VideoInput(IGLEnvironment render) {
        super();
        this.mRender = render;
        initShader();
    }

    public VideoInput(Context context, IGLEnvironment render, Uri uri) {
        super();
        this.mRender = render;
        initShader();
        try {
            setVideoUri(context, uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public VideoInput(Context context, IGLEnvironment render, Uri uri, IMediaPlayer player) {
        super();
        this.mRender = render;
        initShader();
        try {
            setVideoUri(context, uri, player);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initShader() {
        setVertexShader("uniform mat4 " + UNIFORM_CAM_MATRIX + ";\n"
                + "attribute vec4 " + ATTRIBUTE_POSITION + ";\n"
                + "attribute vec2 " + ATTRIBUTE_TEXTURE_COORD + ";\n"
                + "varying vec2 " + VARYING_TEXTURE_COORD + ";\n"
                + "void main() {\n"
                + "   vec4 texPos = " + UNIFORM_CAM_MATRIX + " * vec4(" + ATTRIBUTE_TEXTURE_COORD + "," + " 1, 1);\n"
                + "   " + VARYING_TEXTURE_COORD + " = texPos.xy;\n"
                + "   gl_Position = " + ATTRIBUTE_POSITION + ";\n"
                + "}\n");
        setFragmentShader("#extension GL_OES_EGL_image_external : require\n"
                + "precision mediump float;\n"
                + "uniform samplerExternalOES " + UNIFORM_TEXTURE_0 + ";\n"
                + "varying vec2 " + VARYING_TEXTURE_COORD + ";\n"
                + "void main() {\n"
                + "   gl_FragColor = texture2D(" + UNIFORM_TEXTURE_0 + ", " + VARYING_TEXTURE_COORD + ");\n"
                + "}\n");
    }

    public void setOnPreparedListener(IMediaPlayer.OnPreparedListener listener) {
        mPreparedListener = listener;
    }

    public void setOnCompletionListener(IMediaPlayer.OnCompletionListener listener) {
        mCompletionListener = listener;
    }

    public void setOnErrorListener(IMediaPlayer.OnErrorListener listener) {
        mErrorListener = listener;
    }

    public void setVideoUri(Context context, Uri uri, IMediaPlayer player) throws IOException {
        if (uri != null) {
            release();
            mVideoUri = uri;
            mPlayer = player;
            mPlayer.setDataSource(context, mVideoUri);
            mPlayer.setLooping(mIsLoop);
            mPlayer.setVolume(mVideoVolumeLeft, mVideoVolumeRight);
            mPlayer.setOnPreparedListener(var1 -> {
                mReady = true;
                if (mStartWhenReady) {
                    var1.start();
                }

                setRenderSize(mPlayer.getVideoWidth(), mPlayer.getVideoHeight());

                if (mPreparedListener != null) {
                    mPreparedListener.onPrepared(var1);
                }
            });
            mPlayer.setOnCompletionListener(var1 -> {
                if (mCompletionListener != null) {
                    mCompletionListener.onCompletion(var1);
                }
            });
            mPlayer.setOnErrorListener((mp, what, extra) ->
                    mErrorListener == null || mErrorListener.onError(mp, what, extra)
            );
            reInit();
            mRender.requestRender();
        }
    }

    public void setVideoUri(Context context, Uri uri) throws IOException {
        setVideoUri(context, uri, new DefaultMediaPlayer());
    }

    public IMediaPlayer getMediaPlayer() {
        return mPlayer;
    }

    public Uri getVideoUri() {
        return mVideoUri;
    }

    public void setStartWhenReady(boolean startWhenReady) {
        mStartWhenReady = startWhenReady;
    }

    public void setLoop(boolean isLoop) {
        mIsLoop = isLoop;
        if (mPlayer != null) {
            mPlayer.setLooping(mIsLoop);
        }
    }

    public void setVolume(float left, float right) {
        mVideoVolumeLeft = left;
        mVideoVolumeRight = right;
        if (mPlayer != null) {
            mPlayer.setVolume(mVideoVolumeLeft, mVideoVolumeRight);
        }
    }

    @Override
    protected void drawFrame() {
        try {
            mSurfaceTexture.updateTexImage();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.drawFrame();
    }

    @Override
    protected void initShaderHandles() {
        super.initShaderHandles();
        mMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, UNIFORM_CAM_MATRIX);
    }

    @Override
    protected void initGLContext() {
        super.initGLContext();
        mReady = false;

        if (mTextureIn != 0) {
            int[] tex = new int[1];
            tex[0] = mTextureIn;
            GLES20.glDeleteTextures(1, tex, 0);
            mTextureIn = 0;
        }
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        mTextureIn = textures[0];

        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        mSurfaceTexture = new SurfaceTexture(mTextureIn);
        mSurfaceTexture.setOnFrameAvailableListener(this);

        mSurface = new Surface(mSurfaceTexture);
        mPlayer.setSurface(mSurface);

        // 在主线程执行prepareAsync，因为某些Player的实现在异步线程调用prepareAsync时可能崩溃
        new Handler(Looper.getMainLooper()).post(() -> {
            // prepareAsync 不放在setVideoUri里是为了确保onPrepared中mPlayer.start()时已经设置了Surface，否则可能播放失败
            try {
                mPlayer.prepareAsync();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onFrameAvailable(SurfaceTexture arg0) {
        mRender.requestRender();
    }

    @Override
    protected void bindShaderValues() {
        super.bindShaderVertices();

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureIn);
        GLES20.glUniform1i(mTextureHandle, 0);

        mSurfaceTexture.getTransformMatrix(mMatrix);
        GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mMatrix, 0);
    }

    public boolean isPlaying() {
        return null != mPlayer && mPlayer.isPlaying();
    }

    public void start() {
        if (mReady && null != mPlayer) {
            mPlayer.start();
        } else {
            mStartWhenReady = true;
        }
    }

    public void pause() {
        if (null != mPlayer) {
            try {
                mPlayer.pause();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    public void seekTo(int time) {
        if (null != mPlayer) {
            try {
                mPlayer.seekTo(time);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        if (null != mPlayer) {
            try {
                mPlayer.stop();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    public void reset() {
        if (null != mPlayer) {
            try {
                mPlayer.reset();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    public void release() {
        mVideoUri = null;
        if (null != mPlayer) {
            mPlayer.release();
            mPlayer = null;
            mReady = false;
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        if (mTextureIn != 0) {
            int[] tex = new int[1];
            tex[0] = mTextureIn;
            GLES20.glDeleteTextures(1, tex, 0);
            mTextureIn = 0;
        }
        release();
    }
}

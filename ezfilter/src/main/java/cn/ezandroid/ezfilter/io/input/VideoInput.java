package cn.ezandroid.ezfilter.io.input;

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
import cn.ezandroid.ezfilter.io.player.IMediaPlayer;
import cn.ezandroid.ezfilter.io.player.SystemMediaPlayer;
import cn.ezandroid.ezfilter.view.IRender;

/**
 * VideoInput
 *
 * @author like
 * @date 2017-09-16
 */
public class VideoInput extends FBORender implements SurfaceTexture.OnFrameAvailableListener {

    private static final String UNIFORM_CAM_MATRIX = "u_Matrix";

    private IMediaPlayer mPlayer;
    private SurfaceTexture mSurfaceTexture;

    private Uri mVideoUri;

    private int mMatrixHandle;
    private float[] mMatrix = new float[16];

    private IRender mRender;

    private boolean mStartWhenReady;
    private boolean mReady;

    private float mVideoVolumeLeft = 1.0f;
    private float mVideoVolumeRight = 1.0f;

    private boolean mIsLoop;

    private OnPlayerPreparedListener mOnPlayerPreparedListener;

    public interface OnPlayerPreparedListener {

        void OnPlayerPrepared(IMediaPlayer player);
    }

    public VideoInput(IRender render) {
        super();
        this.mRender = render;
    }

    public VideoInput(Context context, IRender render, Uri uri) {
        super();
        this.mRender = render;
        try {
            setVideoUri(context, uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public VideoInput(Context context, IRender render, Uri uri, IMediaPlayer player) {
        super();
        this.mRender = render;
        try {
            setVideoUri(context, uri, player);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setOnPlayerPreparedListener(OnPlayerPreparedListener listener) {
        mOnPlayerPreparedListener = listener;
    }

    public void setVideoUri(Context context, Uri uri, IMediaPlayer player) throws IOException {
        if (uri != null) {
            release();
            mVideoUri = uri;
            mPlayer = player;
            mPlayer.setDataSource(context, mVideoUri);
            mPlayer.setVolume(mVideoVolumeLeft, mVideoVolumeRight);
            mPlayer.setLooping(mIsLoop);
            reInit();
        }
    }

    public void setVideoUri(Context context, Uri uri) throws IOException {
        setVideoUri(context, uri, new SystemMediaPlayer());
    }

    public IMediaPlayer getMediaPlayer() {
        return mPlayer;
    }

    public Uri getVideoUri() {
        return mVideoUri;
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
    protected String getFragmentShader() {
        return "#extension GL_OES_EGL_image_external : require\n"
                + "precision mediump float;\n"
                + "uniform samplerExternalOES " + UNIFORM_TEXTURE_0 + ";\n"
                + "varying vec2 " + VARYING_TEXTURE_COORD + ";\n"
                + "void main() {\n"
                + "   gl_FragColor = texture2D(" + UNIFORM_TEXTURE_0 + ", " + VARYING_TEXTURE_COORD +
                ");\n"
                + "}\n";
    }

    @Override
    protected String getVertexShader() {
        return "uniform mat4 " + UNIFORM_CAM_MATRIX + ";\n"
                + "attribute vec4 " + ATTRIBUTE_POSITION + ";\n"
                + "attribute vec2 " + ATTRIBUTE_TEXTURE_COORD + ";\n"
                + "varying vec2 " + VARYING_TEXTURE_COORD + ";\n"
                + "void main() {\n"
                + "   vec4 texPos = " + UNIFORM_CAM_MATRIX + " * vec4(" + ATTRIBUTE_TEXTURE_COORD + "," + " 1, 1);\n"
                + "   " + VARYING_TEXTURE_COORD + " = texPos.xy;\n"
                + "   gl_Position = " + ATTRIBUTE_POSITION + ";\n"
                + "}\n";
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

        Surface surface = new Surface(mSurfaceTexture);
        mPlayer.setSurface(surface);

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                // prepareAsync 不放在setVideoUri里是为了onPrepared中mPlayer.start()时已经设置了Surface，否则可能播放失败
                try {
                    mPlayer.prepareAsync();
                    mPlayer.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(IMediaPlayer var1) {
                            mReady = true;
                            if (mStartWhenReady) {
                                mPlayer.start();
                            }

                            if (mOnPlayerPreparedListener != null) {
                                mOnPlayerPreparedListener.OnPlayerPrepared(mPlayer);
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onFrameAvailable(SurfaceTexture arg0) {
        mRender.requestRender();
    }

    @Override
    protected void bindShaderValues() {
        mRenderVertices.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, 2, GLES20.GL_FLOAT, false,
                8, mRenderVertices);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        mTextureVertices[mCurrentRotation].position(0);
        GLES20.glVertexAttribPointer(mTextureCoordHandle, 2, GLES20.GL_FLOAT, false,
                8, mTextureVertices[mCurrentRotation]);
        GLES20.glEnableVertexAttribArray(mTextureCoordHandle);

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
        if (mTextureIn != 0) {
            int[] tex = new int[1];
            tex[0] = mTextureIn;
            GLES20.glDeleteTextures(1, tex, 0);
            mTextureIn = 0;
        }
        release();
    }
}

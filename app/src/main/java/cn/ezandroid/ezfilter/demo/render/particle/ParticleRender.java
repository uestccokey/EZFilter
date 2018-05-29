package cn.ezandroid.ezfilter.demo.render.particle;

import android.content.Context;
import android.graphics.Color;
import android.opengl.GLES20;

import org.jetbrains.annotations.NotNull;

import cn.ezandroid.ezfilter.core.FilterRender;
import cn.ezandroid.ezfilter.core.util.BitmapUtil;
import cn.ezandroid.ezfilter.core.util.Path;
import cn.ezandroid.ezfilter.demo.R;
import cn.ezandroid.ezfilter.demo.render.particle.util.Geometry;

/**
 * 粒子效果滤镜
 *
 * @author like
 * @date 2018-05-15
 */
public class ParticleRender extends FilterRender {

    private ParticleShaderProgram mParticleShaderProgram;
    private ParticleSystem mParticleSystem;
    private ParticleShooter mParticleShooter;

    private int mTexture;

    private Context mContext;

    private boolean mIsPause = true;

    public interface IParticleTimeController {

        float getCurrentTime();
    }

    private IParticleTimeController mTimeController;

    public ParticleRender(Context context, @NotNull IParticleTimeController controller) {
        mContext = context;
        mTimeController = controller;
    }

    @Override
    protected void initGLContext() {
        super.initGLContext();

        mParticleSystem = new ParticleSystem(10000);

        mParticleShaderProgram = new ParticleShaderProgram(mContext);

        mParticleShooter = new ParticleShooter(new Geometry.Point(0, 0, 0), 10,
                new Geometry.Vector(0.0f, 0.2f, 0.0f), 360, 1f,
                16, 4, 32, 4,
                0, 10, 360, 10,
                Color.argb(255, 255, 0, 0), Color.argb(0, 0, 0, 0),
                Color.argb(255, 0, 0, 255), Color.argb(0, 0, 0, 0),
                1f,
                2);

        mTexture = BitmapUtil.bindBitmap(BitmapUtil.loadBitmap(mContext, Path.DRAWABLE.wrap(String.valueOf(R.drawable.texture))));
    }

    public void setPosition(Geometry.Point position) {
        if (mParticleShooter != null) {
            mParticleShooter.setPosition(position);
        }
    }

    public void start() {
        mIsPause = false;
    }

    public void pause() {
        mIsPause = true;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        // 开启混合
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        mParticleShaderProgram.useProgram(); // 切换到粒子效果渲染程序

        if (!mIsPause) {
            mParticleShooter.addParticles(mParticleSystem, mTimeController.getCurrentTime());
        }

        mParticleShaderProgram.setUniforms(mTimeController.getCurrentTime(), mTexture, 2);

        mParticleSystem.bindData(mParticleShaderProgram);
        mParticleSystem.draw();

        GLES20.glDisable(GLES20.GL_BLEND);
    }
}

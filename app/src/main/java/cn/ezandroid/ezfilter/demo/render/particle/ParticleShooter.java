package cn.ezandroid.ezfilter.demo.render.particle;

import android.graphics.Color;

import java.util.Random;

import cn.ezandroid.ezfilter.demo.render.particle.util.Geometry;

import static android.opengl.Matrix.multiplyMV;
import static android.opengl.Matrix.setRotateEulerM;

/**
 * 粒子发射器
 */
public class ParticleShooter {

    // 粒子的初始位置
    private Geometry.Point mPosition;
    // 一次产生的粒子数量
    private int mBirthRate;

    // 粒子移动的方向向量(包括角度与速度)
    private float mDirectionVector[] = new float[4];
    // 粒子移动角度的方差
    private final float mAngleVariance;
    // 粒子移动速度的方差
    private final float mSpeedVariance;

    // 粒子起始的大小
    private final float mFromSize;
    // 粒子起始大小的方差
    private final float mFromSizeVariance;
    // 粒子结束的大小
    private final float mToSize;
    // 粒子结束大小的方差
    private final float mToSizeVariance;

    // 粒子起始的角度
    private final float mFromAngle;
    // 粒子起始角度的方差
    private final float mFromAngleVariance;
    // 粒子结束的角度
    private final float mToAngle;
    // 粒子结束角度的方差
    private final float mToAngleVariance;

    // 粒子起始的颜色
    private final int mFromColor;
    // 粒子起始颜色的方差
    private final int mFromColorVariance;
    // 粒子结束的颜色
    private final int mToColor;
    // 粒子结束颜色的方差
    private final int mToColorVariance;

    // 粒子持续的时间
    private final float mDuration;
    // 粒子纹理总数
    private final int mTextureCount;

    private final Random mRandom = new Random();

    private float mRotationMatrix[] = new float[16];
    private float mResultVector[] = new float[4];

    public ParticleShooter(Geometry.Point position, int birthRate,
                           Geometry.Vector direction, float angleVarianceInDegrees, float speedVariance,
                           float fromSize, float fromSizeVariance, float toSize, float toSizeVariance,
                           float fromAngle, float fromAngleVariance, float toAngle, float toAngleVariance,
                           int fromColor, int fromColorVariance, int toColor, int toColorVariance,
                           float duration, int textureCount) {
        this.mPosition = position;
        this.mBirthRate = birthRate;

        mDirectionVector[0] = direction.x;
        mDirectionVector[1] = direction.y;
        mDirectionVector[2] = direction.z;
        this.mAngleVariance = angleVarianceInDegrees;
        this.mSpeedVariance = speedVariance;

        this.mFromSize = fromSize;
        this.mFromSizeVariance = fromSizeVariance;
        this.mToSize = toSize;
        this.mToSizeVariance = toSizeVariance;

        this.mFromAngle = fromAngle;
        this.mFromAngleVariance = fromAngleVariance;
        this.mToAngle = toAngle;
        this.mToAngleVariance = toAngleVariance;

        this.mFromColor = fromColor;
        this.mFromColorVariance = fromColorVariance;
        this.mToColor = toColor;
        this.mToColorVariance = toColorVariance;

        this.mDuration = duration;
        this.mTextureCount = textureCount;
    }

    public void setPosition(Geometry.Point position) {
        this.mPosition = position;
    }

    private int getRandomColor(int originalColor, int variance) {
        int alpha = Color.alpha(originalColor) + Math.round((mRandom.nextFloat() - 0.5f) * Color.alpha(variance));
        if (alpha < 0) {
            alpha = 0;
        } else if (alpha > 255) {
            alpha = 255;
        }

        int red = Color.red(originalColor) + Math.round((mRandom.nextFloat() - 0.5f) * Color.red(variance));
        if (red < 0) {
            red = 0;
        } else if (red > 255) {
            red = 255;
        }

        int green = Color.green(originalColor) + Math.round((mRandom.nextFloat() - 0.5f) * Color.green(variance));
        if (green < 0) {
            green = 0;
        } else if (green > 255) {
            green = 255;
        }

        int blue = Color.blue(originalColor) + Math.round((mRandom.nextFloat() - 0.5f) * Color.blue(variance));
        if (blue < 0) {
            blue = 0;
        } else if (blue > 255) {
            blue = 255;
        }

        return Color.argb(alpha, red, green, blue);
    }

    /**
     * Adding the particles to the particular ParticleSystem instance.
     *
     * @param particleSystem - ParticleSystem instance.
     * @param currentTime    - particle creation time.
     */
    public void addParticles(ParticleSystem particleSystem, float currentTime) {
        for (int i = 0; i < mBirthRate; i++) {
            // Generate random rotation matrix and rotate particle speed vector
            setRotateEulerM(mRotationMatrix, 0,
                    (mRandom.nextFloat() - 0.5f) * mAngleVariance,
                    (mRandom.nextFloat() - 0.5f) * mAngleVariance,
                    (mRandom.nextFloat() - 0.5f) * mAngleVariance);

            multiplyMV(mResultVector, 0, mRotationMatrix, 0, mDirectionVector, 0);

            float speedAdjustment = 1f + (mRandom.nextFloat() - 0.5f) * mSpeedVariance;

            Geometry.Vector thisDirection = new Geometry.Vector(
                    mResultVector[0] * speedAdjustment,
                    mResultVector[1] * speedAdjustment,
                    mResultVector[2] * speedAdjustment);

            particleSystem.addParticle(currentTime,
                    mDuration,
                    mFromSize + (mRandom.nextFloat() - 0.5f) * mFromSizeVariance,
                    mToSize + (mRandom.nextFloat() - 0.5f) * mToSizeVariance,
                    mFromAngle + (mRandom.nextFloat() - 0.5f) * mFromAngleVariance,
                    mToAngle + (mRandom.nextFloat() - 0.5f) * mToAngleVariance,
                    mPosition,
                    thisDirection,
                    getRandomColor(mFromColor, mFromColorVariance),
                    getRandomColor(mToColor, mToColorVariance),
                    mRandom.nextInt(mTextureCount));
        }
    }
}

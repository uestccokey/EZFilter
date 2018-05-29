package cn.ezandroid.ezfilter.demo.render.particle;

import java.util.Random;

import cn.ezandroid.ezfilter.demo.render.particle.util.Geometry;

import static android.opengl.Matrix.multiplyMV;
import static android.opengl.Matrix.setRotateEulerM;

/**
 * 粒子发射器
 */
public class ParticleShooter {

    private final float mAngleVariance; // 粒子角度方差
    private final float mSpeedVariance; // 粒子速度方差
    private final float mSizeVariance; // 粒子大小方差
    private Geometry.Point mPosition; // 粒子发射位置
    private final int mColor; // 粒子颜色
    private final float mSize; // 粒子大小
    private final float mDuration;

    private final Random mRandom = new Random();

    private float mRotationMatrix[] = new float[16];
    private float mDirectionVector[] = new float[4];
    private float mResultVector[] = new float[4];

    /**
     * Create the ParticleShooter object.
     *
     * @param position               - position of the particle shooter.
     * @param direction              - shoot direction.
     * @param color                  - particle color
     * @param size                   - particle size
     * @param duration               - particle duration
     * @param angleVarianceInDegrees - spreading particles variance
     * @param speedVariance          - particle speed variance
     * @param sizeVariance           - particle size variance
     */
    public ParticleShooter(Geometry.Point position, Geometry.Vector direction, int color, float size, float duration,
                           float angleVarianceInDegrees, float speedVariance, float sizeVariance) {
        this.mPosition = position;

        this.mColor = color;
        this.mSize = size;
        this.mDuration = duration;

        this.mAngleVariance = angleVarianceInDegrees;
        this.mSpeedVariance = speedVariance;
        this.mSizeVariance = sizeVariance;

        mDirectionVector[0] = direction.x;
        mDirectionVector[1] = direction.y;
        mDirectionVector[2] = direction.z;
    }

    public void setPosition(Geometry.Point position) {
        this.mPosition = position;
    }

    /**
     * Adding the particles to the particular ParticleSystem instance.
     *
     * @param particleSystem - ParticleSystem instance.
     * @param currentTime    - particle creation time.
     * @param count          - how much particle should be added.
     */
    public void addParticles(ParticleSystem particleSystem, float currentTime, int count) {
        for (int i = 0; i < count; i++) {
            // Generate random rotation matrix and rotate particle speed vector
            setRotateEulerM(mRotationMatrix, 0,
                    (mRandom.nextFloat() - 0.5f) * mAngleVariance,
                    (mRandom.nextFloat() - 0.5f) * mAngleVariance,
                    (mRandom.nextFloat() - 0.5f) * mAngleVariance);

            multiplyMV(mResultVector, 0, mRotationMatrix, 0, mDirectionVector, 0);

            float speedAdjustment = 1f + mRandom.nextFloat() * mSpeedVariance;

            Geometry.Vector thisDirection = new Geometry.Vector(
                    mResultVector[0] * speedAdjustment,
                    mResultVector[1] * speedAdjustment,
                    mResultVector[2] * speedAdjustment);

            particleSystem.addParticle(mPosition, mColor, thisDirection, currentTime, mDuration, mSize + mRandom.nextFloat() * mSizeVariance);
        }
    }
}

package cn.ezandroid.ezfilter.demo.render.particle;

import android.graphics.Color;

import cn.ezandroid.ezfilter.demo.render.particle.util.Geometry;

import static android.opengl.GLES20.GL_POINTS;
import static android.opengl.GLES20.glDrawArrays;

/**
 * 粒子系统
 */
public class ParticleSystem {

    private static final int PARTICLE_BIRTH_TIME_COMPONENT_COUNT = 1;
    private static final int PARTICLE_DURATION_COMPONENT_COUNT = 1;
    private static final int PARTICLE_FROM_SIZE_COMPONENT_COUNT = 1;
    private static final int PARTICLE_TO_SIZE_COMPONENT_COUNT = 1;
    private static final int PARTICLE_FROM_ANGLE_COMPONENT_COUNT = 1;
    private static final int PARTICLE_TO_ANGLE_COMPONENT_COUNT = 1;
    private static final int PARTICLE_POSITION_COMPONENT_COUNT = 3; // [ x, y, z ]
    private static final int PARTICLE_DIRECTION_COMPONENT_COUNT = 3; // [ x2, y2, z2 ]
    private static final int PARTICLE_FROM_COLOR_COMPONENT_COUNT = 4; // [ a, r, g, b ]
    private static final int PARTICLE_TO_COLOR_COMPONENT_COUNT = 4; // [ a, r, g, b ]
    private static final int PARTICLE_TEXTURE_INDEX_COMPONENT_COUNT = 1;

    private static final int TOTAL_COMPONENT_COUNT = PARTICLE_BIRTH_TIME_COMPONENT_COUNT
            + PARTICLE_DURATION_COMPONENT_COUNT
            + PARTICLE_FROM_SIZE_COMPONENT_COUNT
            + PARTICLE_TO_SIZE_COMPONENT_COUNT
            + PARTICLE_FROM_ANGLE_COMPONENT_COUNT
            + PARTICLE_TO_ANGLE_COMPONENT_COUNT
            + PARTICLE_POSITION_COMPONENT_COUNT
            + PARTICLE_DIRECTION_COMPONENT_COUNT
            + PARTICLE_FROM_COLOR_COMPONENT_COUNT
            + PARTICLE_TO_COLOR_COMPONENT_COUNT
            + PARTICLE_TEXTURE_INDEX_COMPONENT_COUNT; // 每个粒子的Float字段总数

    private static int STRIDE = TOTAL_COMPONENT_COUNT * 4; // 每个粒子的字节总数 = Float字段总数 * Float字节数

    private final float mParticles[];
    private final VertexArray mVertexArray;

    private final int mMaxParticleCount;
    private int mCurrentParticleCount;
    private int mNextParticle;

    /**
     * Constructor that takes maximum number of particles in this system.
     *
     * @param maxParticleCount - max number of particles.
     */
    public ParticleSystem(int maxParticleCount) {
        mParticles = new float[maxParticleCount * TOTAL_COMPONENT_COUNT];
        mVertexArray = new VertexArray(mParticles);
        this.mMaxParticleCount = maxParticleCount;
    }

    /**
     * Add one particle to the vertexArray (native memory)
     *
     * @param startTime    - particle born time.
     * @param duration     - particle duration time.
     * @param fromSize     - particle from size.
     * @param toSize       - particle to size.
     * @param fromAngle    - particle from angle.
     * @param toAngle      - particle to angle.
     * @param position     - particle position.
     * @param direction    - particle direction.
     * @param fromColor    - particle from color.
     * @param toColor      - particle to color.
     * @param textureIndex - particle texture index.
     */
    public void addParticle(float startTime,
                            float duration,
                            float fromSize,
                            float toSize,
                            float fromAngle,
                            float toAngle,
                            Geometry.Point position,
                            Geometry.Vector direction,
                            int fromColor,
                            int toColor,
                            int textureIndex) {
        final int particleOffset = mNextParticle * TOTAL_COMPONENT_COUNT;
        int currentOffset = particleOffset;
        mNextParticle++;

        if (mCurrentParticleCount < mMaxParticleCount) {
            mCurrentParticleCount++;
        }
        if (mNextParticle == mMaxParticleCount) {
            mNextParticle = 0;
        }

        mParticles[currentOffset++] = startTime;
        mParticles[currentOffset++] = duration;
        mParticles[currentOffset++] = fromSize;
        mParticles[currentOffset++] = toSize;
        mParticles[currentOffset++] = fromAngle;
        mParticles[currentOffset++] = toAngle;

        mParticles[currentOffset++] = position.x;
        mParticles[currentOffset++] = position.y;
        mParticles[currentOffset++] = position.z;

        mParticles[currentOffset++] = direction.x;
        mParticles[currentOffset++] = direction.y;
        mParticles[currentOffset++] = direction.z;

        mParticles[currentOffset++] = Color.red(fromColor) / 255f;
        mParticles[currentOffset++] = Color.green(fromColor) / 255f;
        mParticles[currentOffset++] = Color.blue(fromColor) / 255f;
        mParticles[currentOffset++] = Color.alpha(fromColor) / 255f;

        mParticles[currentOffset++] = Color.red(toColor) / 255f;
        mParticles[currentOffset++] = Color.green(toColor) / 255f;
        mParticles[currentOffset++] = Color.blue(toColor) / 255f;
        mParticles[currentOffset++] = Color.alpha(toColor) / 255f;

        mParticles[currentOffset++] = textureIndex;

        // Refresh only requested part of the native memory (one particle at a time)
        mVertexArray.updateBuffer(mParticles, particleOffset, TOTAL_COMPONENT_COUNT);
    }

    /**
     * Bind attribute pointers with actual data.
     *
     * @param particleProgram - Shader program we bind with.
     */
    public void bindData(ParticleShaderProgram particleProgram) {
        int dataOffset = 0;
        mVertexArray.setVertexAttribPointer(dataOffset, particleProgram.getParticleBirthTimeAttributeLocation(),
                PARTICLE_BIRTH_TIME_COMPONENT_COUNT, STRIDE);
        dataOffset += PARTICLE_BIRTH_TIME_COMPONENT_COUNT;

        mVertexArray.setVertexAttribPointer(dataOffset, particleProgram.getParticleDurationAttributeLocation(),
                PARTICLE_DURATION_COMPONENT_COUNT, STRIDE);
        dataOffset += PARTICLE_DURATION_COMPONENT_COUNT;

        mVertexArray.setVertexAttribPointer(dataOffset, particleProgram.getParticleFromSizeAttributeLocation(),
                PARTICLE_FROM_SIZE_COMPONENT_COUNT, STRIDE);
        dataOffset += PARTICLE_FROM_SIZE_COMPONENT_COUNT;

        mVertexArray.setVertexAttribPointer(dataOffset, particleProgram.getParticleToSizeAttributeLocation(),
                PARTICLE_TO_SIZE_COMPONENT_COUNT, STRIDE);
        dataOffset += PARTICLE_TO_SIZE_COMPONENT_COUNT;

        mVertexArray.setVertexAttribPointer(dataOffset, particleProgram.getParticleFromAngleAttributeLocation(),
                PARTICLE_FROM_ANGLE_COMPONENT_COUNT, STRIDE);
        dataOffset += PARTICLE_FROM_ANGLE_COMPONENT_COUNT;

        mVertexArray.setVertexAttribPointer(dataOffset, particleProgram.getParticleToAngleLocation(),
                PARTICLE_TO_ANGLE_COMPONENT_COUNT, STRIDE);
        dataOffset += PARTICLE_TO_ANGLE_COMPONENT_COUNT;

        mVertexArray.setVertexAttribPointer(dataOffset, particleProgram.getParticlePositionAttributeLocation(),
                PARTICLE_POSITION_COMPONENT_COUNT, STRIDE);
        dataOffset += PARTICLE_POSITION_COMPONENT_COUNT;

        mVertexArray.setVertexAttribPointer(dataOffset, particleProgram.getParticleDirectionAttributeLocation(),
                PARTICLE_DIRECTION_COMPONENT_COUNT, STRIDE);
        dataOffset += PARTICLE_DIRECTION_COMPONENT_COUNT;

        mVertexArray.setVertexAttribPointer(dataOffset, particleProgram.getParticleFromColorAttributeLocation(),
                PARTICLE_FROM_COLOR_COMPONENT_COUNT, STRIDE);
        dataOffset += PARTICLE_FROM_COLOR_COMPONENT_COUNT;

        mVertexArray.setVertexAttribPointer(dataOffset, particleProgram.getParticleToColorAttributeLocation(),
                PARTICLE_TO_COLOR_COMPONENT_COUNT, STRIDE);
        dataOffset += PARTICLE_TO_COLOR_COMPONENT_COUNT;

        mVertexArray.setVertexAttribPointer(dataOffset, particleProgram.getParticleTextureIndexLocation(),
                PARTICLE_TEXTURE_INDEX_COMPONENT_COUNT, STRIDE);
        dataOffset += PARTICLE_TEXTURE_INDEX_COMPONENT_COUNT;
    }

    /**
     * Call glDrawArrays for current number of particles.
     */
    public void draw() {
        glDrawArrays(GL_POINTS, 0, mCurrentParticleCount);
    }
}

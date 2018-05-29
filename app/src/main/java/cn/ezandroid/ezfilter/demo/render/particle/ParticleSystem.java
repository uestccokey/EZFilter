package cn.ezandroid.ezfilter.demo.render.particle;

import android.graphics.Color;

import cn.ezandroid.ezfilter.demo.render.particle.util.Geometry;

import static android.opengl.GLES20.GL_POINTS;
import static android.opengl.GLES20.glDrawArrays;

/**
 * 粒子系统
 */
public class ParticleSystem {

    private static final int PARTICLE_START_TIME_COMPONENT_COUNT = 1;
    private static final int PARTICLE_DURATION_COMPONENT_COUNT = 1;
    private static final int PARTICLE_SIZE_COMPONENT_COUNT = 1;
    private static final int PARTICLE_POSITION_COMPONENT_COUNT = 3; // [ x, y, z ]
    private static final int PARTICLE_DIRECTION_COMPONENT_COUNT = 3; // [ x2, y2, z2 ]
    private static final int PARTICLE_COLOR_COMPONENT_COUNT = 4; // [ a, r, g, b ]

    private static final int TOTAL_COMPONENT_COUNT = PARTICLE_START_TIME_COMPONENT_COUNT
            + PARTICLE_DURATION_COMPONENT_COUNT
            + PARTICLE_SIZE_COMPONENT_COUNT
            + PARTICLE_POSITION_COMPONENT_COUNT
            + PARTICLE_DIRECTION_COMPONENT_COUNT
            + PARTICLE_COLOR_COMPONENT_COUNT; // 每个粒子的Float字段总数

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
     * @param position          - particle position.
     * @param color             - particle color.
     * @param direction         - speed direction.
     * @param particleStartTime - particle born time.
     * @param particleDuration  - particle duration time.
     * @param particleSize      - particle size.
     */
    public void addParticle(Geometry.Point position,
                            int color,
                            Geometry.Vector direction,
                            float particleStartTime,
                            float particleDuration,
                            float particleSize) {
        final int particleOffset = mNextParticle * TOTAL_COMPONENT_COUNT;
        int currentOffset = particleOffset;
        mNextParticle++;

        if (mCurrentParticleCount < mMaxParticleCount) {
            mCurrentParticleCount++;
        }
        if (mNextParticle == mMaxParticleCount) {
            mNextParticle = 0;
        }

        mParticles[currentOffset++] = particleStartTime;
        mParticles[currentOffset++] = particleDuration;
        mParticles[currentOffset++] = particleSize;

        mParticles[currentOffset++] = position.x;
        mParticles[currentOffset++] = position.y;
        mParticles[currentOffset++] = position.z;

        mParticles[currentOffset++] = direction.x;
        mParticles[currentOffset++] = direction.y;
        mParticles[currentOffset++] = direction.z;

        mParticles[currentOffset++] = Color.red(color) / 255f;
        mParticles[currentOffset++] = Color.green(color) / 255f;
        mParticles[currentOffset++] = Color.blue(color) / 255f;
        mParticles[currentOffset++] = Color.alpha(color) / 255f;

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
        mVertexArray.setVertexAttribPointer(dataOffset, particleProgram.getParticleStartTimeAttributeLocation(),
                PARTICLE_START_TIME_COMPONENT_COUNT, STRIDE);
        dataOffset += PARTICLE_START_TIME_COMPONENT_COUNT;

        mVertexArray.setVertexAttribPointer(dataOffset, particleProgram.getParticleDurationAttributeLocation(),
                PARTICLE_DURATION_COMPONENT_COUNT, STRIDE);
        dataOffset += PARTICLE_DURATION_COMPONENT_COUNT;

        mVertexArray.setVertexAttribPointer(dataOffset, particleProgram.getParticleSizeAttributeLocation(),
                PARTICLE_SIZE_COMPONENT_COUNT, STRIDE);
        dataOffset += PARTICLE_SIZE_COMPONENT_COUNT;

        mVertexArray.setVertexAttribPointer(dataOffset, particleProgram.getParticlePositionAttributeLocation(),
                PARTICLE_POSITION_COMPONENT_COUNT, STRIDE);
        dataOffset += PARTICLE_POSITION_COMPONENT_COUNT;

        mVertexArray.setVertexAttribPointer(dataOffset, particleProgram.getParticleDirectionAttributeLocation(),
                PARTICLE_DIRECTION_COMPONENT_COUNT, STRIDE);
        dataOffset += PARTICLE_DIRECTION_COMPONENT_COUNT;

        mVertexArray.setVertexAttribPointer(dataOffset, particleProgram.getParticleColorAttributeLocation(),
                PARTICLE_COLOR_COMPONENT_COUNT, STRIDE);
//        dataOffset += PARTICLE_COLOR_COMPONENT_COUNT;
    }

    /**
     * Call glDrawArrays for current number of particles.
     */
    public void draw() {
        glDrawArrays(GL_POINTS, 0, mCurrentParticleCount);
    }
}

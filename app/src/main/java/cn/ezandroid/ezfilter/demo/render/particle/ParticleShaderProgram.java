package cn.ezandroid.ezfilter.demo.render.particle;

import android.content.Context;

import java.util.Arrays;

import cn.ezandroid.ezfilter.demo.R;
import cn.ezandroid.ezfilter.demo.render.particle.util.ShaderHelper;
import cn.ezandroid.ezfilter.demo.render.particle.util.TextResourceReader;

import static android.opengl.GLES20.GL_TEXTURE2;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUseProgram;

/**
 * 粒子效果Shader程序封装
 */
public class ParticleShaderProgram {

    // Uniform constants
    private static final String U_TIME = "u_Time";
    private static final String U_TEXTURE_UNIT = "u_TextureUnit";

    // Attributes constants
    private static final String A_PARTICLE_START_TIME = "a_BirthTime";
    private static final String A_PARTICLE_DURATION = "a_Duration";
    private static final String A_PARTICLE_SIZE = "a_Size";
    private static final String A_PARTICLE_POSITION = "a_BirthPosition";
    private static final String A_PARTICLE_DIRECTION = "a_DirectionVector";
    private static final String A_PARTICLE_COLOR = "a_Color";

    private final int mProgram;

    // Attribute constants
    private static final String ATTRIBUTES[] = {A_PARTICLE_START_TIME, A_PARTICLE_DURATION, A_PARTICLE_SIZE, A_PARTICLE_POSITION,
            A_PARTICLE_DIRECTION, A_PARTICLE_COLOR};

    // Attribute locations
    private static final int A_PARTICLE_START_TIME_LOCATION = getAttributeLocation(ATTRIBUTES, A_PARTICLE_START_TIME);
    private static final int A_PARTICLE_DURATION_LOCATION = getAttributeLocation(ATTRIBUTES, A_PARTICLE_DURATION);
    private static final int A_PARTICLE_SIZE_LOCATION = getAttributeLocation(ATTRIBUTES, A_PARTICLE_SIZE);
    private static final int A_PARTICLE_POSITION_LOCATION = getAttributeLocation(ATTRIBUTES, A_PARTICLE_POSITION);
    private static final int A_PARTICLE_DIRECTION_LOCATION = getAttributeLocation(ATTRIBUTES, A_PARTICLE_DIRECTION);
    private static final int A_PARTICLE_COLOR_LOCATION = getAttributeLocation(ATTRIBUTES, A_PARTICLE_COLOR);

    // Uniform locations
    private final int mUTimeLocation;
    private final int mUTextureLocation;

    public ParticleShaderProgram(Context context) {
        mProgram = ShaderHelper.buildProgram(
                TextResourceReader.readTextFileFromResource(context, R.raw.particle_vertex_shader),
                TextResourceReader.readTextFileFromResource(context, R.raw.particle_fragment_shader),
                ATTRIBUTES);
        mUTimeLocation = glGetUniformLocation(mProgram, U_TIME);
        mUTextureLocation = glGetUniformLocation(mProgram, U_TEXTURE_UNIT);
    }

    public void setUniforms(float elapsedTime, int textureId) {
        glUniform1f(mUTimeLocation, elapsedTime);

        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glUniform1i(mUTextureLocation, 2);
    }

    public void useProgram() {
        glUseProgram(mProgram);
    }

    private static int getAttributeLocation(String array[], String attribute) {
        return Arrays.asList(array).indexOf(attribute);
    }

    public int getParticleStartTimeAttributeLocation() {
        return A_PARTICLE_START_TIME_LOCATION;
    }

    public int getParticleDurationAttributeLocation() {
        return A_PARTICLE_DURATION_LOCATION;
    }

    public int getParticleSizeAttributeLocation() {
        return A_PARTICLE_SIZE_LOCATION;
    }

    public int getParticlePositionAttributeLocation() {
        return A_PARTICLE_POSITION_LOCATION;
    }

    public int getParticleDirectionAttributeLocation() {
        return A_PARTICLE_DIRECTION_LOCATION;
    }

    public int getParticleColorAttributeLocation() {
        return A_PARTICLE_COLOR_LOCATION;
    }
}

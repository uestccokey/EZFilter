package cn.ezandroid.ezfilter.demo.render.particle.util;

import android.opengl.GLES20;
import android.util.Log;

import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_VALIDATE_STATUS;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glBindAttribLocation;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glGetProgramInfoLog;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glValidateProgram;

/**
 * Shader编译和链接工具类
 */
public class ShaderHelper {

    private static final String TAG = "Shader helper";

    public static int compileVertexShader(String shaderCode) { return compileShader(GL_VERTEX_SHADER, shaderCode); }

    public static int compileFragmentShader(String shaderCode) { return compileShader(GL_FRAGMENT_SHADER, shaderCode); }

    /**
     * This method creates the shader object, loads shader source in it and compile.
     *
     * @param shaderType   - Type of the shader (vertex or fragment)
     * @param shaderSource - String - shaderSource
     * @return - shader handler if success, otherwise - "-1"
     */
    private static int compileShader(int shaderType, String shaderSource) {
        final int shaderObjectId = glCreateShader(shaderType);
        if (shaderObjectId == 0) {
//            if (LoggerConfig.ON) {
            Log.w(TAG, "Could not create new shader.");
//            }
            return 0;
        }

        glShaderSource(shaderObjectId, shaderSource);
        glCompileShader(shaderObjectId);
        final int compileStatus[] = new int[1];
        glGetShaderiv(shaderObjectId, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
//        if (LoggerConfig.ON) {
//            Log.v(TAG, "Results of compiling source:" + "\n" + shaderSource + "\n" + glGetShaderInfoLog(shaderObjectId));
//        }
        if (compileStatus[0] == 0) {
            glDeleteShader(shaderObjectId);
//            if (LoggerConfig.ON) {
            Log.w(TAG, "Compilation of shader failed.");
//            }
            return 0;
        }
        return shaderObjectId;
    }

    /**
     * Link to shader to the shader program.
     *
     * @param vertexShaderId   - vertex shader handler.
     * @param fragmentShaderId - fragment shader handler.
     * @param attributes       - array of shader string attributes.
     * @return
     */
    public static int linkProgram(int vertexShaderId, int fragmentShaderId, String attributes[]) {
        final int programObjectId = glCreateProgram();
        if (programObjectId == 0) {
//            if (LoggerConfig.ON) {
            Log.w(TAG, "Could not create new program.");
//            }
            return 0;
        }

        glAttachShader(programObjectId, vertexShaderId);
        glAttachShader(programObjectId, fragmentShaderId);
        for (int i = 0; i < attributes.length; i++) {
            glBindAttribLocation(programObjectId, i, attributes[i]);
        }
        glLinkProgram(programObjectId);
        final int linkStatus[] = new int[1];
        glGetProgramiv(programObjectId, GL_LINK_STATUS, linkStatus, 0);
//        if (LoggerConfig.ON) {
//            Log.v(TAG, "Results of linking program:\n" + glGetProgramInfoLog(programObjectId));
//        }
        if (linkStatus[0] == 0) {
            glDeleteProgram(programObjectId);
//            if (LoggerConfig.ON) {
            Log.w(TAG, "Linking of program failed.");
//            }
            return 0;
        }
        return programObjectId;
    }

    /**
     * Check if program is valid.
     *
     * @param programObjectId - shader program handler.
     * @return true if shader program is valid
     */
    private static boolean validateProgram(int programObjectId) {
        glValidateProgram(programObjectId);
        final int validateStatus[] = new int[1];
        glGetProgramiv(programObjectId, GL_VALIDATE_STATUS, validateStatus, 0);
        Log.v(TAG, "Results of validating program: " + validateStatus[0] + "\nLog: " + glGetProgramInfoLog(programObjectId));
        return validateStatus[0] != 0;
    }

    public static int buildProgram(String vertexShaderSource, String fragmentShaderSource, String attributes[]) {
        int program;
        int vertexShader = compileVertexShader(vertexShaderSource);
        int fragmentShader = compileFragmentShader(fragmentShaderSource);
        program = linkProgram(vertexShader, fragmentShader, attributes);
//        if (LoggerConfig.ON) {
        validateProgram(program);
//        }
        return program;
    }
}

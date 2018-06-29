package cn.ezandroid.ezfilter.core.util;

import android.opengl.GLES20;

import static android.opengl.GLES20.glBindAttribLocation;

/**
 * ShaderHelper
 *
 * @author like
 * @date 2018-05-29
 */
public class ShaderHelper {

    public static int compileShader(String shaderSource, int shaderType) {
        String errorInfo = "none";

        int shaderHandle = GLES20.glCreateShader(shaderType);
        if (shaderHandle != 0) {
            GLES20.glShaderSource(shaderHandle, shaderSource);
            GLES20.glCompileShader(shaderHandle);
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
            if (compileStatus[0] == 0) {
                errorInfo = GLES20.glGetShaderInfoLog(shaderHandle);
                GLES20.glDeleteShader(shaderHandle);
                shaderHandle = 0;
            }
        }
        if (shaderHandle == 0) {
            throw new RuntimeException("failed to compile shader. Reason: " + errorInfo);
        }

        return shaderHandle;
    }

    public static int linkProgram(int vertexShaderHandle, int fragmentShaderHandle, String attributes[]) {
        int programHandle = GLES20.glCreateProgram();
        if (programHandle != 0) {
            GLES20.glAttachShader(programHandle, vertexShaderHandle);
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);

            for (int i = 0; i < attributes.length; i++) {
                glBindAttribLocation(programHandle, i, attributes[i]);
            }

            GLES20.glLinkProgram(programHandle);
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] == 0) {
                GLES20.glDeleteProgram(programHandle);
                programHandle = 0;
            }
        }
        if (programHandle == 0) {
            throw new RuntimeException("failed to link program.");
        }

        return programHandle;
    }
}

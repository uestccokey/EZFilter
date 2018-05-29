package cn.ezandroid.ezfilter.demo.render.particle;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glVertexAttribPointer;

/**
 * 顶点数组封装
 */
public class VertexArray {

    private final FloatBuffer mFloatBuffer;

    /**
     * Allocate memory in native space and put @vertexData[] there.
     */
    public VertexArray(float vertexData[]) {
        mFloatBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
    }

    /**
     * Set attribute pointer to floatBuffer(native memory)
     *
     * @param dataOffset        - offset from the start of the floatBuffer.
     * @param attributeLocation - location of the attribute.
     * @param componentCount    - number of components per attribute
     * @param stride            - stride between the consequently located attributes. (in bytes)
     */
    public void setVertexAttribPointer(int dataOffset, int attributeLocation, int componentCount, int stride) {
        mFloatBuffer.position(dataOffset);
        glVertexAttribPointer(attributeLocation, componentCount, GL_FLOAT, false, stride, mFloatBuffer);
        glEnableVertexAttribArray(attributeLocation);
        mFloatBuffer.position(0);
    }

    /**
     * Update part of the buffer.
     *
     * @param vertexData - data read from.
     * @param start      - start index.
     * @param count      - number of floats should be updated.
     */
    public void updateBuffer(float vertexData[], int start, int count) {
        mFloatBuffer.position(start);
        mFloatBuffer.put(vertexData, start, count);
        mFloatBuffer.position(0);
    }
}

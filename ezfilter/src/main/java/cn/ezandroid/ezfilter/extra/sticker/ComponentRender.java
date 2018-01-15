package cn.ezandroid.ezfilter.extra.sticker;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import cn.ezandroid.ezfilter.core.cache.IBitmapCache;
import cn.ezandroid.ezfilter.core.util.BitmapUtil;
import cn.ezandroid.ezfilter.extra.sticker.model.Component;

/**
 * 贴纸组件渲染器
 *
 * @author like
 * @date 2018-01-09
 */
public class ComponentRender {

    private Context mContext;
    private Component mComponent;
    private IBitmapCache mBitmapCache;

    private int mTexture;

    private int mLastIndex = -1;

    private long mStartTime = -1;

    private FloatBuffer mRenderVertices; // 渲染顶点

    public ComponentRender(Context context, Component component) {
        mContext = context;
        mComponent = component;

        // 测试数据
        float vertices[] = new float[8];
        vertices[0] = 1.5671111f;
        vertices[1] = -1.0f;
        vertices[2] = -1.0f;
        vertices[3] = -1.0f;
        vertices[4] = 1.5671111f;
        vertices[5] = 1.0f;
        vertices[6] = -1.0f;
        vertices[7] = 1.0f;
        mRenderVertices = ByteBuffer.allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertices);
    }

    public void setBitmapCache(IBitmapCache bitmapCache) {
        mBitmapCache = bitmapCache;
    }

    /**
     * 绘制组件
     * <p>
     * 在GL线程调用
     *
     * @param textureHandle
     * @param positionHandle
     * @param textureCoordHandle
     * @param textureVertices
     */
    public void onDraw(int textureHandle, int positionHandle, int textureCoordHandle, FloatBuffer textureVertices) {
        mRenderVertices.position(0);
        textureVertices.position(0);

        if (mStartTime == -1) {
            mStartTime = System.currentTimeMillis();
        }
        long currentTime = System.currentTimeMillis();
        long position = (currentTime - mStartTime) % mComponent.duration;
        // 如mComponent.duration=3000，mComponent.length=60，position=1000，则currentIndex=20
        int currentIndex = Math.round((mComponent.length - 1) * 1.0f / mComponent.duration * position);

//        Log.e("ComponentRender:", "onDraw:" + currentIndex);

        String path = mComponent.resources.get(currentIndex);
        Bitmap bitmap = mBitmapCache.get(path);
        if (bitmap == null || bitmap.isRecycled()) {
            bitmap = BitmapUtil.loadBitmap(mContext, path);
//            Log.e("ComponentRender:", "loadBitmap" + path + " -> " + bitmap);
            if (bitmap != null && !bitmap.isRecycled()) {
                mBitmapCache.put(path, bitmap);
            } else {
                return;
            }
        }

        // 当前帧不变时，不重新绑定Bitmap，直接渲染已绑定的纹理
        if (mLastIndex != currentIndex) {
            if (mTexture != 0) {
                int[] tex = new int[1];
                tex[0] = mTexture;
                GLES20.glDeleteTextures(1, tex, 0);
                mTexture = 0;
            }
            mTexture = BitmapUtil.bindBitmap(bitmap);
//            Log.e("ComponentRender:", "bindBitmap:" + bitmap + " -> " + mTexture);
        }

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture);
        GLES20.glUniform1i(textureHandle, 2);

        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, mRenderVertices);
        GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, textureVertices);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        mLastIndex = currentIndex;
    }

    /**
     * 销毁组件资源
     * <p>
     * 在GL线程调用
     */
    public void destroy() {
        if (mTexture != 0) {
            int[] tex = new int[1];
            tex[0] = mTexture;
            GLES20.glDeleteTextures(1, tex, 0);
            mTexture = 0;
        }
    }
}

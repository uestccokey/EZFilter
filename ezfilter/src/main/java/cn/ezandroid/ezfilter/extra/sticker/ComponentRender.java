package cn.ezandroid.ezfilter.extra.sticker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import cn.ezandroid.ezfilter.core.cache.IBitmapCache;
import cn.ezandroid.ezfilter.core.util.BitmapUtil;
import cn.ezandroid.ezfilter.extra.sticker.model.Component;
import cn.ezandroid.ezfilter.extra.sticker.model.ScreenAnchor;

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

    // 上一帧序号
    private int mLastIndex = -1;

    private long mStartTime = -1;

    // 渲染顶点坐标
    private FloatBuffer mRenderVertices;

    // 显示锚点
    private ScreenAnchor mScreenAnchor;

    public ComponentRender(Context context, Component component) {
        mContext = context;
        mComponent = component;

        // 4个顶点，每个顶点由x，y两个float变量组成，每个float占4字节，总共32字节
        mRenderVertices = ByteBuffer.allocateDirect(32)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
    }

    /**
     * 设置图片缓存
     *
     * @param bitmapCache
     */
    public void setBitmapCache(IBitmapCache bitmapCache) {
        mBitmapCache = bitmapCache;
    }

    /**
     * 设置显示锚点
     *
     * @param screenAnchor
     */
    public void setScreenAnchor(ScreenAnchor screenAnchor) {
        mScreenAnchor = screenAnchor;
    }

    /**
     * 更新渲染顶点坐标
     * <p>
     * 在GL线程调用
     *
     * @param width
     * @param height
     */
    public void updateRenderVertices(int width, int height) {
        PointF screenLeftPoint = mScreenAnchor.getLeftAnchorPoint();
        PointF screenRightPoint = mScreenAnchor.getRightAnchorPoint();
        PointF textureLeftPoint = mComponent.textureAnchor.getLeftAnchorPoint();
        PointF textureRightPoint = mComponent.textureAnchor.getRightAnchorPoint();

        float w = mComponent.width;
        float h = mComponent.height;

        // 计算屏幕两点距离与贴纸对应的两点之间距离的比例，并等比缩放贴纸
        float rate = distanceOf(screenLeftPoint, screenRightPoint) / distanceOf(textureLeftPoint, textureRightPoint);
        textureLeftPoint.x = textureLeftPoint.x * rate;
        textureLeftPoint.y = textureLeftPoint.y * rate;
        textureRightPoint.x = textureRightPoint.x * rate;
        textureRightPoint.y = textureRightPoint.y * rate;
        w = w * rate;
        h = h * rate;

        // 确定贴纸四个顶点坐标
        PointF leftTop = new PointF(screenLeftPoint.x - textureLeftPoint.x, screenLeftPoint.y + textureLeftPoint.y);
        PointF leftBottom = new PointF(leftTop.x, leftTop.y - h);
        PointF rightTop = new PointF(leftTop.x + w, leftTop.y);
        PointF rightBottom = new PointF(rightTop.x, leftBottom.y);

        // 计算旋转角
        double angle;
        if (mScreenAnchor.roll == ScreenAnchor.INVALID_VALUE) {
            // 这个旋转点在旋转角度为0时就是screenRightPoint的坐标
            PointF beforeRotatePoint = new PointF(leftTop.x + textureRightPoint.x, leftTop.y - textureRightPoint.y);
            // 根据三点算旋转角度
            float a = distanceOf(screenLeftPoint, beforeRotatePoint);
            float b = distanceOf(screenLeftPoint, screenRightPoint);
            float c = distanceOf(beforeRotatePoint, screenRightPoint);
            // 余弦定理求出旋转角度
            angle = Math.acos((a * a + b * b - c * c) / (2 * a * b));

            // 修正旋转角度；贴纸右边的点关于左边的点的对称点，关于x轴对称
            if (screenRightPoint.x < beforeRotatePoint.x && screenRightPoint.y < 2 * screenLeftPoint.y - beforeRotatePoint.y) {
                angle = -angle;
            }
        } else {
            angle = (180.0 - mScreenAnchor.roll) / 180.0 * 3.14;
        }

        // 旋转四个顶点到目标位置
        leftTop = getRotateVertices(leftTop, screenLeftPoint, angle);
        leftBottom = getRotateVertices(leftBottom, screenLeftPoint, angle);
        rightTop = getRotateVertices(rightTop, screenLeftPoint, angle);
        rightBottom = getRotateVertices(rightBottom, screenLeftPoint, angle);

        // 转换为OpenGL坐标系坐标值
        leftTop = transVerticesToOpenGL(leftTop, width, height);
        leftBottom = transVerticesToOpenGL(leftBottom, width, height);
        rightTop = transVerticesToOpenGL(rightTop, width, height);
        rightBottom = transVerticesToOpenGL(rightBottom, width, height);

        // 之前遇到素材被镜像的问题改下面坐标对应关系就好了
        float vertices[] = new float[8];
        vertices[0] = rightBottom.x;
        vertices[1] = rightBottom.y;
        vertices[2] = leftBottom.x;
        vertices[3] = leftBottom.y;
        vertices[4] = rightTop.x;
        vertices[5] = rightTop.y;
        vertices[6] = leftTop.x;
        vertices[7] = leftTop.y;

        mRenderVertices.clear();
        mRenderVertices.put(vertices);
    }

    /**
     * 绘制组件
     * <p>
     * 在GL线程调用
     *
     * @param textureHandle      纹理指针
     * @param positionHandle     渲染顶点坐标指针
     * @param textureCoordHandle 纹理顶点坐标指针
     * @param textureVertices    纹理顶点坐标
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

        String path = mComponent.resources.get(currentIndex);
        Bitmap bitmap = mBitmapCache.get(path);
        if (bitmap == null || bitmap.isRecycled()) {
            bitmap = BitmapUtil.loadBitmap(mContext, path, mComponent.width, mComponent.height);
            if (bitmap != null && !bitmap.isRecycled()) {
//                // 按照mComponent.width和mComponent.height尺寸对图片进行缩放
//                if (bitmap.getWidth() != mComponent.width || bitmap.getHeight() != mComponent.height) {
//                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, mComponent.width, mComponent.height, true);
//                    if (scaledBitmap != bitmap) {
//                        bitmap.recycle();
//                    }
//                    bitmap = scaledBitmap;
//                }
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

    private PointF getRotateVertices(PointF point, PointF anchorPoint, double angle) {
        return new PointF(
                (float) ((point.x - anchorPoint.x) * Math.cos(angle) -
                        (point.y - anchorPoint.y) * Math.sin(angle) + anchorPoint.x),
                (float) ((point.x - anchorPoint.x) * Math.sin(angle) +
                        (point.y - anchorPoint.y) * Math.cos(angle) + anchorPoint.y));
    }

    private PointF transVerticesToOpenGL(PointF point, float width, float height) {
        return new PointF((point.x - width / 2) / (width / 2),
                (point.y - height / 2) / (height / 2));
    }

    private float distanceOf(PointF p0, PointF p1) {
        return (float) Math.sqrt((p0.x - p1.x) * (p0.x - p1.x) + (p0.y - p1.y) * (p0.y - p1.y));
    }
}

package cn.ezandroid.ezfilter.extra.sticker.model;

import android.graphics.PointF;

import java.io.Serializable;

/**
 * 锚点
 *
 * @author like
 * @date 2018-01-05
 */
public class AnchorPoint implements Serializable {

    private static final long serialVersionUID = 1L;

    // 锚点 屏幕左上
    public static final int LEFT_TOP = -1;
    // 锚点 屏幕右上
    public static final int RIGHT_TOP = -2;
    // 锚点 屏幕左下
    public static final int LEFT_BOTTOM = -3;
    // 锚点 屏幕右下
    public static final int RIGHT_BOTTOM = -4;
    // 锚点 屏幕中心
    public static final int CENTER = -5;

    // 锚点id
    public int id;

    // 组件左上角与锚点id的x轴偏移量
    public float x;
    // 组件左上角与锚点id的y轴偏移量
    public float y;

    public float width;

    public float height;

    public AnchorPoint() {
    }

    public AnchorPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public AnchorPoint(int id, int x, int y, int width, int height) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public String toString() {
        return "AnchorPoint{" +
                "id=" + id +
                ", x=" + x +
                ", y=" + y +
                '}';
    }

    /**
     * 根据锚点id获取实际锚点位置
     *
     * @param mirror 是否垂直镜像
     * @return
     */
    public PointF getPointF(boolean mirror) {
        if (mirror) {
            switch (id) {
                case LEFT_TOP:
                    return new PointF(x, height - y);
                case LEFT_BOTTOM:
                    return new PointF(x, -y);
                case RIGHT_BOTTOM:
                    return new PointF(width + x, -y);
                case RIGHT_TOP:
                    return new PointF(width + x, height - y);
                case CENTER:
                    return new PointF(width / 2 + x, height / 2 - y);
            }
        } else {
            switch (id) {
                case LEFT_TOP:
                    return new PointF(-x, -y);
                case LEFT_BOTTOM:
                    return new PointF(-x, height - y);
                case RIGHT_BOTTOM:
                    return new PointF(width - x, height - y);
                case RIGHT_TOP:
                    return new PointF(width - x, -y);
                case CENTER:
                    return new PointF(width / 2 - x, height / 2 - y);
            }
        }
        return new PointF(x, y);
    }
}

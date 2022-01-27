package cn.ezandroid.lib.ezfilter.extra.sticker.model;

import android.graphics.PointF;

import java.io.Serializable;

/**
 * 锚点
 *
 * @author like
 * @date 2018-01-05
 */
public class AnchorPoint extends PointF implements Serializable {

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

    public AnchorPoint() {
    }

    public AnchorPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public AnchorPoint(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "AnchorPoint{" +
                "id=" + id +
                ", x=" + x +
                ", y=" + y +
                '}';
    }
}

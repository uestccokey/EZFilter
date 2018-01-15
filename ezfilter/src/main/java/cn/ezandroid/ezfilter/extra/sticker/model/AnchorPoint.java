package cn.ezandroid.ezfilter.extra.sticker.model;

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
    public int x;
    // 组件左上角与锚点id的y轴偏移量
    public int y;

    @Override
    public String toString() {
        return "AnchorPoint{" +
                "id=" + id +
                ", x=" + x +
                ", y=" + y +
                '}';
    }
}

package cn.ezandroid.ezfilter.extra.sticker.model;

import android.graphics.PointF;

import java.io.Serializable;

/**
 * 屏幕锚点
 * <p>
 * 由左右两个子锚点组成，屏幕锚点标记后，组件的纹理锚点再进行对准，可以算出组件此时的缩放比、旋转角及位置信息。
 * 比如屏幕锚点设置为-1，-2，纹理锚点设置为-1，-2，意思为，使用纹理的左上角对齐屏幕的左上角，纹理的右上角对齐屏幕的右上角，图像等比拉伸
 * -1        -2
 * ┌——————————┐
 * |  Screen  |
 * |-1      -2|
 * |┌————————┐|
 * ||Texture ||
 * ||   -5   ||
 * |└--------┘|
 * |-3      -4|
 * |          |
 * └----------┘
 * -3        -4
 *
 * @author like
 * @date 2018-01-16
 */
public class ScreenAnchor implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int INVALID_VALUE = Integer.MAX_VALUE;

    // 左锚点
    public AnchorPoint leftAnchor;

    // 右锚点
    public AnchorPoint rightAnchor;

    // 旋转角，往右转从-180~0，往左转从180~0
    //>              180 | -180
    //>            125   |    -125
    //>          90      |       -90
    //>            45    |    -45
    //>               0  |  -0
    public float roll = INVALID_VALUE;

    // 显示宽度
    public int width;

    // 显示高度
    public int height;

    public PointF getLeftAnchorPoint() {
        return getPoint(leftAnchor);
    }

    public PointF getRightAnchorPoint() {
        return getPoint(rightAnchor);
    }

    private PointF getPoint(AnchorPoint anchorPoint) {
        int id = anchorPoint.id;
        float x = anchorPoint.x;
        float y = anchorPoint.y;
        switch (id) {
            case AnchorPoint.LEFT_TOP:
                return new PointF(x, height - y);
            case AnchorPoint.LEFT_BOTTOM:
                return new PointF(x, -y);
            case AnchorPoint.RIGHT_BOTTOM:
                return new PointF(width + x, -y);
            case AnchorPoint.RIGHT_TOP:
                return new PointF(width + x, height - y);
            case AnchorPoint.CENTER:
                return new PointF(width / 2 + x, height / 2 - y);
        }
        return new PointF(x, y);
    }
}

package cn.ezandroid.ezfilter.extra.sticker.model;

import java.io.Serializable;

/**
 * 锚点组
 * <p>
 * 2D贴纸要确定在空间的位置需要两个锚点
 *
 * @author like
 * @date 2018-01-05
 */
public class AnchorGroup implements Serializable {

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
}

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

    public AnchorPoint leftAnchor;

    public AnchorPoint rightAnchor;

    public float roll = INVALID_VALUE;
}

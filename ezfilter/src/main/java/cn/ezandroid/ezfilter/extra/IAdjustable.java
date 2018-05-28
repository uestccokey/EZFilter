package cn.ezandroid.ezfilter.extra;

/**
 * 可调节强度接口
 *
 * @author like
 * @date 2017-09-26
 */
public interface IAdjustable {

    /**
     * 调节强度
     *
     * @param progress 0~1之间的值，0表示没有效果，1表示效果最强
     */
    void adjust(float progress);

    /**
     * 获取当前强度
     * <p>
     * 可选方法
     *
     * @return
     */
    default float getProgress() {
        return 0;
    }
}

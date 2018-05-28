package cn.ezandroid.ezfilter.camera;

/**
 * 支持拍照接口
 *
 * @author like
 * @date 2017-10-24
 */
public interface ISupportTakePhoto {

    /**
     * 拍照
     *
     * @param isFront  是否前置摄像头
     * @param degree   手机旋转角度（0~360度）
     * @param callback 回调
     */
    void takePhoto(boolean isFront, int degree, PhotoTakenCallback callback);
}

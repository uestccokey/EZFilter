package cn.ezandroid.ezfilter.core;

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
     * @param cameraId    摄像头ID
     * @param orientation 手机旋转方向，0,1,2,3表示0度，90第，180度，270度
     * @param callback    回调
     */
    void takePhoto(int cameraId, int orientation, PhotoTakenCallback callback);
}

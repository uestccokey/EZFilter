package cn.ezandroid.ezfilter.camera;

import android.graphics.Bitmap;

/**
 * 拍照回调
 *
 * @author like
 * @date 2017-10-24
 */
public interface PhotoTakenCallback {

    void onPhotoTaken(Bitmap bitmap);
}

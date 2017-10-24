package cn.ezandroid.ezfilter.demo.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.util.Log;
import android.view.OrientationEventListener;

/**
 * PictureJpeg
 *
 * @author like
 * @date 2017-10-24
 */
public class PictureJpeg implements Camera.PictureCallback {

    // Orientation hysteresis amount used in rounding, in degrees
    private static final int ORIENTATION_HYSTERESIS = 5;

    private int mOrientation;

    private int mCameraId;

    private PictureTakenCallback mTakenCallback;

    public interface PictureTakenCallback {

        void onPictureTaken(final Bitmap bitmap);
    }

    public PictureJpeg(PictureTakenCallback takenCallback) {
        mTakenCallback = takenCallback;
    }

    public void setCameraId(int cameraId) {
        mCameraId = cameraId;
    }

    public void setOrientation(int orientation) {
        mOrientation = roundOrientation(orientation, mOrientation);
    }

    @Override
    public void onPictureTaken(final byte[] data, final Camera camera) {
        // data为Jpeg格式的压缩数据
        new Thread() {
            public void run() {
                long time = System.currentTimeMillis();
                // 1.读取原始图片
                final Bitmap bitmap0 = BitmapFactory.decodeByteArray(data, 0, data.length);
                Log.e("Camera", "读取原始图片:" + (System.currentTimeMillis() - time));
                time = System.currentTimeMillis();

                // 2.旋转及镜像原始图片
                Matrix matrix = new Matrix();
                matrix.setRotate(mOrientation);
                if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    matrix.setScale(-1, 1);
                }
                Bitmap bitmap1 = Bitmap.createBitmap(bitmap0, 0, 0,
                        bitmap0.getWidth(), bitmap0.getHeight(), matrix, true);
                Log.e("Camera", "旋转及镜像原始图片:" + (System.currentTimeMillis() - time));

                // 由于bitmap1可能与bitmap0是同一个对象，这里进行判断
                if (bitmap1 != bitmap0) {
                    bitmap0.recycle();
                }

                if (mTakenCallback != null) {
                    mTakenCallback.onPictureTaken(bitmap1);
                }
            }
        }.start();
    }

    private int roundOrientation(int orientation, int orientationHistory) {
        boolean changeOrientation;
        if (orientationHistory == OrientationEventListener.ORIENTATION_UNKNOWN) {
            changeOrientation = true;
        } else {
            int dist = Math.abs(orientation - orientationHistory);
            dist = Math.min(dist, 360 - dist);
            changeOrientation = (dist >= 45 + ORIENTATION_HYSTERESIS);
        }
        if (changeOrientation) {
            return ((orientation + 45) / 90 * 90) % 360;
        }
        return orientationHistory;
    }
}

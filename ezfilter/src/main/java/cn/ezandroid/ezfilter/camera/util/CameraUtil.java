package cn.ezandroid.ezfilter.camera.util;

import android.media.ExifInterface;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * CameraUtil
 *
 * @author like
 * @date 2017-10-25
 */
public class CameraUtil {

    /**
     * 获取Exif中的旋转角信息
     *
     * @param data 相机输出的原始图片数据
     * @return
     */
    public static int getExifDegree(byte[] data) {
        FileOutputStream fos = null;
        int degree = 0;
        try {
            // 创建临时文件
            File file = new File(Environment.getExternalStorageDirectory().getPath() + "/" + System.currentTimeMillis() + ".jpg");
            if (!file.exists()) {
                file.createNewFile();
            }

            // 写入原图数据
            fos = new FileOutputStream(file);
            fos.write(data);

            // 读取旋转信息
            ExifInterface exifInterface = new ExifInterface(file.getAbsolutePath());
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }

            // 删除临时文件
            file.delete();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return degree;
    }
}

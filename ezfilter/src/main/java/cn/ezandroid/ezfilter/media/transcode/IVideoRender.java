package cn.ezandroid.ezfilter.media.transcode;

import android.graphics.SurfaceTexture;

/**
 * 纹理渲染接口
 *
 * @author like
 * @date 2017-09-23
 */
public interface IVideoRender {

    /**
     * 绘制当前帧
     *
     * @param time 当前帧时间（单位纳秒）
     */
    void drawFrame(long time);

    SurfaceTexture getSurfaceTexture();
}

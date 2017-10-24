package cn.ezandroid.ezfilter.core;

/**
 * 支持录制接口
 *
 * @author like
 * @date 2017-10-24
 */
public interface ISupportRecord {

    /**
     * 是否正在录制视频
     *
     * @return
     */
    boolean isRecording();

    /**
     * 开始录制
     */
    void startRecording();

    /**
     * 结束录制
     */
    void stopRecording();
}

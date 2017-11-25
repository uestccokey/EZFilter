package cn.ezandroid.ezfilter.core;

/**
 * 支持录制接口
 *
 * @author like
 * @date 2017-10-24
 */
public interface ISupportRecord {

    /**
     * 设置录制输出路径
     *
     * @param outputPath
     */
    default void setRecordOutputPath(String outputPath) {
    }

    /**
     * 音频录制开关
     *
     * @param enable
     */
    default void enableRecordAudio(boolean enable) {
    }

    /**
     * 影像录制开关
     *
     * @param enable
     */
    default void enableRecordVideo(boolean enable) {
    }

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

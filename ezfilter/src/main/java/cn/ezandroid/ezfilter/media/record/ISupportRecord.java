package cn.ezandroid.ezfilter.media.record;

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
     * 设置视频录制宽高
     *
     * @param width
     * @param height
     */
    default void setRecordSize(int width, int height) {
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
    boolean startRecording();

    /**
     * 结束录制
     */
    void stopRecording();
}

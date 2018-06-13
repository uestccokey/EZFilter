package cn.ezandroid.ezfilter.media.record;

/**
 * 视频录制监听器
 *
 * @author like
 * @date 2018-03-21
 */
public interface IRecordListener {

    /**
     * 调用startRecording后回调
     */
    void onStart();

    /**
     * 调用stopRecording后回调
     */
    void onStop();

    /**
     * 当视频完全渲染好后回调
     */
    void onFinish();
}

package cn.ezandroid.ezfilter.media.record;

/**
 * 视频录制监听器
 *
 * @author like
 * @date 2018-03-21
 */
public interface IRecordListener {

    void onStart();

    void onStop();

    void onFinish();
}

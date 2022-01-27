package cn.ezandroid.lib.ezfilter.media.transcode;

import java.io.IOException;

/**
 * 轨道转码器接口
 *
 * @date 2017-08-23
 */
public interface TrackTranscoder {

    void setup() throws IOException;

    boolean stepPipeline();

    boolean isFinished();

    void release();
}

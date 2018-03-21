package cn.ezandroid.ezfilter.media.record;

import java.nio.ByteBuffer;

/**
 * 额外的音频编码器
 *
 * @author like
 * @date 2018-03-21
 */
public interface IAudioExtraEncoder {

    void setup(int channel, int sampleRate, int bytesPerSample);

    ByteBuffer encode(ByteBuffer buffer);

    void release();
}

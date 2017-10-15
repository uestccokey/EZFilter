package cn.ezandroid.ezfilter.video.offscreen.media;

import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * MediaUtil
 *
 * @author like
 * @date 2017-09-23
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class MediaUtil {

    // 兼容低版本
    public static final String KEY_ROTATION = "rotation-degrees";

    public static final String MIME_TYPE_MP4 = "video/avc";
    public static final int FRAME_RATE = 20;
    public static final int I_FRAME_INTERVAL = 1;

    public static final String MIME_TYPE_AAC = "audio/mp4a-latm";
    public static final int AUDIO_BIT_RATE = 96000;

    /**
     * 创建视频MediaFormat[mp4]
     *
     * @param w 视频宽度
     * @param h 视频高度
     * @return
     */
    public static MediaFormat createVideoFormat(int w, int h) {
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE_MP4, w, h);
        // 数据来源
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        // 视频bit率
        format.setInteger(MediaFormat.KEY_BIT_RATE, (int) (0.2 * FRAME_RATE * w * h));
        // 帧率
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        // 设置关键帧时间间隔（单位为秒）表示：每隔多长时间有一个关键帧
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);
        return format;
    }

    /**
     * 创建音频MediaFormat[aac]
     *
     * @param sampleRate 采样率
     * @param channel    声道
     * @return
     */
    public static MediaFormat createAudioFormat(int sampleRate, int channel) {
        MediaFormat format = MediaFormat.createAudioFormat(MIME_TYPE_AAC, sampleRate, channel);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_STEREO);
        // 声道
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channel);
        // 音频bit率
        format.setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BIT_RATE);
        return format;
    }

    /**
     * 读取多媒体第一个视频Track和音频Track
     *
     * @param extractor
     * @return null, if has no video track and audio track.
     */
    public static Track getFirstTrack(MediaExtractor extractor) {
        Track track = new Track();
        track.videoTrackIndex = -1;
        track.audioTrackIndex = -1;
        int trackCount = extractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (track.videoTrackIndex < 0 && mime.startsWith("video/")) {
                track.videoTrackIndex = i;
                track.videoTrackMime = mime;
                track.videoTrackFormat = format;
            } else if (track.audioTrackIndex < 0 && mime.startsWith("audio/")) {
                track.audioTrackIndex = i;
                track.audioTrackMime = mime;
                track.audioTrackFormat = format;
            }
            if (track.videoTrackIndex >= 0 && track.audioTrackIndex >= 0) break;
        }

        if (track.videoTrackIndex < 0 && track.audioTrackIndex < 0) {
            // 视频轨和音轨都没有
            Log.e("ExtractorUtil", "Not found video/audio track.");
            return null;
        } else {
            return track;
        }
    }

    /**
     * 防止出现 http://stackoverflow.com/q/30646885 的问题
     *
     * @param codec
     * @param index
     * @return
     */
    public static ByteBuffer getInputBuffer(MediaCodec codec, int index) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return codec.getInputBuffer(index);
        } else {
            return codec.getInputBuffers()[index];
        }
    }

    /**
     * 防止出现 http://stackoverflow.com/q/30646885 的问题
     *
     * @param codec
     * @param index
     * @return
     */
    public static ByteBuffer getOutputBuffer(MediaCodec codec, int index) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return codec.getOutputBuffer(index);
        } else {
            return codec.getOutputBuffers()[index];
        }
    }

    public static class Track {

        private Track() {
        }

        public int videoTrackIndex;
        public String videoTrackMime;
        public MediaFormat videoTrackFormat;

        public int audioTrackIndex;
        public String audioTrackMime;
        public MediaFormat audioTrackFormat;
    }
}

package cn.ezandroid.ezfilter.media.util;

import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import cn.ezandroid.ezfilter.core.util.NumberUtil;

/**
 * MediaUtil
 *
 * @author like
 * @date 2017-09-23
 */
public class MediaUtil {

    // 兼容低版本
    public static final String KEY_ROTATION = "rotation-degrees";

    public static final String MIME_TYPE_MP4 = "video/avc";
    public static final int FRAME_RATE = 20;
    public static final int I_FRAME_INTERVAL = 1;

    public static final String MIME_TYPE_AAC = "audio/mp4a-latm";
    public static final int AUDIO_BIT_RATE = 96000;

    // 统一配置一个buffer size，避免缓冲区大小不一致导致的各种问题
    public static final int BUFFER_SIZE = 256 * 1024;

    /**
     * 进行整除16对齐
     * <p>
     * MediaCodec这个API在设计的时候，过于贴近HAL层，这在很多Soc的实现上，是直接把传入MediaCodec的buffer，
     * 在不经过任何前置处理的情况下就直接送入了Soc中。 而在编码h264视频流的时候，由于h264的编码块大小一般是16x16，
     * 于是乎在一开始设置视频的宽高的时候，如果设置了一个没有对齐16的大小，例如960x540， 在某些cpu上，
     * 最终编码出来的视频就会直接花屏。
     *
     * @param size
     * @return
     */
    private static int align16(int size) {
        if (size % 16 > 0) {
            size = (size / 16) * 16 + 16;
        }
        return size;
    }

    /**
     * 创建视频MediaFormat[mp4]
     *
     * @param w 视频宽度
     * @param h 视频高度
     * @return
     */
    public static MediaFormat createVideoFormat(int w, int h) {
        return createVideoFormat(align16(w), align16(h), MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
    }

    /**
     * 创建视频MediaFormat[mp4]
     *
     * @param w           视频宽度
     * @param h           视频高度
     * @param colorFormat 颜色格式参考
     *                    {@link MediaCodecInfo.CodecCapabilities#COLOR_FormatYUV420SemiPlanar}
     *                    {@link MediaCodecInfo.CodecCapabilities#COLOR_FormatYUV420Planar}
     *                    {@link MediaCodecInfo.CodecCapabilities#COLOR_FormatSurface}
     * @return
     */
    public static MediaFormat createVideoFormat(int w, int h, int colorFormat) {
        return createVideoFormat(align16(w), align16(h), (int) (0.2 * FRAME_RATE * w * h), colorFormat);
    }

    /**
     * 创建视频MediaFormat[mp4]
     *
     * @param w           视频宽度
     * @param h           视频高度
     * @param bitrate     视频码率
     * @param colorFormat 颜色格式参考
     *                    {@link MediaCodecInfo.CodecCapabilities#COLOR_FormatYUV420SemiPlanar}
     *                    {@link MediaCodecInfo.CodecCapabilities#COLOR_FormatYUV420Planar}
     *                    {@link MediaCodecInfo.CodecCapabilities#COLOR_FormatSurface}
     * @return
     */
    public static MediaFormat createVideoFormat(int w, int h, int bitrate, int colorFormat) {
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE_MP4, w, h);
        // 数据来源
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
        // 视频bit率
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        // 帧率
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        // 设置关键帧时间间隔（单位为秒）表示：每隔多长时间有一个关键帧
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);
        return format;
    }

    /**
     * 创建音频MediaFormat[aac]
     *
     * @param sampleRate   采样率
     * @param channelMask  声道
     * @param channelCount 声道数
     * @return
     */
    public static MediaFormat createAudioFormat(int sampleRate, int channelMask, int channelCount) {
        MediaFormat format = MediaFormat.createAudioFormat(MIME_TYPE_AAC, sampleRate, channelCount);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        // 声道
        format.setInteger(MediaFormat.KEY_CHANNEL_MASK, channelMask);
        // 声道数
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelCount);
        // 音频bit率
        format.setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BIT_RATE);
        // 统一设置最大缓冲容量，否则在音频转码时因为大小不一致会报错
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, BUFFER_SIZE);
        return format;
    }

    /**
     * 读取多媒体第一个视频轨和音频轨
     *
     * @param extractor
     * @return
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
            Log.e("MediaUtil", "Not found video/audio track.");
            return null;
        } else {
            return track;
        }
    }

    /**
     * 解析多媒体文件元数据
     *
     * @param input
     * @return
     */
    public static Metadata getMetadata(String input) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(input);
            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            String mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
            String rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            String tracks = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS);

            Metadata metadata = new Metadata();
            metadata.duration = NumberUtil.parseLong(duration);
            metadata.width = NumberUtil.parseInt(width);
            metadata.height = NumberUtil.parseInt(height);
            metadata.bitrate = NumberUtil.parseInt(bitrate, 1);
            metadata.rotation = NumberUtil.parseInt(rotation);
            metadata.tracks = NumberUtil.parseInt(tracks);
            metadata.mimeType = mimeType;
            return metadata;
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            retriever.release();
        }
        return new Metadata();
    }

    /**
     * 音轨和视频轨
     */
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

    /**
     * 多媒体文件元数据
     */
    public static class Metadata {

        public String mimeType;
        public int width;
        public int height;
        /** 时长，单位ms，会有一定的精度损失 */
        public long duration;
        public int rotation;
        public int tracks;
        public int bitrate;

        @Override
        public String toString() {
            return "Metadata{" +
                    "mimeType='" + mimeType + '\'' +
                    ", width=" + width +
                    ", height=" + height +
                    ", duration=" + duration +
                    ", rotation=" + rotation +
                    ", tracks=" + tracks +
                    ", bitrate=" + bitrate +
                    '}';
        }
    }
}

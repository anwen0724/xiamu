package com.anwen.xiamubackend.utils;

import com.anwen.xiamubackend.exception.BusinessException;
import com.anwen.xiamubackend.exception.ErrorCode;
import com.github.kokorin.jaffree.Rational;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import com.github.kokorin.jaffree.ffprobe.Stream;
import com.github.kokorin.jaffree.StreamType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * FFmpeg 工具类
 * 用于获取视频的元数据信息（时长、格式、分辨率等）
 */
@Slf4j
@Component
public class FFmpegUtil {

    /**
     * 获取视频信息
     * @param videoPath 视频文件路径
     * @return 视频信息
     */
    public VideoInfo getVideoInfo(String videoPath) {
        try {
            FFprobe probe = FFprobe.atPath();
            FFprobeResult result = probe.setInput(videoPath).execute();

            List<Stream> streams = result.getStreams();
            if (streams.isEmpty()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "无法获取视频信息：视频流为空");
            }

            // 查找视频流
            Stream videoStream = streams.stream()
                .filter(s -> s.getCodecType() == StreamType.VIDEO)
                .findFirst()
                .orElse(streams.get(0));

            VideoInfo info = new VideoInfo();

            // 获取时长（秒）
            Float duration = result.getFormat().getDuration();
            if (duration != null) {
                info.setDuration(duration.longValue());
            }

            // 获取格式（编码格式）
            String codec = videoStream.getCodecName();
            if (codec != null) {
                info.setFormat(codec);
            }

            // 获取分辨率
            info.setWidth(videoStream.getWidth());
            info.setHeight(videoStream.getHeight());

            // 获取帧率
            Rational frameRate = videoStream.getRFrameRate();
            if (frameRate != null) {
                double fps = (double) frameRate.getNumerator() / frameRate.getDenominator();
                info.setFrameRate(String.format("%.2f", fps));
            }

            log.info("视频信息获取成功: duration={}s, format={}, {}x{}, fps={}",
                info.getDuration(), info.getFormat(), info.getWidth(), info.getHeight(), info.getFrameRate());

            return info;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取视频信息失败: {}", videoPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取视频信息失败: " + e.getMessage());
        }
    }

    /**
     * 视频信息内部类
     */
    @Data
    public static class VideoInfo {
        /**
         * 时长（秒）
         */
        private Long duration;

        /**
         * 格式（编码格式，如 h264、hevc 等）
         */
        private String format;

        /**
         * 宽度（像素）
         */
        private Integer width;

        /**
         * 高度（像素）
         */
        private Integer height;

        /**
         * 帧率（fps）
         */
        private String frameRate;
    }
}

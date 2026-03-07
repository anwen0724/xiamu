package com.anwen.xiamubackend.utils;

import com.anwen.xiamubackend.exception.BusinessException;
import com.anwen.xiamubackend.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * FFmpeg 工具类
 * 用于获取视频的元数据信息（时长、格式、分辨率等）
 */
@Slf4j
@Component
public class FFmpegUtil {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取视频信息
     * @param videoPath 视频文件路径
     * @return 视频信息
     */
    public VideoInfo getVideoInfo(String videoPath) {
        try {
            log.info("开始获取视频信息: {}", videoPath);

            // 检查文件是否存在
            File videoFile = new File(videoPath);
            if (!videoFile.exists()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "视频文件不存在: " + videoPath);
            }

            // 使用 ProcessBuilder 直接执行 FFprobe 命令
            ProcessBuilder processBuilder = new ProcessBuilder(
                "ffprobe",
                "-loglevel", "quiet",
                "-print_format", "json",
                "-show_streams",
                "-show_format",
                videoPath
            );
            processBuilder.redirectErrorStream(false);

            log.info("执行命令: ffprobe -loglevel quiet -print_format json -show_streams -show_format {}", videoPath);

            Process process = processBuilder.start();

            // 读取标准输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            // 等待进程结束
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                // 读取错误信息
                StringBuilder errorOutput = new StringBuilder();
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorOutput.append(line).append("\n");
                    }
                }
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "FFprobe 执行失败，退出码: " + exitCode + ", 错误信息: " + errorOutput);
            }

            String jsonOutput = output.toString();
            log.info("FFprobe 输出长度: {}", jsonOutput.length());

            if (jsonOutput.isEmpty()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "FFprobe 输出为空");
            }

            // 解析 JSON
            JsonNode rootNode = objectMapper.readTree(jsonOutput);

            // 解析格式信息（时长）
            VideoInfo info = new VideoInfo();
            JsonNode formatNode = rootNode.get("format");
            if (formatNode != null && formatNode.has("duration")) {
                String durationStr = formatNode.get("duration").asText();
                try {
                    info.setDuration((long) Double.parseDouble(durationStr));
                } catch (NumberFormatException e) {
                    log.warn("解析时长失败: {}", durationStr);
                }
            }

            // 解析流信息
            JsonNode streamsNode = rootNode.get("streams");
            if (streamsNode == null || !streamsNode.isArray() || streamsNode.size() == 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "无法获取视频信息：视频流为空");
            }

            // 查找视频流
            JsonNode videoStream = null;
            for (JsonNode stream : streamsNode) {
                if (stream.has("codec_type") && "video".equals(stream.get("codec_type").asText())) {
                    videoStream = stream;
                    break;
                }
            }

            if (videoStream == null) {
                videoStream = streamsNode.get(0);
            }

            // 获取格式（编码格式）
            if (videoStream.has("codec_name")) {
                info.setFormat(videoStream.get("codec_name").asText());
            }

            // 获取分辨率
            if (videoStream.has("width")) {
                info.setWidth(videoStream.get("width").asInt());
            }
            if (videoStream.has("height")) {
                info.setHeight(videoStream.get("height").asInt());
            }

            // 获取帧率
            if (videoStream.has("r_frame_rate")) {
                String fpsStr = videoStream.get("r_frame_rate").asText();
                String[] parts = fpsStr.split("/");
                if (parts.length == 2) {
                    try {
                        double numerator = Double.parseDouble(parts[0]);
                        double denominator = Double.parseDouble(parts[1]);
                        double fps = numerator / denominator;
                        info.setFrameRate(String.format("%.2f", fps));
                    } catch (NumberFormatException e) {
                        log.warn("解析帧率失败: {}", fpsStr);
                    }
                }
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

package com.anwen.xiamubackend.utils;

import com.anwen.xiamubackend.config.StorageConfig;
import com.anwen.xiamubackend.exception.BusinessException;
import com.anwen.xiamubackend.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

/**
 * 音频提取工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AudioExtractUtil {

    private final StorageConfig storageConfig;

    /**
     * 从视频提取音频
     * @param videoPath 视频文件路径
     * @return 音频文件路径
     */
    public String extractAudio(String videoPath) {
        // 生成音频文件路径
        String audioPath = generateAudioPath(videoPath);

        // 确保输出目录存在
        Path outputPath = Paths.get(audioPath);
        File outputDir = outputPath.getParent().toFile();
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        try {
            log.info("开始提取音频: {} -> {}", videoPath, audioPath);

            // 使用 ProcessBuilder 执行 ffmpeg 命令
            ProcessBuilder processBuilder = new ProcessBuilder(
                "ffmpeg",
                "-i", videoPath,                    // 输入文件
                "-vn",                               // 禁用视频
                "-acodec", "libmp3lame",            // 音频编码器
                "-ar", "16000",                      // 采样率 16kHz（Whisper 推荐）
                "-ac", "1",                          // 单声道
                "-ab", "64k",                        // 比特率 64kbps
                "-y",                                // 覆盖输出文件
                audioPath                            // 输出文件
            );

            processBuilder.redirectErrorStream(true);

            log.info("执行命令: ffmpeg -i {} -vn -acodec libmp3lame -ar 16000 -ac 1 -ab 64k -y {}",
                videoPath, audioPath);

            Process process = processBuilder.start();

            // 读取输出（用于日志和进度）
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("ffmpeg 输出: {}", line);
                }
            }

            // 等待进程结束
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "ffmpeg 执行失败，退出码: " + exitCode);
            }

            // 验证输出文件是否存在
            File outputFile = new File(audioPath);
            if (!outputFile.exists()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "音频文件未生成: " + audioPath);
            }

            log.info("音频提取成功: {}, 文件大小: {} bytes", audioPath, outputFile.length());
            return audioPath;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("音频提取失败: {}", videoPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "音频提取失败: " + e.getMessage());
        }
    }

    /**
     * 生成音频文件路径
     * @param videoPath 视频文件路径
     * @return 音频文件路径
     */
    private String generateAudioPath(String videoPath) {
        // 获取视频文件的日期路径（如 2026/03/07）
        LocalDate now = LocalDate.now();
        String datePath = Paths.get(
                String.valueOf(now.getYear()),
                String.format("%02d", now.getMonthValue()),
                String.format("%02d", now.getDayOfMonth())
        ).toString();

        // 生成音频文件名（使用视频文件名的时间戳部分）
        Path videoFileName = Paths.get(videoPath).getFileName();
        String videoName = videoFileName.toString();
        String nameWithoutExt = videoName.substring(0, videoName.lastIndexOf('.'));
        String audioName = "audio_" + nameWithoutExt.replace("video_", "") + ".mp3";

        // 构建完整路径
        return Paths.get(storageConfig.getAudioPath(), datePath, audioName).toString();
    }
}

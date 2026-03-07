package com.anwen.xiamubackend.service.impl;

import com.anwen.xiamubackend.model.enums.VideoStatusEnum;
import com.anwen.xiamubackend.model.entity.Video;
import com.anwen.xiamubackend.mapper.VideoMapper;
import com.anwen.xiamubackend.service.WhisperTranscriptionService;
import com.anwen.xiamubackend.strategy.TranscriptionStrategy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * Whisper 转录服务实现（使用策略模式）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WhisperTranscriptionServiceImpl implements WhisperTranscriptionService {

    private final VideoMapper videoMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final TranscriptionStrategy localModelStrategy;
    private final TranscriptionStrategy apiCallStrategy;

    @Value("${xiamu.whisper.mode:local}")
    private String mode;

    @Async
    @Override
    public void transcribeAsync(Long videoId, String audioPath) {
        log.info("开始异步转写音频: videoId={}, audioPath={}, mode={}", videoId, audioPath, mode);

        // 1. 查询视频信息
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            log.error("视频不存在: videoId={}", videoId);
            return;
        }

        // 2. 检查音频路径
        if (audioPath == null || audioPath.isEmpty()) {
            log.error("音频路径为空: videoId={}", videoId);
            updateTranscriptionFailed(video, "音频路径为空");
            return;
        }

        // 3. 检查音频文件是否存在
        File audioFile = new File(audioPath);
        if (!audioFile.exists()) {
            log.error("音频文件不存在: videoId={}, audioPath={}", videoId, audioPath);
            updateTranscriptionFailed(video, "音频文件不存在");
            return;
        }

        // 4. 更新状态为处理中
        updateStatus(video, VideoStatusEnum.PROCESSING.getCode());

        try {
            // 5. 根据模式选择策略
            TranscriptionStrategy strategy = getStrategy();

            // 6. 执行转写
            String result = strategy.transcribe(audioPath);

            // 7. 解析 JSON 结果
            JsonNode jsonNode = objectMapper.readTree(result);

            // 检查是否有错误
            if (jsonNode.has("error")) {
                String error = jsonNode.get("error").asText();
                throw new Exception(error);
            }

            // 8. 获取转写文本
            String transcript = jsonNode.get("text").asText();

            // 9. 获取语言信息（可选，可用于后续处理）
            String language = jsonNode.has("language") ? jsonNode.get("language").asText() : "";

            log.info("转写成功: videoId={}, language={}, textLength={}", videoId, language, transcript.length());

            // 10. 更新数据库
            video.setTranscript(transcript);
            video.setStatus(VideoStatusEnum.WAITING.getCode());  // 转写成功，等待下一步 AI 分析
            videoMapper.updateById(video);

        } catch (Exception e) {
            log.error("转写失败: videoId={}", videoId, e);
            updateTranscriptionFailed(video, "转写失败: " + e.getMessage());
        }
    }

    /**
     * 根据配置获取策略
     * @return 转录策略
     */
    private TranscriptionStrategy getStrategy() {
        switch (mode.toLowerCase()) {
            case "api":
                log.info("使用 API 调用策略");
                return apiCallStrategy;
            case "local":
            default:
                log.info("使用本地模型策略");
                return localModelStrategy;
        }
    }

    /**
     * 更新状态
     */
    private void updateStatus(Video video, String status) {
        video.setStatus(status);
        videoMapper.updateById(video);
    }

    /**
     * 更新转写失败
     */
    private void updateTranscriptionFailed(Video video, String errorMessage) {
        video.setStatus(VideoStatusEnum.FAILED.getCode());
        video.setErrorMessage(errorMessage);
        videoMapper.updateById(video);
    }
}

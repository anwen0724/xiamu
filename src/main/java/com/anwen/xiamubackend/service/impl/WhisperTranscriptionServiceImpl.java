package com.anwen.xiamubackend.service.impl;

import com.anwen.xiamubackend.ai.service.AiSummaryService;
import com.anwen.xiamubackend.mapper.VideoMapper;
import com.anwen.xiamubackend.model.entity.Video;
import com.anwen.xiamubackend.model.enums.VideoStatusEnum;
import com.anwen.xiamubackend.service.WhisperTranscriptionService;
import com.anwen.xiamubackend.strategy.TranscriptionStrategy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * Whisper 转写服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WhisperTranscriptionServiceImpl implements WhisperTranscriptionService {

    private final VideoMapper videoMapper;
    private final AiSummaryService aiSummaryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final TranscriptionStrategy localModelStrategy;
    private final TranscriptionStrategy apiCallStrategy;

    @Value("${xiamu.whisper.mode:local}")
    private String mode;

    @Async
    @Override
    public void transcribeAsync(Long videoId, String audioPath) {
        log.info("开始异步转写音频: videoId={}, audioPath={}, mode={}", videoId, audioPath, mode);

        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            log.error("视频不存在: videoId={}", videoId);
            return;
        }

        if (audioPath == null || audioPath.isEmpty()) {
            log.error("音频路径为空: videoId={}", videoId);
            updateFailed(video, "音频路径为空");
            return;
        }

        File audioFile = new File(audioPath);
        if (!audioFile.exists()) {
            log.error("音频文件不存在: videoId={}, audioPath={}", videoId, audioPath);
            updateFailed(video, "音频文件不存在");
            return;
        }

        updateStatus(video, VideoStatusEnum.PROCESSING.getCode());

        try {
            TranscriptionStrategy strategy = getStrategy();
            String result = strategy.transcribe(audioPath);
            JsonNode jsonNode = objectMapper.readTree(result);

            if (jsonNode.has("error")) {
                throw new IllegalStateException(jsonNode.get("error").asText());
            }

            String transcript = jsonNode.path("text").asText("");
            String language = jsonNode.path("language").asText("");

            if (transcript.trim().isEmpty()) {
                throw new IllegalStateException("转写结果为空");
            }

            log.info("转写成功: videoId={}, language={}, textLength={}", videoId, language, transcript.length());

            video.setTranscript(transcript);
            video.setStatus(VideoStatusEnum.PROCESSING.getCode());
            videoMapper.updateById(video);

            String summary = aiSummaryService.generateSummary(transcript);
            video.setSummary(summary);
            video.setStatus(VideoStatusEnum.SUCCESS.getCode());
            video.setErrorMessage(null);
            videoMapper.updateById(video);

            log.info("摘要生成成功: videoId={}, summaryLength={}", videoId, summary.length());
        } catch (Exception e) {
            log.error("转写或摘要生成失败: videoId={}", videoId, e);
            updateFailed(video, "转写或摘要生成失败: " + e.getMessage());
        }
    }

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

    private void updateStatus(Video video, String status) {
        video.setStatus(status);
        videoMapper.updateById(video);
    }

    private void updateFailed(Video video, String errorMessage) {
        video.setStatus(VideoStatusEnum.FAILED.getCode());
        video.setErrorMessage(errorMessage);
        videoMapper.updateById(video);
    }
}

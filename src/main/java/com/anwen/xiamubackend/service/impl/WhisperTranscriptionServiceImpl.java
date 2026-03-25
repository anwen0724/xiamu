package com.anwen.xiamubackend.service.impl;

import com.anwen.xiamubackend.ai.service.AiChapterService;
import com.anwen.xiamubackend.ai.service.AiSummaryService;
import com.anwen.xiamubackend.mapper.VideoMapper;
import com.anwen.xiamubackend.model.entity.Video;
import com.anwen.xiamubackend.model.enums.VideoProcessStageEnum;
import com.anwen.xiamubackend.model.vo.VideoChapterVO;
import com.anwen.xiamubackend.service.VideoProcessStateService;
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
import java.util.List;

/**
 * Whisper 转写服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WhisperTranscriptionServiceImpl implements WhisperTranscriptionService {

    private final VideoMapper videoMapper;
    private final AiSummaryService aiSummaryService;
    private final AiChapterService aiChapterService;
    private final ObjectMapper objectMapper;
    private final VideoProcessStateService videoProcessStateService;
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
            updateFailed(videoId, VideoProcessStageEnum.TRANSCRIBING, "音频路径为空");
            return;
        }

        File audioFile = new File(audioPath);
        if (!audioFile.exists()) {
            log.error("音频文件不存在: videoId={}, audioPath={}", videoId, audioPath);
            updateFailed(videoId, VideoProcessStageEnum.TRANSCRIBING, "音频文件不存在");
            return;
        }

        VideoProcessStageEnum currentStage = VideoProcessStageEnum.TRANSCRIBING;
        videoProcessStateService.updateProcessingStage(videoId, currentStage, null);

        try {
            TranscriptionStrategy strategy = getStrategy();
            String result = strategy.transcribe(audioPath);
            JsonNode jsonNode = objectMapper.readTree(result);

            if (jsonNode.has("error")) {
                throw new IllegalStateException(jsonNode.get("error").asText());
            }

            String transcript = jsonNode.path("text").asText("");
            String language = jsonNode.path("language").asText("");
            String segmentsJson = jsonNode.path("segments").toString();

            if (transcript.trim().isEmpty()) {
                throw new IllegalStateException("转写结果为空");
            }

            log.info("转写成功: videoId={}, language={}, textLength={}", videoId, language, transcript.length());

            currentStage = VideoProcessStageEnum.GENERATING_SUMMARY;
            videoProcessStateService.updateProcessingStage(videoId, currentStage, currentVideo ->
                    currentVideo.setTranscript(transcript));

            String summary = aiSummaryService.generateSummary(transcript);

            currentStage = VideoProcessStageEnum.GENERATING_CHAPTERS;
            videoProcessStateService.updateProcessingStage(videoId, currentStage, currentVideo ->
                    currentVideo.setSummary(summary));

            List<VideoChapterVO> chapters = List.of();
            boolean chapterGenerated = false;
            try {
                chapters = aiChapterService.generateChapters(transcript, segmentsJson, video.getDuration());
                chapterGenerated = true;
            } catch (Exception chapterException) {
                log.warn("章节生成失败，降级为仅摘要成功状态: videoId={}, message={}",
                        videoId, chapterException.getMessage(), chapterException);
            }

            String chaptersJson = objectMapper.writeValueAsString(chapters);
            videoProcessStateService.markSuccess(videoId, currentVideo -> currentVideo.setChapters(chaptersJson));

            if (chapterGenerated) {
                log.info("摘要和章节生成成功: videoId={}, summaryLength={}, chapterCount={}",
                        videoId, summary.length(), chapters.size());
            } else {
                log.info("摘要生成成功，章节降级为空列表: videoId={}, summaryLength={}", videoId, summary.length());
            }
        } catch (Exception e) {
            log.error("转写或摘要生成失败: videoId={}", videoId, e);
            updateFailed(videoId, currentStage, "转写或 AI 内容生成失败: " + e.getMessage());
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

    private void updateFailed(Long videoId, VideoProcessStageEnum stage, String errorMessage) {
        videoProcessStateService.markFailed(videoId, stage, errorMessage);
    }
}

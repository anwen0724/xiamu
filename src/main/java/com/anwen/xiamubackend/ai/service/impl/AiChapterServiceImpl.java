package com.anwen.xiamubackend.ai.service.impl;

import com.anwen.xiamubackend.ai.config.AiProperties;
import com.anwen.xiamubackend.ai.routing.AiModelRegistration;
import com.anwen.xiamubackend.ai.routing.AiModelRouter;
import com.anwen.xiamubackend.ai.routing.AiTaskType;
import com.anwen.xiamubackend.ai.service.AiChapterService;
import com.anwen.xiamubackend.model.vo.VideoChapterVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 基于 Spring AI 的章节生成服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChapterServiceImpl implements AiChapterService {

    private static final int SHORT_VIDEO_MAX_SECONDS = 180;
    private static final int SHORT_TRANSCRIPT_MAX_CHARS = 400;
    private static final int FALLBACK_SUMMARY_MAX_CHARS = 80;
    private static final int MAX_LOG_CONTENT_CHARS = 1000;
    private static final String FALLBACK_TITLE = "完整内容";

    private static final String SYSTEM_PROMPT = """
            你是一名帮助学习者整理课程视频结构的中文助手。
            你会根据视频转写文本和分段时间信息，输出适合结果页展示的章节列表。
            请严格遵守以下要求：
            1. 仅输出 JSON 数组，不要输出 Markdown、解释或额外文字。
            2. 每个章节对象固定包含 title、startSeconds、endSeconds、summary 四个字段。
            3. startSeconds 和 endSeconds 必须是整数，且 startSeconds < endSeconds。
            4. 如果视频整体较短或内容较少，可以输出 1 到 3 个章节；其它情况控制在 3 到 8 个之间。
            5. title 要简洁、可读，summary 用一句中文概括该章节重点。
            6. 不能编造超出转写文本的信息。
            """;

    private final AiProperties aiProperties;
    private final AiModelRouter aiModelRouter;
    private final ObjectMapper objectMapper;

    @Override
    public List<VideoChapterVO> generateChapters(String transcript, String transcriptionJson, Long durationSeconds) {
        return generateChapters(transcript, transcriptionJson, durationSeconds, null);
    }

    @Override
    public List<VideoChapterVO> generateChapters(String transcript, String transcriptionJson, Long durationSeconds, String modelKey) {
        String normalizedTranscript = normalizeTranscript(transcript);
        String truncatedTranscript = truncateTranscript(normalizedTranscript);
        String normalizedSegments = truncateSegments(transcriptionJson);
        AiModelRegistration registration = aiModelRouter.route(AiTaskType.CHAPTER, modelKey);

        String content = registration.chatClient()
                .prompt()
                .system(SYSTEM_PROMPT)
                .user("""
                        视频总时长（秒）：%s

                        转写全文：
                        %s

                        转写分段 JSON：
                        %s
                        """.formatted(durationSeconds == null ? 0 : durationSeconds, truncatedTranscript, normalizedSegments))
                .call()
                .content();

        log.info("开始调用 LLM 生成章节，transcriptLength={}, segmentLength={}, modelKey={}, provider={}, model={}",
                truncatedTranscript.length(), normalizedSegments.length(), registration.key(), registration.provider(), registration.model());

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalStateException("LLM 返回的章节内容为空");
        }

        String json;
        try {
            json = extractJsonArray(content);
        } catch (Exception e) {
            return fallbackOrThrow(truncatedTranscript, durationSeconds, content, "章节结果中未找到 JSON 数组", e);
        }

        List<VideoChapterVO> chapters;
        try {
            chapters = objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return fallbackOrThrow(truncatedTranscript, durationSeconds, content, "章节 JSON 解析失败: " + e.getMessage(), e);
        }

        List<VideoChapterVO> normalizedChapters = normalizeChapters(chapters, durationSeconds);
        if (normalizedChapters.isEmpty()) {
            return fallbackOrThrow(truncatedTranscript, durationSeconds, content, "未生成有效章节", null);
        }
        return normalizedChapters;
    }

    private List<VideoChapterVO> fallbackOrThrow(String transcript,
                                                 Long durationSeconds,
                                                 String rawContent,
                                                 String reason,
                                                 Exception cause) {
        log.warn("章节结果不可用，reason={}, rawContent={}", reason, abbreviateForLog(rawContent), cause);

        if (shouldUseSingleChapterFallback(transcript, durationSeconds)) {
            List<VideoChapterVO> fallbackChapters = List.of(buildSingleChapter(transcript, durationSeconds));
            log.warn("章节生成降级为单章节兜底，durationSeconds={}, transcriptLength={}, chapterCount={}",
                    durationSeconds, transcript.length(), fallbackChapters.size());
            return fallbackChapters;
        }

        if (cause == null) {
            throw new IllegalStateException(reason);
        }
        throw new IllegalStateException(reason, cause);
    }

    private boolean shouldUseSingleChapterFallback(String transcript, Long durationSeconds) {
        if (transcript.length() <= SHORT_TRANSCRIPT_MAX_CHARS) {
            return true;
        }
        return durationSeconds != null && durationSeconds > 0 && durationSeconds <= SHORT_VIDEO_MAX_SECONDS;
    }

    private VideoChapterVO buildSingleChapter(String transcript, Long durationSeconds) {
        VideoChapterVO chapter = new VideoChapterVO();
        chapter.setTitle(FALLBACK_TITLE);
        chapter.setStartSeconds(0);
        chapter.setEndSeconds(resolveFallbackEndSeconds(durationSeconds));
        chapter.setSummary(buildFallbackSummary(transcript));
        return chapter;
    }

    private int resolveFallbackEndSeconds(Long durationSeconds) {
        if (durationSeconds == null || durationSeconds <= 0) {
            return 60;
        }
        return Math.max(1, durationSeconds.intValue());
    }

    private String buildFallbackSummary(String transcript) {
        String normalized = transcript.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= FALLBACK_SUMMARY_MAX_CHARS) {
            return normalized;
        }
        return normalized.substring(0, FALLBACK_SUMMARY_MAX_CHARS) + "...";
    }

    private String abbreviateForLog(String content) {
        if (content == null) {
            return "null";
        }
        if (content.length() <= MAX_LOG_CONTENT_CHARS) {
            return content;
        }
        return content.substring(0, MAX_LOG_CONTENT_CHARS) + "...(truncated)";
    }

    private String normalizeTranscript(String transcript) {
        if (transcript == null || transcript.trim().isEmpty()) {
            throw new IllegalArgumentException("转写文本为空，无法生成章节");
        }
        return transcript.replace("\r", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String truncateTranscript(String transcript) {
        int maxChars = aiProperties.getChapterTranscriptMaxChars() == null ? 12000 : aiProperties.getChapterTranscriptMaxChars();
        if (transcript.length() <= maxChars) {
            return transcript;
        }
        log.warn("章节生成转写文本过长，已截断：originalLength={}, maxChars={}", transcript.length(), maxChars);
        return transcript.substring(0, maxChars);
    }

    private String truncateSegments(String transcriptionJson) {
        if (transcriptionJson == null) {
            return "[]";
        }
        int maxChars = aiProperties.getChapterSegmentsMaxChars() == null ? 16000 : aiProperties.getChapterSegmentsMaxChars();
        if (transcriptionJson.length() <= maxChars) {
            return transcriptionJson;
        }
        log.warn("章节生成分段 JSON 过长，已截断：originalLength={}, maxChars={}", transcriptionJson.length(), maxChars);
        return transcriptionJson.substring(0, maxChars);
    }

    private String extractJsonArray(String content) {
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        if (start < 0 || end <= start) {
            throw new IllegalStateException("章节结果中未找到 JSON 数组");
        }
        return content.substring(start, end + 1);
    }

    private List<VideoChapterVO> normalizeChapters(List<VideoChapterVO> chapters, Long durationSeconds) {
        List<VideoChapterVO> normalized = new ArrayList<>();
        for (VideoChapterVO chapter : chapters) {
            if (chapter == null || chapter.getStartSeconds() == null || chapter.getEndSeconds() == null) {
                continue;
            }
            if (chapter.getTitle() == null || chapter.getTitle().trim().isEmpty()) {
                continue;
            }

            chapter.setTitle(chapter.getTitle().trim());
            chapter.setSummary(chapter.getSummary() == null ? "" : chapter.getSummary().trim());
            chapter.setStartSeconds(Math.max(0, chapter.getStartSeconds()));

            if (durationSeconds != null && durationSeconds > 0 && chapter.getEndSeconds() > durationSeconds) {
                chapter.setEndSeconds(durationSeconds.intValue());
            }
            if (chapter.getEndSeconds() <= chapter.getStartSeconds()) {
                continue;
            }
            normalized.add(chapter);
        }
        normalized.sort(Comparator.comparing(VideoChapterVO::getStartSeconds));
        return normalized;
    }
}

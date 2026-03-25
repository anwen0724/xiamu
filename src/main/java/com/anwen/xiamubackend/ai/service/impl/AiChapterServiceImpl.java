package com.anwen.xiamubackend.ai.service.impl;

import com.anwen.xiamubackend.ai.service.AiChapterService;
import com.anwen.xiamubackend.model.vo.VideoChapterVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
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

    private static final String SYSTEM_PROMPT = """
            你是一名帮助学习者整理课程视频结构的中文助手。
            你会根据视频转写文本和分段时间信息，输出适合结果页展示的章节列表。
            请严格遵守以下要求：
            1. 仅输出 JSON 数组，不要输出 Markdown、解释或额外文字。
            2. 每个章节对象固定包含 title、startSeconds、endSeconds、summary 四个字段。
            3. startSeconds 和 endSeconds 必须是整数，且 startSeconds < endSeconds。
            4. 章节按时间顺序排列，数量控制在 3 到 8 个之间。
            5. title 要简洁、可读，summary 用一句中文概括该章节重点。
            6. 不能编造超出转写文本的信息。
            """;

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${xiamu.ai.chapter-transcript-max-chars:12000}")
    private Integer chapterTranscriptMaxChars;

    @Value("${xiamu.ai.chapter-segments-max-chars:16000}")
    private Integer chapterSegmentsMaxChars;

    @Override
    public List<VideoChapterVO> generateChapters(String transcript, String transcriptionJson, Long durationSeconds) {
        String normalizedTranscript = normalizeTranscript(transcript);
        String truncatedTranscript = truncateTranscript(normalizedTranscript);
        String normalizedSegments = truncateSegments(transcriptionJson);

        String content = chatClientBuilder.build()
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

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalStateException("LLM 返回的章节内容为空");
        }

        String json = extractJsonArray(content);
        List<VideoChapterVO> chapters;
        try {
            chapters = objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("章节 JSON 解析失败: " + e.getMessage(), e);
        }

        List<VideoChapterVO> normalizedChapters = normalizeChapters(chapters, durationSeconds);
        if (normalizedChapters.isEmpty()) {
            throw new IllegalStateException("未生成有效章节");
        }
        return normalizedChapters;
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
        int maxChars = chapterTranscriptMaxChars == null ? 12000 : chapterTranscriptMaxChars;
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
        int maxChars = chapterSegmentsMaxChars == null ? 16000 : chapterSegmentsMaxChars;
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
            if (chapter.getEndSeconds() <= chapter.getStartSeconds()) {
                continue;
            }
            if (chapter.getTitle() == null || chapter.getTitle().trim().isEmpty()) {
                continue;
            }
            chapter.setTitle(chapter.getTitle().trim());
            chapter.setSummary(chapter.getSummary() == null ? "" : chapter.getSummary().trim());
            if (chapter.getStartSeconds() < 0) {
                chapter.setStartSeconds(0);
            }
            if (durationSeconds != null && durationSeconds > 0 && chapter.getEndSeconds() > durationSeconds) {
                chapter.setEndSeconds(durationSeconds.intValue());
            }
            normalized.add(chapter);
        }
        normalized.sort(Comparator.comparing(VideoChapterVO::getStartSeconds));
        return normalized;
    }
}

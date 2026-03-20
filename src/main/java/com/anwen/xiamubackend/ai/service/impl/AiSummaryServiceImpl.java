package com.anwen.xiamubackend.ai.service.impl;

import com.anwen.xiamubackend.ai.config.AiConfig;
import com.anwen.xiamubackend.ai.service.AiSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * 基于 Spring AI 的摘要服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiSummaryServiceImpl implements AiSummaryService {

    private static final String SYSTEM_PROMPT = """
            你是一名帮助学习者整理长视频内容的中文学习助手。
            你的任务是根据转写文本生成简洁、准确、可读的中文摘要。
            请遵守以下要求：
            1. 不要编造原文中不存在的信息。
            2. 先给出一段 2 到 4 句的整体概述。
            3. 再给出 3 到 5 条关键要点，每条单独一行，以“- ”开头。
            4. 输出保持简洁，避免空话和套话。
            5. 如果转写文本质量一般，也尽量提炼主要内容。
            """;

    private final ChatClient.Builder chatClientBuilder;
    private final AiConfig aiConfig;

    @Override
    public String generateSummary(String transcript) {
        String normalizedTranscript = normalizeTranscript(transcript);
        String input = truncateTranscript(normalizedTranscript);

        log.info("开始调用 LLM 生成摘要，输入长度={}", input.length());

        String summary = chatClientBuilder.build()
                .prompt()
                .system(SYSTEM_PROMPT)
                .user("""
                        请根据以下视频转写文本生成摘要：

                        %s
                        """.formatted(input))
                .call()
                .content();

        if (summary == null || summary.trim().isEmpty()) {
            throw new IllegalStateException("LLM 返回的摘要为空");
        }

        return summary.trim();
    }

    private String normalizeTranscript(String transcript) {
        if (transcript == null) {
            throw new IllegalArgumentException("转写文本不能为空");
        }

        String normalized = transcript
                .replace("\r", " ")
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("转写文本为空，无法生成摘要");
        }

        return normalized;
    }

    private String truncateTranscript(String transcript) {
        int maxChars = aiConfig.getSummaryMaxChars() == null ? 12000 : aiConfig.getSummaryMaxChars();
        if (transcript.length() <= maxChars) {
            return transcript;
        }

        log.warn("转写文本过长，已截断：originalLength={}, maxChars={}", transcript.length(), maxChars);
        return transcript.substring(0, maxChars);
    }
}

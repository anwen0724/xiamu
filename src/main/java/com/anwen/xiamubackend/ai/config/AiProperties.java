package com.anwen.xiamubackend.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 相关配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "xiamu.ai")
public class AiProperties {

    /**
     * 摘要输入最大字符数，避免单次请求过长。
     */
    private Integer summaryMaxChars = 12000;

    /**
     * 默认模型 key。
     */
    private String defaultModel = "openai";

    /**
     * 摘要任务默认模型 key。
     */
    private String summaryModel;

    /**
     * 章节任务默认模型 key。
     */
    private String chapterModel;

    /**
     * 章节生成时转写正文的最大长度。
     */
    private Integer chapterTranscriptMaxChars = 12000;

    /**
     * 章节生成时分段 JSON 的最大长度。
     */
    private Integer chapterSegmentsMaxChars = 16000;

    /**
     * 多模型配置。
     */
    private Map<String, AiModelProperties> models = new LinkedHashMap<>();
}

package com.anwen.xiamubackend.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 相关配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "xiamu.ai")
public class AiConfig {

    /**
     * 摘要输入最大字符数，避免单次请求过长。
     */
    private Integer summaryMaxChars = 12000;
}

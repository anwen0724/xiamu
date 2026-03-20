package com.anwen.xiamubackend.ai.service;

/**
 * AI 摘要服务。
 */
public interface AiSummaryService {

    /**
     * 根据转写文本生成摘要。
     * @param transcript 转写文本
     * @return 摘要结果
     */
    String generateSummary(String transcript);
}

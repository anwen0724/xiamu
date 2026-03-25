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

    /**
     * 根据转写文本生成摘要，并显式指定模型 key。
     *
     * @param transcript 转写文本
     * @param modelKey   模型 key
     * @return 摘要结果
     */
    String generateSummary(String transcript, String modelKey);
}

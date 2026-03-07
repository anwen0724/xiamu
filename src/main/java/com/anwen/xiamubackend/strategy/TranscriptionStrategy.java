package com.anwen.xiamubackend.strategy;

/**
 * 转录策略接口
 */
public interface TranscriptionStrategy {

    /**
     * 转写音频
     * @param audioPath 音频文件路径
     * @return 转写结果（JSON 格式）
     * @throws Exception 转写失败时抛出异常
     */
    String transcribe(String audioPath) throws Exception;
}

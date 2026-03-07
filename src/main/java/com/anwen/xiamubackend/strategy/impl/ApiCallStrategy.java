package com.anwen.xiamubackend.strategy.impl;

import com.anwen.xiamubackend.strategy.TranscriptionStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * API 调用转录策略（预留）
 */
@Slf4j
@Component
public class ApiCallStrategy implements TranscriptionStrategy {

    // TODO: 实现 API 调用逻辑
    // 1. 配置 API Key
    // 2. 调用 OpenAI Whisper API
    // 3. 处理返回结果

    @Override
    public String transcribe(String audioPath) throws Exception {
        log.warn("API 调用方式暂未实现");
        throw new Exception("API 调用方式暂未实现");
    }
}

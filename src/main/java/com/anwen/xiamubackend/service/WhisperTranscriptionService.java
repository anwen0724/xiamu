package com.anwen.xiamubackend.service;

/**
 * Whisper 转录服务接口
 */
public interface WhisperTranscriptionService {

    /**
     * 异步转写音频
     * @param videoId 视频ID
     * @param audioPath 音频文件路径
     */
    void transcribeAsync(Long videoId, String audioPath);
}

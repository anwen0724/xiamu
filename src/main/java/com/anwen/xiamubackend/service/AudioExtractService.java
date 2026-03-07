package com.anwen.xiamubackend.service;

/**
 * 音频提取服务接口
 */
public interface AudioExtractService {

    /**
     * 异步提取音频
     * @param videoId 视频ID
     */
    void extractAudioAsync(Long videoId);
}

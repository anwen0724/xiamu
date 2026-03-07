package com.anwen.xiamubackend.service.impl;

import com.anwen.xiamubackend.model.enums.VideoStatusEnum;
import com.anwen.xiamubackend.model.entity.Video;
import com.anwen.xiamubackend.mapper.VideoMapper;
import com.anwen.xiamubackend.service.AudioExtractService;
import com.anwen.xiamubackend.service.WhisperTranscriptionService;
import com.anwen.xiamubackend.utils.AudioExtractUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 音频提取服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AudioExtractServiceImpl implements AudioExtractService {

    private final VideoMapper videoMapper;
    private final AudioExtractUtil audioExtractUtil;
    private final WhisperTranscriptionService whisperTranscriptionService;

    /**
     * 异步提取音频
     * @param videoId 视频ID
     */
    @Async
    @Override
    public void extractAudioAsync(Long videoId) {
        log.info("开始异步提取音频: videoId={}", videoId);

        // 1. 查询视频信息
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            log.error("视频不存在: videoId={}", videoId);
            return;
        }

        // 2. 检查视频路径
        if (video.getUrl() == null || video.getUrl().isEmpty()) {
            log.error("视频路径为空: videoId={}", videoId);
            updateExtractFailed(video, "视频路径为空");
            return;
        }

        // 3. 更新状态为处理中
        updateStatus(video, VideoStatusEnum.PROCESSING.getCode());

        try {
            // 4. 提取音频
            String audioPath = audioExtractUtil.extractAudio(video.getUrl());

            // 5. 更新数据库
            video.setAudioPath(audioPath);
            video.setStatus(VideoStatusEnum.WAITING.getCode());  // 提取成功，等待下一步 ASR
            videoMapper.updateById(video);

            log.info("音频提取成功: videoId={}, audioPath={}", videoId, audioPath);

            // 6. 触发 ASR 转写
            whisperTranscriptionService.transcribeAsync(videoId, audioPath);

        } catch (Exception e) {
            log.error("音频提取失败: videoId={}", videoId, e);
            updateExtractFailed(video, "音频提取失败: " + e.getMessage());
        }
    }

    /**
     * 更新状态
     * @param video 视频实体
     * @param status 状态
     */
    private void updateStatus(Video video, String status) {
        video.setStatus(status);
        videoMapper.updateById(video);
    }

    /**
     * 更新提取失败
     * @param video 视频实体
     * @param errorMessage 错误信息
     */
    private void updateExtractFailed(Video video, String errorMessage) {
        video.setStatus(VideoStatusEnum.FAILED.getCode());
        video.setErrorMessage(errorMessage);
        videoMapper.updateById(video);
    }
}

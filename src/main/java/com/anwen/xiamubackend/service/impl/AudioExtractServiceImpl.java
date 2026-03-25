package com.anwen.xiamubackend.service.impl;

import com.anwen.xiamubackend.mapper.VideoMapper;
import com.anwen.xiamubackend.model.entity.Video;
import com.anwen.xiamubackend.model.enums.VideoProcessStageEnum;
import com.anwen.xiamubackend.service.AudioExtractService;
import com.anwen.xiamubackend.service.VideoProcessStateService;
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
    private final VideoProcessStateService videoProcessStateService;

    /**
     * 异步提取音频
     *
     * @param videoId 视频 ID
     */
    @Async
    @Override
    public void extractAudioAsync(Long videoId) {
        log.info("开始异步提取音频: videoId={}", videoId);

        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            log.error("视频不存在: videoId={}", videoId);
            return;
        }

        if (video.getUrl() == null || video.getUrl().isEmpty()) {
            log.error("视频路径为空: videoId={}", videoId);
            updateExtractFailed(videoId, "视频路径为空");
            return;
        }

        videoProcessStateService.updateProcessingStage(videoId, VideoProcessStageEnum.EXTRACTING_AUDIO, null);

        try {
            String audioPath = audioExtractUtil.extractAudio(video.getUrl());

            video.setAudioPath(audioPath);
            videoMapper.updateById(video);

            log.info("音频提取成功: videoId={}, audioPath={}", videoId, audioPath);

            whisperTranscriptionService.transcribeAsync(videoId, audioPath);
        } catch (Exception e) {
            log.error("音频提取失败: videoId={}", videoId, e);
            updateExtractFailed(videoId, "音频提取失败: " + e.getMessage());
        }
    }

    private void updateExtractFailed(Long videoId, String errorMessage) {
        videoProcessStateService.markFailed(videoId, VideoProcessStageEnum.EXTRACTING_AUDIO, errorMessage);
    }
}

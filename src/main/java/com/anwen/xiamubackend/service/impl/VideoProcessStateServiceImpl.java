package com.anwen.xiamubackend.service.impl;

import com.anwen.xiamubackend.mapper.VideoMapper;
import com.anwen.xiamubackend.model.entity.Video;
import com.anwen.xiamubackend.model.enums.VideoProcessStageEnum;
import com.anwen.xiamubackend.model.enums.VideoStatusEnum;
import com.anwen.xiamubackend.model.vo.VideoProcessStatusVO;
import com.anwen.xiamubackend.service.VideoProcessSseService;
import com.anwen.xiamubackend.service.VideoProcessStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.function.Consumer;

/**
 * 视频处理状态更新服务实现。
 */
@Service
@RequiredArgsConstructor
public class VideoProcessStateServiceImpl implements VideoProcessStateService {

    private final VideoMapper videoMapper;
    private final VideoProcessSseService videoProcessSseService;

    @Override
    public Video updateQueued(Long videoId, Consumer<Video> mutator) {
        return updateVideo(videoId, video -> {
            video.setStatus(VideoStatusEnum.WAITING.getCode());
            video.setStage(VideoProcessStageEnum.QUEUED.getCode());
            video.setStageMessage(VideoProcessStageEnum.QUEUED.getDesc());
            video.setErrorMessage(null);
            applyMutator(video, mutator);
        });
    }

    @Override
    public Video updateProcessingStage(Long videoId, VideoProcessStageEnum stage, Consumer<Video> mutator) {
        return updateVideo(videoId, video -> {
            video.setStatus(VideoStatusEnum.PROCESSING.getCode());
            video.setStage(stage.getCode());
            video.setStageMessage(stage.getDesc());
            video.setErrorMessage(null);
            applyMutator(video, mutator);
        });
    }

    @Override
    public Video markSuccess(Long videoId, Consumer<Video> mutator) {
        return updateVideo(videoId, video -> {
            video.setStatus(VideoStatusEnum.SUCCESS.getCode());
            video.setStage(VideoProcessStageEnum.COMPLETED.getCode());
            video.setStageMessage(VideoProcessStageEnum.COMPLETED.getDesc());
            video.setErrorMessage(null);
            applyMutator(video, mutator);
        });
    }

    @Override
    public Video markFailed(Long videoId, VideoProcessStageEnum stage, String errorMessage) {
        return updateVideo(videoId, video -> {
            video.setStatus(VideoStatusEnum.FAILED.getCode());
            video.setStage(stage == null ? VideoProcessStageEnum.FAILED.getCode() : stage.getCode());
            video.setStageMessage(stage == null ? VideoProcessStageEnum.FAILED.getDesc() : stage.getDesc());
            video.setErrorMessage(errorMessage);
        });
    }

    @Override
    public VideoProcessStatusVO toStatusVO(Video video) {
        VideoProcessStatusVO statusVO = new VideoProcessStatusVO();
        statusVO.setId(video.getId());
        statusVO.setTitle(video.getTitle());
        statusVO.setDuration(video.getDuration());
        statusVO.setStatus(video.getStatus());
        statusVO.setStage(video.getStage());
        statusVO.setStageMessage(video.getStageMessage());
        statusVO.setErrorMessage(video.getErrorMessage());
        statusVO.setUpdateTime(video.getUpdateTime());
        statusVO.setHasTranscript(StringUtils.hasText(video.getTranscript()));
        statusVO.setHasSummary(StringUtils.hasText(video.getSummary()));
        statusVO.setHasChapters(hasAvailableChapters(video.getChapters()));
        return statusVO;
    }

    private boolean hasAvailableChapters(String chapters) {
        if (!StringUtils.hasText(chapters)) {
            return false;
        }
        return !"[]".equals(chapters.trim());
    }

    private Video updateVideo(Long videoId, Consumer<Video> mutator) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            throw new IllegalStateException("视频不存在: " + videoId);
        }
        mutator.accept(video);
        video.setUpdateTime(LocalDateTime.now());
        videoMapper.updateById(video);
        videoProcessSseService.publish(toStatusVO(video));
        return video;
    }

    private void applyMutator(Video video, Consumer<Video> mutator) {
        if (mutator != null) {
            mutator.accept(video);
        }
    }
}

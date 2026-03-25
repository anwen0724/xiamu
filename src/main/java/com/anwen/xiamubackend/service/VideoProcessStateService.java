package com.anwen.xiamubackend.service;

import com.anwen.xiamubackend.model.entity.Video;
import com.anwen.xiamubackend.model.enums.VideoProcessStageEnum;
import com.anwen.xiamubackend.model.vo.VideoProcessStatusVO;

import java.util.function.Consumer;

/**
 * 视频处理状态更新服务。
 */
public interface VideoProcessStateService {

    Video updateQueued(Long videoId, Consumer<Video> mutator);

    Video updateProcessingStage(Long videoId, VideoProcessStageEnum stage, Consumer<Video> mutator);

    Video markSuccess(Long videoId, Consumer<Video> mutator);

    Video markFailed(Long videoId, VideoProcessStageEnum stage, String errorMessage);

    VideoProcessStatusVO toStatusVO(Video video);
}

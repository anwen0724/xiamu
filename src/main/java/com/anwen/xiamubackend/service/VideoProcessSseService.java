package com.anwen.xiamubackend.service;

import com.anwen.xiamubackend.model.vo.VideoProcessStatusVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 视频处理状态 SSE 服务。
 */
public interface VideoProcessSseService {

    SseEmitter subscribe(Long videoId, VideoProcessStatusVO initialStatus);

    void publish(VideoProcessStatusVO status);
}

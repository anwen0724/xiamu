package com.anwen.xiamubackend.service.impl;

import com.anwen.xiamubackend.model.vo.VideoProcessStatusVO;
import com.anwen.xiamubackend.service.VideoProcessSseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 视频处理状态 SSE 服务实现。
 */
@Slf4j
@Service
public class VideoProcessSseServiceImpl implements VideoProcessSseService {

    private static final long SSE_TIMEOUT_MILLIS = 30L * 60L * 1000L;

    private final Map<Long, Map<String, SseEmitter>> emittersByVideoId = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribe(Long videoId, VideoProcessStatusVO initialStatus) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        String emitterId = UUID.randomUUID().toString();
        emittersByVideoId.computeIfAbsent(videoId, key -> new ConcurrentHashMap<>()).put(emitterId, emitter);

        emitter.onCompletion(() -> removeEmitter(videoId, emitterId));
        emitter.onTimeout(() -> removeEmitter(videoId, emitterId));
        emitter.onError(error -> removeEmitter(videoId, emitterId));

        try {
            emitter.send(SseEmitter.event()
                    .name("status")
                    .data(initialStatus));
        } catch (IOException e) {
            log.warn("发送视频初始状态 SSE 失败: videoId={}", videoId, e);
            removeEmitter(videoId, emitterId);
            emitter.completeWithError(e);
        }
        return emitter;
    }

    @Override
    public void publish(VideoProcessStatusVO status) {
        if (status == null || status.getId() == null) {
            return;
        }
        Map<String, SseEmitter> emitters = emittersByVideoId.get(status.getId());
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        emitters.forEach((emitterId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("status")
                        .data(status));
            } catch (IOException e) {
                log.warn("推送视频状态 SSE 失败: videoId={}, emitterId={}", status.getId(), emitterId, e);
                removeEmitter(status.getId(), emitterId);
                emitter.completeWithError(e);
            }
        });
    }

    private void removeEmitter(Long videoId, String emitterId) {
        Map<String, SseEmitter> emitters = emittersByVideoId.get(videoId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitterId);
        if (emitters.isEmpty()) {
            emittersByVideoId.remove(videoId);
        }
    }
}

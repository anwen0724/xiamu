package com.anwen.xiamubackend.ai.routing;

import com.anwen.xiamubackend.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * AI 模型路由器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiModelRouter {

    private final AiProperties aiProperties;
    private final AiModelRegistry aiModelRegistry;

    public AiModelRegistration route(AiTaskType taskType) {
        return route(taskType, null);
    }

    public AiModelRegistration route(AiTaskType taskType, String requestedModelKey) {
        String resolvedModelKey = resolveModelKey(taskType, requestedModelKey);
        AiModelRegistration registration = aiModelRegistry.getRequired(resolvedModelKey);
        log.info("AI 路由完成: taskType={}, requestedModelKey={}, resolvedModelKey={}, provider={}, model={}",
                taskType, requestedModelKey, registration.key(), registration.provider(), registration.model());
        return registration;
    }

    private String resolveModelKey(AiTaskType taskType, String requestedModelKey) {
        if (StringUtils.hasText(requestedModelKey)) {
            return requestedModelKey.trim();
        }

        return switch (taskType) {
            case SUMMARY -> StringUtils.hasText(aiProperties.getSummaryModel())
                    ? aiProperties.getSummaryModel()
                    : aiModelRegistry.getDefault(aiProperties.getDefaultModel()).key();
            case CHAPTER -> StringUtils.hasText(aiProperties.getChapterModel())
                    ? aiProperties.getChapterModel()
                    : aiModelRegistry.getDefault(aiProperties.getDefaultModel()).key();
        };
    }
}

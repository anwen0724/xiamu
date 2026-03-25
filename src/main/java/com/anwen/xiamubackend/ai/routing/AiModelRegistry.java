package com.anwen.xiamubackend.ai.routing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 模型注册表。
 */
@Slf4j
@Component
public class AiModelRegistry {

    private final Map<String, AiModelRegistration> registrations;

    public AiModelRegistry(List<RegistrationGroup> registrationGroups) {
        Map<String, AiModelRegistration> merged = new LinkedHashMap<>();
        for (RegistrationGroup group : registrationGroups) {
            if (group == null || group.registrations() == null || group.registrations().isEmpty()) {
                continue;
            }
            for (Map.Entry<String, AiModelRegistration> entry : group.registrations().entrySet()) {
                AiModelRegistration previous = merged.putIfAbsent(entry.getKey(), entry.getValue());
                if (previous != null) {
                    throw new IllegalStateException("检测到重复的 AI 模型 key: " + entry.getKey());
                }
            }
        }

        if (merged.isEmpty()) {
            throw new IllegalStateException("未配置任何可用 AI 模型，请检查 xiamu.ai.models");
        }

        this.registrations = Collections.unmodifiableMap(merged);
        log.info("AI 模型注册完成: providers={}, modelKeys={}",
                registrationGroups.stream()
                        .filter(group -> group != null && group.registrations() != null && !group.registrations().isEmpty())
                        .map(RegistrationGroup::provider)
                        .collect(Collectors.toList()),
                this.registrations.keySet());
    }

    public AiModelRegistration getRequired(String modelKey) {
        if (!StringUtils.hasText(modelKey)) {
            throw new IllegalArgumentException("模型 key 不能为空");
        }
        AiModelRegistration registration = registrations.get(modelKey);
        if (registration == null) {
            throw new IllegalArgumentException("未找到模型配置: " + modelKey);
        }
        return registration;
    }

    public AiModelRegistration getDefault(String defaultModelKey) {
        if (StringUtils.hasText(defaultModelKey) && registrations.containsKey(defaultModelKey)) {
            return registrations.get(defaultModelKey);
        }
        return registrations.values().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未注册任何可用 AI 模型"));
    }

    /**
     * provider 级模型注册分组，仅供 AiModelRegistry 聚合时使用。
     */
    public record RegistrationGroup(
            String provider,
            Map<String, AiModelRegistration> registrations) {
    }
}

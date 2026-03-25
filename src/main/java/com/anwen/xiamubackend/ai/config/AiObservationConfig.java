package com.anwen.xiamubackend.ai.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 观测配置。
 */
@Configuration
public class AiObservationConfig {

    @Bean
    @ConditionalOnMissingBean(ObservationRegistry.class)
    public ObservationRegistry observationRegistry() {
        return ObservationRegistry.NOOP;
    }
}

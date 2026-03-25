package com.anwen.xiamubackend.ai.config;

import com.anwen.xiamubackend.ai.routing.AiModelRegistration;
import com.anwen.xiamubackend.ai.routing.AiModelRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OpenAI 模型装配配置。
 */
@Configuration
public class OpenAiModelConfig {

    @Bean
    public AiModelRegistry.RegistrationGroup openAiModelRegistrationGroup(AiProperties aiProperties) {
        Map<String, AiModelRegistration> registrations = new LinkedHashMap<>();
        aiProperties.getModels().forEach((key, properties) -> {
            if (!supportsProvider(properties, "openai")) {
                return;
            }
            registrations.put(key, createRegistration(key, properties));
        });
        return new AiModelRegistry.RegistrationGroup("openai", registrations);
    }

    private boolean supportsProvider(AiModelProperties properties, String provider) {
        return properties != null
                && Boolean.TRUE.equals(properties.getEnabled())
                && provider.equalsIgnoreCase(properties.getProvider());
    }

    private AiModelRegistration createRegistration(String key, AiModelProperties properties) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .build();

        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                .model(properties.getModel())
                .temperature(properties.getTemperature())
                .build();

        OpenAiChatModel chatModel = new OpenAiChatModel(openAiApi, chatOptions);
        return new AiModelRegistration(
                key,
                "openai",
                properties.getModel(),
                properties.getBaseUrl(),
                ChatClient.builder(chatModel).build());
    }
}

package com.anwen.xiamubackend.ai.routing;

import com.anwen.xiamubackend.ai.config.AiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class AiModelRouterTests {

    private AiModelRouter aiModelRouter;

    @BeforeEach
    void setUp() {
        Map<String, AiModelRegistration> registrations = new LinkedHashMap<>();
        registrations.put("openai", new AiModelRegistration("openai", "openai", "gpt-4.1-mini", "https://api.openai.com", mock(ChatClient.class)));
        registrations.put("deepseek", new AiModelRegistration("deepseek", "deepseek", "deepseek-chat", "https://api.deepseek.com", mock(ChatClient.class)));

        AiProperties aiProperties = new AiProperties();
        aiProperties.setDefaultModel("openai");
        aiProperties.setChapterModel("deepseek");

        AiModelRegistry aiModelRegistry = new AiModelRegistry(List.of(
                new AiModelRegistry.RegistrationGroup("test", registrations)));
        aiModelRouter = new AiModelRouter(aiProperties, aiModelRegistry);
    }

    @Test
    void shouldUseDefaultModelForSummaryWhenNoTaskModelSpecified() {
        AiModelRegistration registration = aiModelRouter.route(AiTaskType.SUMMARY);
        assertEquals("openai", registration.key());
    }

    @Test
    void shouldUseTaskModelForChapterWhenConfigured() {
        AiModelRegistration registration = aiModelRouter.route(AiTaskType.CHAPTER);
        assertEquals("deepseek", registration.key());
    }

    @Test
    void shouldUseRequestedModelWhenExplicitModelKeyProvided() {
        AiModelRegistration registration = aiModelRouter.route(AiTaskType.SUMMARY, "deepseek");
        assertEquals("deepseek", registration.key());
    }
}

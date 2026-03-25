package com.anwen.xiamubackend.ai.service.impl;

import com.anwen.xiamubackend.ai.config.AiProperties;
import com.anwen.xiamubackend.ai.routing.AiModelRegistration;
import com.anwen.xiamubackend.ai.routing.AiModelRouter;
import com.anwen.xiamubackend.ai.routing.AiTaskType;
import com.anwen.xiamubackend.model.vo.VideoChapterVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChapterServiceImplTests {

    @Mock
    private AiModelRouter aiModelRouter;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private AiChapterServiceImpl aiChapterService;

    @BeforeEach
    void setUp() {
        aiChapterService = new AiChapterServiceImpl(new AiProperties(), aiModelRouter, new ObjectMapper());
    }

    @Test
    void generateChaptersShouldFallbackToSingleChapterForShortVideo() {
        when(aiModelRouter.route(eq(AiTaskType.CHAPTER), isNull()))
                .thenReturn(new AiModelRegistration("deepseek", "deepseek", "deepseek-chat", "https://api.deepseek.com", chatClient));
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("""
                        [{"title":"只有一段","startSeconds":0,"endSeconds":0,"summary":"测试"}]
                        """);

        List<VideoChapterVO> chapters = aiChapterService.generateChapters("这是一个很短的视频测试文本", "[]", 67L);

        assertEquals(1, chapters.size());
        assertEquals("完整内容", chapters.get(0).getTitle());
        assertEquals(0, chapters.get(0).getStartSeconds());
        assertEquals(67, chapters.get(0).getEndSeconds());
        assertFalse(chapters.get(0).getSummary().isBlank());
    }
}

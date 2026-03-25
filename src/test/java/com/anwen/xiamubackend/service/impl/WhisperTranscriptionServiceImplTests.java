package com.anwen.xiamubackend.service.impl;

import com.anwen.xiamubackend.ai.service.AiChapterService;
import com.anwen.xiamubackend.ai.service.AiSummaryService;
import com.anwen.xiamubackend.mapper.VideoMapper;
import com.anwen.xiamubackend.model.entity.Video;
import com.anwen.xiamubackend.model.enums.VideoStatusEnum;
import com.anwen.xiamubackend.strategy.TranscriptionStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhisperTranscriptionServiceImplTests {

    @Mock
    private VideoMapper videoMapper;

    @Mock
    private AiSummaryService aiSummaryService;

    @Mock
    private AiChapterService aiChapterService;

    @Mock(name = "localModelStrategy")
    private TranscriptionStrategy localModelStrategy;

    @Mock(name = "apiCallStrategy")
    private TranscriptionStrategy apiCallStrategy;

    private WhisperTranscriptionServiceImpl whisperTranscriptionService;

    @BeforeEach
    void setUp() {
        whisperTranscriptionService = new WhisperTranscriptionServiceImpl(
                videoMapper,
                aiSummaryService,
                aiChapterService,
                new ObjectMapper(),
                localModelStrategy,
                apiCallStrategy);
        ReflectionTestUtils.setField(whisperTranscriptionService, "mode", "local");
    }

    @Test
    void transcribeAsyncShouldKeepSuccessWhenChapterGenerationFails() throws Exception {
        Path audioFile = Files.createTempFile("xiamu-audio", ".mp3");

        try {
            Video video = new Video();
            video.setId(1L);
            video.setDuration(67L);
            video.setStatus(VideoStatusEnum.WAITING.getCode());

            when(videoMapper.selectById(1L)).thenReturn(video);
            when(videoMapper.updateById(any(Video.class))).thenReturn(1);
            when(localModelStrategy.transcribe(audioFile.toString())).thenReturn("""
                    {"text":"这是一个很短的视频测试文本","language":"zh","segments":[]}
                    """);
            when(aiSummaryService.generateSummary("这是一个很短的视频测试文本")).thenReturn("摘要内容");
            when(aiChapterService.generateChapters("这是一个很短的视频测试文本", "[]", 67L))
                    .thenThrow(new IllegalStateException("未生成有效章节"));

            whisperTranscriptionService.transcribeAsync(1L, audioFile.toString());

            ArgumentCaptor<Video> videoCaptor = ArgumentCaptor.forClass(Video.class);
            verify(videoMapper, atLeast(1)).updateById(videoCaptor.capture());

            Video finalVideo = videoCaptor.getValue();
            assertEquals(VideoStatusEnum.SUCCESS.getCode(), finalVideo.getStatus());
            assertEquals("摘要内容", finalVideo.getSummary());
            assertEquals("[]", finalVideo.getChapters());
            assertNull(finalVideo.getErrorMessage());
        } finally {
            Files.deleteIfExists(audioFile);
        }
    }
}

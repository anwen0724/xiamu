package com.anwen.xiamubackend.service.impl;

import com.anwen.xiamubackend.ai.service.AiChapterService;
import com.anwen.xiamubackend.ai.service.AiSummaryService;
import com.anwen.xiamubackend.mapper.VideoMapper;
import com.anwen.xiamubackend.model.entity.Video;
import com.anwen.xiamubackend.model.enums.VideoProcessStageEnum;
import com.anwen.xiamubackend.service.VideoProcessStateService;
import com.anwen.xiamubackend.strategy.TranscriptionStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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

    @Mock
    private VideoProcessStateService videoProcessStateService;

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
                videoProcessStateService,
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

            when(videoMapper.selectById(1L)).thenReturn(video);
            when(localModelStrategy.transcribe(audioFile.toString())).thenReturn("""
                    {"text":"这是一个很短的视频测试文本","language":"zh","segments":[]}
                    """);
            when(aiSummaryService.generateSummary("这是一个很短的视频测试文本")).thenReturn("摘要内容");
            when(aiChapterService.generateChapters("这是一个很短的视频测试文本", "[]", 67L))
                    .thenThrow(new IllegalStateException("未生成有效章节"));

            whisperTranscriptionService.transcribeAsync(1L, audioFile.toString());

            verify(videoProcessStateService).updateProcessingStage(eq(1L), eq(VideoProcessStageEnum.TRANSCRIBING), any());
            verify(videoProcessStateService).updateProcessingStage(eq(1L), eq(VideoProcessStageEnum.GENERATING_SUMMARY), any());
            verify(videoProcessStateService).updateProcessingStage(eq(1L), eq(VideoProcessStageEnum.GENERATING_CHAPTERS), any());
            verify(videoProcessStateService).markSuccess(eq(1L), any());
        } finally {
            Files.deleteIfExists(audioFile);
        }
    }

    @Test
    void transcribeAsyncShouldUseApiStrategyWhenModeIsApi() throws Exception {
        Path audioFile = Files.createTempFile("xiamu-audio-api", ".mp3");

        try {
            ReflectionTestUtils.setField(whisperTranscriptionService, "mode", "api");

            Video video = new Video();
            video.setId(2L);
            video.setDuration(45L);

            when(videoMapper.selectById(2L)).thenReturn(video);
            when(apiCallStrategy.transcribe(audioFile.toString())).thenReturn("""
                    {"text":"接口转写结果","language":"zh","segments":[]}
                    """);
            when(aiSummaryService.generateSummary("接口转写结果")).thenReturn("接口摘要");
            when(aiChapterService.generateChapters("接口转写结果", "[]", 45L)).thenReturn(List.of());

            whisperTranscriptionService.transcribeAsync(2L, audioFile.toString());

            verify(apiCallStrategy).transcribe(audioFile.toString());
            verify(localModelStrategy, never()).transcribe(anyString());
            verify(videoProcessStateService).markSuccess(eq(2L), any());
        } finally {
            Files.deleteIfExists(audioFile);
        }
    }
}
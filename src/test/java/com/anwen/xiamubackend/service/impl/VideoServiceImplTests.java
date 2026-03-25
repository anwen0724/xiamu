package com.anwen.xiamubackend.service.impl;

import com.anwen.xiamubackend.mapper.VideoMapper;
import com.anwen.xiamubackend.model.entity.Video;
import com.anwen.xiamubackend.model.enums.VideoStatusEnum;
import com.anwen.xiamubackend.model.vo.VideoProcessStatusVO;
import com.anwen.xiamubackend.model.vo.VideoResultVO;
import com.anwen.xiamubackend.service.AudioExtractService;
import com.anwen.xiamubackend.service.VideoProcessStateService;
import com.anwen.xiamubackend.service.WhisperTranscriptionService;
import com.anwen.xiamubackend.utils.FFmpegUtil;
import com.anwen.xiamubackend.utils.FileStorageUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoServiceImplTests {

    @Mock
    private FileStorageUtil fileStorageUtil;

    @Mock
    private FFmpegUtil ffmpegUtil;

    @Mock
    private AudioExtractService audioExtractService;

    @Mock
    private WhisperTranscriptionService whisperTranscriptionService;

    @Mock
    private VideoProcessStateService videoProcessStateService;

    @Mock
    private VideoMapper videoMapper;

    private VideoServiceImpl videoService;

    @BeforeEach
    void setUp() {
        videoService = new VideoServiceImpl(
                fileStorageUtil,
                ffmpegUtil,
                audioExtractService,
                whisperTranscriptionService,
                videoProcessStateService,
                videoMapper,
                new ObjectMapper());
    }

    @Test
    void getVideoResultShouldParseChapters() {
        Video video = new Video();
        video.setId(1L);
        video.setUserId(10L);
        video.setTitle("demo");
        video.setStatus(VideoStatusEnum.SUCCESS.getCode());
        video.setChapters("""
                [{"title":"开场","startSeconds":0,"endSeconds":60,"summary":"介绍"}]
                """);
        when(videoMapper.selectById(1L)).thenReturn(video);

        VideoResultVO resultVO = videoService.getVideoResult(1L, 10L);

        assertEquals(1, resultVO.getChapters().size());
        assertEquals("开场", resultVO.getChapters().get(0).getTitle());
    }

    @Test
    void getVideoStatusShouldReflectContentFlags() {
        Video video = new Video();
        video.setId(1L);
        video.setUserId(10L);
        video.setTitle("demo");
        video.setDuration(180L);
        video.setStatus(VideoStatusEnum.PROCESSING.getCode());
        video.setTranscript("hello");
        video.setSummary(null);
        video.setChapters("");
        when(videoMapper.selectById(1L)).thenReturn(video);

        VideoProcessStatusVO statusVO = new VideoProcessStatusVO();
        statusVO.setId(1L);
        statusVO.setStatus(VideoStatusEnum.PROCESSING.getCode());
        statusVO.setHasTranscript(true);
        statusVO.setHasSummary(false);
        statusVO.setHasChapters(false);
        when(videoProcessStateService.toStatusVO(video)).thenReturn(statusVO);

        VideoProcessStatusVO result = videoService.getVideoStatus(1L, 10L);

        assertTrue(result.isHasTranscript());
        assertFalse(result.isHasSummary());
        assertFalse(result.isHasChapters());
    }

    @Test
    void retryVideoShouldReuseAudioPathWhenPresent() {
        Video video = new Video();
        video.setId(1L);
        video.setUserId(10L);
        video.setStatus(VideoStatusEnum.FAILED.getCode());
        video.setUrl("video.mp4");
        video.setAudioPath("audio.mp3");
        when(videoMapper.selectById(1L)).thenReturn(video);

        boolean result = videoService.retryVideo(1L, 10L);

        assertTrue(result);
        verify(videoProcessStateService).updateQueued(any(Long.class), any());
        verify(whisperTranscriptionService).transcribeAsync(1L, "audio.mp3");
    }
}
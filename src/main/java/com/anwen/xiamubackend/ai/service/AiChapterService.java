package com.anwen.xiamubackend.ai.service;

import com.anwen.xiamubackend.model.vo.VideoChapterVO;

import java.util.List;

/**
 * AI 章节生成服务。
 */
public interface AiChapterService {

    List<VideoChapterVO> generateChapters(String transcript, String transcriptionJson, Long durationSeconds);

    List<VideoChapterVO> generateChapters(String transcript, String transcriptionJson, Long durationSeconds, String modelKey);
}

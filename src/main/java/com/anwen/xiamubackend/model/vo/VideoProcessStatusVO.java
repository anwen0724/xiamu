package com.anwen.xiamubackend.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 视频处理状态返回对象。
 */
@Data
public class VideoProcessStatusVO {

    private Long id;

    private String title;

    private Long duration;

    private String status;

    private String stage;

    private String stageMessage;

    private String errorMessage;

    private LocalDateTime updateTime;

    private boolean hasTranscript;

    private boolean hasSummary;

    private boolean hasChapters;
}

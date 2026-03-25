package com.anwen.xiamubackend.model.vo;

import lombok.Data;

/**
 * 视频处理状态返回对象。
 */
@Data
public class VideoProcessStatusVO {

    private Long id;

    private String status;

    private String errorMessage;

    private boolean hasTranscript;

    private boolean hasSummary;

    private boolean hasChapters;
}

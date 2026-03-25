package com.anwen.xiamubackend.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Dashboard 视频列表返回对象。
 */
@Data
public class VideoDashboardVO {

    private Long id;

    private String title;

    private String status;

    private Long duration;

    private Long fileSize;

    private String format;

    private String summary;

    private LocalDateTime createTime;
}

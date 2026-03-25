package com.anwen.xiamubackend.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频结果页返回对象。
 */
@Data
public class VideoResultVO {

    private Long id;

    private String title;

    private String status;

    private Long duration;

    private Long fileSize;

    private String format;

    private String summary;

    private String transcript;

    private String errorMessage;

    private List<VideoChapterVO> chapters;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

package com.anwen.xiamubackend.model.vo;

import lombok.Data;

/**
 * 视频章节视图对象。
 */
@Data
public class VideoChapterVO {

    private String title;

    private Integer startSeconds;

    private Integer endSeconds;

    private String summary;
}

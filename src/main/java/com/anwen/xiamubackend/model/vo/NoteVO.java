package com.anwen.xiamubackend.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记返回对象。
 */
@Data
public class NoteVO {

    private Long id;

    private Long videoId;

    private String title;

    private String content;

    private Integer timeSeconds;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

package com.anwen.xiamubackend.model.dto.note;

import lombok.Data;

import java.io.Serializable;

/**
 * 新增笔记请求。
 */
@Data
public class NoteAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long videoId;

    private String title;

    private String content;

    private Integer timeSeconds;
}

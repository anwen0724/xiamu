package com.anwen.xiamubackend.model.dto.note;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新笔记请求。
 */
@Data
public class NoteUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String title;

    private String content;

    private Integer timeSeconds;
}

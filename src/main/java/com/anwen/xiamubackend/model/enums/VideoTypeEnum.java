package com.anwen.xiamubackend.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 视频类型枚举
 */
@Getter
@AllArgsConstructor
public enum VideoTypeEnum {

    /**
     * 上传视频
     */
    UPLOAD("UPLOAD", "上传视频"),

    /**
     * URL导入
     */
    URL("URL", "URL导入");

    /**
     * 类型码
     */
    private final String code;

    /**
     * 类型描述
     */
    private final String desc;
}

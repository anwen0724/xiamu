package com.anwen.xiamubackend.model.enums;

import lombok.Getter;

/**
 * 视频处理阶段枚举。
 */
@Getter
public enum VideoProcessStageEnum {

    QUEUED("QUEUED", "等待处理"),
    EXTRACTING_AUDIO("EXTRACTING_AUDIO", "正在提取音频"),
    TRANSCRIBING("TRANSCRIBING", "正在转写文本"),
    GENERATING_SUMMARY("GENERATING_SUMMARY", "正在生成摘要"),
    GENERATING_CHAPTERS("GENERATING_CHAPTERS", "正在生成章节"),
    COMPLETED("COMPLETED", "处理完成"),
    FAILED("FAILED", "处理失败");

    private final String code;

    private final String desc;

    VideoProcessStageEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

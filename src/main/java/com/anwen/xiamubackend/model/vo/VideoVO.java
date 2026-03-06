package com.anwen.xiamubackend.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 视频信息返回对象
 */
@Data
public class VideoVO {

    /**
     * 视频ID
     */
    private Long id;

    /**
     * 视频标题
     */
    private String title;

    /**
     * 视频文件路径
     */
    private String url;

    /**
     * 视频时长（秒）
     */
    private Long duration;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 处理状态
     */
    private String status;

    /**
     * 视频类型
     */
    private String type;

    /**
     * 视频格式
     */
    private String format;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}

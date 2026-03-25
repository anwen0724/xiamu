package com.anwen.xiamubackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 视频实体类
 */
@Data
@TableName("video")
public class Video implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 视频ID - 雪花算法生成
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID（用户模块后实现，暂时可为空）
     */
    private Long userId;

    /**
     * 视频标题
     */
    private String title;

    /**
     * 视频文件路径
     */
    private String url;

    /**
     * 音频文件路径
     */
    private String audioPath;

    /**
     * 视频时长（秒）
     */
    private Long duration;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 处理状态: WAITING/PROCESSING/SUCCESS/FAILED
     */
    private String status;

    /**
     * 当前处理阶段。
     */
    private String stage;

    /**
     * 当前处理阶段说明。
     */
    private String stageMessage;

    /**
     * 转写文本
     */
    private String transcript;

    /**
     * 摘要内容
     */
    private String summary;

    /**
     * 章节数据（JSON格式）
     */
    private String chapters;

    /**
     * 视频类型: UPLOAD/URL
     */
    private String type;

    /**
     * 视频格式: mp4/avi/mkv/webm
     */
    private String format;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除: 0未删除/1已删除
     */
    @TableLogic
    private Integer isDelete;
}

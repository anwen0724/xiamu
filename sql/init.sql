
-- create the database
create database if not exists xiamu;

-- switch the database
use xiamu;

-- create video table
CREATE TABLE `video` (
    `id` BIGINT NOT NULL COMMENT '视频ID',
    `userId` BIGINT DEFAULT NULL COMMENT '用户ID',
    `title` VARCHAR(255) NOT NULL COMMENT '视频标题',
    `url` VARCHAR(512) NOT NULL COMMENT '视频文件路径',
    `duration` BIGINT DEFAULT 0 COMMENT '视频时长（秒）',
    `fileSize` BIGINT NOT NULL COMMENT '文件大小（字节）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'WAITING' COMMENT '处理状态: WAITING/PROCESSING/SUCCESS/FAILED',
    `transcript` TEXT COMMENT '转写文本',
    `summary` TEXT COMMENT '摘要内容',
    `chapters` JSON COMMENT '章节数据',
    `type` VARCHAR(20) NOT NULL COMMENT '视频类型: UPLOAD/URL',
    `format` VARCHAR(20) NOT NULL COMMENT '视频格式: mp4/avi/mkv/webm',
    `errorMessage` TEXT COMMENT '错误信息',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删除/1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_userId` (`userId`),
    KEY `idx_createTime` (`createTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频表';

-- add audioPath field to video table (第二步：音频提取)
ALTER TABLE `video`
ADD COLUMN `audioPath` VARCHAR(512) DEFAULT NULL COMMENT '音频文件路径' AFTER `url`;

-- create user table
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL COMMENT '用户ID',
    `userAccount` VARCHAR(64) NOT NULL COMMENT '用户账号',
    `userPassword` VARCHAR(255) NOT NULL COMMENT '用户密码（加密）',
    `userName` VARCHAR(128) NOT NULL COMMENT '用户名',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删除/1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_userAccount` (`userAccount`),
    KEY `idx_user_createTime` (`createTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- create note table
CREATE TABLE IF NOT EXISTS `note` (
    `id` BIGINT NOT NULL COMMENT '笔记ID',
    `userId` BIGINT NOT NULL COMMENT '用户ID',
    `videoId` BIGINT NOT NULL COMMENT '视频ID',
    `title` VARCHAR(100) NOT NULL COMMENT '笔记标题',
    `content` TEXT NOT NULL COMMENT '笔记内容',
    `timeSeconds` INT DEFAULT NULL COMMENT '关联视频时间点（秒）',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删除/1已删除',
    PRIMARY KEY (`id`),
    KEY `idx_note_userId` (`userId`),
    KEY `idx_note_videoId` (`videoId`),
    KEY `idx_note_createTime` (`createTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记表';

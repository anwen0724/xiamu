package com.anwen.xiamubackend.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户返回对象。
 */
@Data
public class UserVO {

    private Long id;

    private String userAccount;

    private String userName;

    private LocalDateTime createTime;
}

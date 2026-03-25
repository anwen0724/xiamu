package com.anwen.xiamubackend.service;

import com.anwen.xiamubackend.model.entity.User;
import com.anwen.xiamubackend.model.vo.UserVO;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 用户服务。
 */
public interface UserService extends IService<User> {

    long userRegister(String userAccount, String userPassword, String checkPassword, String userName);

    UserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    boolean userLogout(HttpServletRequest request);

    User getLoginUser(HttpServletRequest request);

    UserVO getLoginUserVO(HttpServletRequest request);
}

package com.anwen.xiamubackend.service.impl;

import com.anwen.xiamubackend.common.UserConstant;
import com.anwen.xiamubackend.exception.BusinessException;
import com.anwen.xiamubackend.exception.ErrorCode;
import com.anwen.xiamubackend.exception.ThrowUtils;
import com.anwen.xiamubackend.mapper.UserMapper;
import com.anwen.xiamubackend.model.entity.User;
import com.anwen.xiamubackend.model.vo.UserVO;
import com.anwen.xiamubackend.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 用户服务实现。
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String userName) {
        ThrowUtils.throwIf(!StringUtils.hasText(userAccount), ErrorCode.PARAMS_ERROR, "账号不能为空");
        ThrowUtils.throwIf(!StringUtils.hasText(userPassword), ErrorCode.PARAMS_ERROR, "密码不能为空");
        ThrowUtils.throwIf(!StringUtils.hasText(checkPassword), ErrorCode.PARAMS_ERROR, "确认密码不能为空");
        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "账号长度不能小于 4");
        ThrowUtils.throwIf(userPassword.length() < 8, ErrorCode.PARAMS_ERROR, "密码长度不能小于 8");
        ThrowUtils.throwIf(!userPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次输入密码不一致");

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>()
                .eq(User::getUserAccount, userAccount.trim());
        Long count = userMapper.selectCount(queryWrapper);
        ThrowUtils.throwIf(count != null && count > 0, ErrorCode.PARAMS_ERROR, "账号已存在");

        User user = new User();
        user.setUserAccount(userAccount.trim());
        user.setUserName(StringUtils.hasText(userName) ? userName.trim() : userAccount.trim());
        user.setUserPassword(passwordEncoder.encode(userPassword));
        int result = userMapper.insert(user);
        ThrowUtils.throwIf(result <= 0, ErrorCode.SYSTEM_ERROR, "注册失败");
        return user.getId();
    }

    @Override
    public UserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.SYSTEM_ERROR, "请求上下文为空");
        ThrowUtils.throwIf(!StringUtils.hasText(userAccount), ErrorCode.PARAMS_ERROR, "账号不能为空");
        ThrowUtils.throwIf(!StringUtils.hasText(userPassword), ErrorCode.PARAMS_ERROR, "密码不能为空");

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>()
                .eq(User::getUserAccount, userAccount.trim())
                .last("limit 1");
        User user = userMapper.selectOne(queryWrapper);
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "账号或密码错误");
        ThrowUtils.throwIf(!passwordEncoder.matches(userPassword, user.getUserPassword()), ErrorCode.PARAMS_ERROR, "账号或密码错误");

        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user.getId());
        return toUserVO(user);
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.SYSTEM_ERROR, "请求上下文为空");
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.NOT_LOGIN_ERROR, "未登录");
        Object userIdObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        ThrowUtils.throwIf(!(userIdObj instanceof Long), ErrorCode.NOT_LOGIN_ERROR, "未登录");
        Long userId = (Long) userIdObj;
        User user = userMapper.selectById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "登录状态失效");
        return user;
    }

    @Override
    public UserVO getLoginUserVO(HttpServletRequest request) {
        return toUserVO(getLoginUser(request));
    }

    private UserVO toUserVO(User user) {
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }
}

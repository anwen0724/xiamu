package com.anwen.xiamubackend.service.impl;

import com.anwen.xiamubackend.common.UserConstant;
import com.anwen.xiamubackend.exception.BusinessException;
import com.anwen.xiamubackend.mapper.UserMapper;
import com.anwen.xiamubackend.model.entity.User;
import com.anwen.xiamubackend.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTests {

    @Mock
    private UserMapper userMapper;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userMapper, new BCryptPasswordEncoder());
    }

    @Test
    void userRegisterShouldInsertEncodedPassword() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1001L);
            return 1;
        });

        long userId = userService.userRegister("testUser", "password123", "password123", "测试用户");

        assertEquals(1001L, userId);
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void userLoginShouldWriteSession() {
        User user = new User();
        user.setId(2001L);
        user.setUserAccount("testUser");
        user.setUserName("测试用户");
        user.setUserPassword(new BCryptPasswordEncoder().encode("password123"));
        when(userMapper.selectOne(any())).thenReturn(user);
        when(request.getSession()).thenReturn(session);

        UserVO userVO = userService.userLogin("testUser", "password123", request);

        assertEquals(2001L, userVO.getId());
        assertEquals("测试用户", userVO.getUserName());
        verify(session).setAttribute(UserConstant.USER_LOGIN_STATE, 2001L);
    }

    @Test
    void getLoginUserShouldThrowWhenSessionMissing() {
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(UserConstant.USER_LOGIN_STATE)).thenReturn(null);

        assertThrows(BusinessException.class, () -> userService.getLoginUser(request));
    }
}

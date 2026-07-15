package xyz.qvtrmx.ai.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import xyz.qvtrmx.ai.common.exception.BusinessException;
import xyz.qvtrmx.ai.chat.entity.ChatMessage;
import xyz.qvtrmx.ai.chat.mapper.ChatMessageMapper;
import xyz.qvtrmx.ai.security.model.AuthenticatedUser;
import xyz.qvtrmx.ai.user.dto.ChangePasswordRequest;
import xyz.qvtrmx.ai.user.entity.User;
import xyz.qvtrmx.ai.user.mapper.UserMapper;
import xyz.qvtrmx.ai.user.vo.UserProfileResponse;

class UserProfileServiceTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final ChatMessageMapper chatMessageMapper = mock(ChatMessageMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final UserProfileService userProfileService = new UserProfileService(userMapper, chatMessageMapper, passwordEncoder);

    @Test
    void returnsTheAuthenticatedUsersProfileWithoutPassword() {
        User user = activeUser();
        user.setRole(1);
        user.setBalance(new BigDecimal("23.50"));
        user.setCreatedAt(LocalDateTime.of(2026, 7, 1, 9, 30));
        when(userMapper.selectById(7L)).thenReturn(user);
        when(chatMessageMapper.selectCount(org.mockito.ArgumentMatchers.<LambdaQueryWrapper<ChatMessage>>any())).thenReturn(1284L);

        UserProfileResponse result = userProfileService.profile(new AuthenticatedUser(7L, "ignored", 0));

        assertEquals("member_01", result.username());
        assertEquals("ADMIN", result.role());
        assertEquals("ENABLED", result.status());
        assertEquals(1284L, result.totalCalls());
        assertEquals(new BigDecimal("23.50"), result.balance());
        assertEquals(LocalDateTime.of(2026, 7, 1, 9, 30), result.createdAt());
    }

    @Test
    void rejectsIncorrectCurrentPassword() {
        User user = activeUser();
        when(userMapper.selectById(7L)).thenReturn(user);
        when(passwordEncoder.matches("wrong-password", user.getPassword())).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userProfileService.changePassword(
                        new AuthenticatedUser(7L, "member_01", 0),
                        new ChangePasswordRequest("wrong-password", "new-password")
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        verify(passwordEncoder).matches("wrong-password", user.getPassword());
        verify(userMapper, never()).updateById(user);
    }

    @Test
    void storesAHashAfterVerifyingTheCurrentPassword() {
        User user = activeUser();
        when(userMapper.selectById(7L)).thenReturn(user);
        when(passwordEncoder.matches("current-password", user.getPassword())).thenReturn(true);
        when(passwordEncoder.matches("new-password", user.getPassword())).thenReturn(false);
        when(passwordEncoder.encode("new-password")).thenReturn("new-password-hash");

        userProfileService.changePassword(
                new AuthenticatedUser(7L, "member_01", 0),
                new ChangePasswordRequest("current-password", "new-password")
        );

        assertEquals("new-password-hash", user.getPassword());
        verify(userMapper).updateById(user);
    }

    private User activeUser() {
        User user = new User();
        user.setId(7L);
        user.setUsername("member_01");
        user.setPassword("current-password-hash");
        user.setRole(0);
        user.setStatus(1);
        user.setDeleted(0);
        return user;
    }
}

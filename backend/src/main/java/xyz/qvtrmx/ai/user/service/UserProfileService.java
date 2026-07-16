package xyz.qvtrmx.ai.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.qvtrmx.ai.common.exception.BusinessException;
import xyz.qvtrmx.ai.chat.entity.ChatMessage;
import xyz.qvtrmx.ai.chat.mapper.ChatMessageMapper;
import xyz.qvtrmx.ai.security.model.AuthenticatedUser;
import xyz.qvtrmx.ai.security.model.UserRole;
import xyz.qvtrmx.ai.user.dto.ChangePasswordRequest;
import xyz.qvtrmx.ai.user.entity.User;
import xyz.qvtrmx.ai.user.mapper.UserMapper;
import xyz.qvtrmx.ai.user.vo.UserProfileResponse;

@Service
public class UserProfileService {

    private static final int ENABLED_STATUS = 1;

    private final UserMapper userMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final PasswordEncoder passwordEncoder;

    public UserProfileService(
            UserMapper userMapper,
            ChatMessageMapper chatMessageMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.userMapper = userMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public UserProfileResponse profile(AuthenticatedUser authenticatedUser) {
        User user = requireExistingUser(authenticatedUser.id());
        return new UserProfileResponse(
                user.getUsername(),
                UserRole.fromCode(user.getRole()).name(),
                user.getStatus() == ENABLED_STATUS ? "ENABLED" : "DISABLED",
                chatMessageMapper.selectCount(new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getUserId, user.getId())
                        .eq(ChatMessage::getRole, "user")),
                user.getBalance(),
                user.getCreatedAt()
        );
    }

    @Transactional
    public void changePassword(AuthenticatedUser authenticatedUser, ChangePasswordRequest request) {
        User user = requireActiveUser(authenticatedUser.id());
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "当前密码错误");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "新密码不能与当前密码相同");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userMapper.updateById(user);
    }

    private User requireExistingUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "账号不可用");
        }
        return user;
    }

    private User requireActiveUser(Long userId) {
        User user = requireExistingUser(userId);
        if (user.getStatus() != ENABLED_STATUS) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "账号不可用");
        }
        return user;
    }
}

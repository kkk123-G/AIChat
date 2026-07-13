package xyz.qvtrmx.ai.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.qvtrmx.ai.common.api.ApiResponse;
import xyz.qvtrmx.ai.common.exception.BusinessException;
import xyz.qvtrmx.ai.security.model.AuthenticatedUser;
import xyz.qvtrmx.ai.user.entity.User;
import xyz.qvtrmx.ai.user.mapper.UserMapper;
import xyz.qvtrmx.ai.user.vo.UserBalanceResponse;

@RestController
@RequestMapping("/api/user")
public class UserAccountController {

    private final UserMapper userMapper;

    public UserAccountController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GetMapping("/balance")
    public ApiResponse<UserBalanceResponse> balance(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        User user = userMapper.selectById(authenticatedUser.id());
        if (user == null || user.getDeleted() == 1 || user.getStatus() != 1) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Account is unavailable");
        }
        return ApiResponse.ok(new UserBalanceResponse(user.getBalance()));
    }
}

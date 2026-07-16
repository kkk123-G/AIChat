package xyz.qvtrmx.ai.user.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.qvtrmx.ai.common.api.ApiResponse;
import xyz.qvtrmx.ai.common.api.PageResponse;
import xyz.qvtrmx.ai.common.audit.SensitiveOperation;
import xyz.qvtrmx.ai.common.exception.BusinessException;
import xyz.qvtrmx.ai.security.model.AuthenticatedUser;
import xyz.qvtrmx.ai.user.entity.User;
import xyz.qvtrmx.ai.user.dto.ChangePasswordRequest;
import xyz.qvtrmx.ai.user.mapper.UserMapper;
import xyz.qvtrmx.ai.user.service.DailyCheckInService;
import xyz.qvtrmx.ai.user.service.UserBalanceService;
import xyz.qvtrmx.ai.user.service.UserProfileService;
import xyz.qvtrmx.ai.user.vo.BalanceChangeRecordResponse;
import xyz.qvtrmx.ai.user.vo.DailyCheckInStatusResponse;
import xyz.qvtrmx.ai.user.vo.UserBalanceResponse;
import xyz.qvtrmx.ai.user.vo.UserProfileResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/user")
public class UserAccountController {

    private final UserMapper userMapper;
    private final DailyCheckInService dailyCheckInService;
    private final UserBalanceService userBalanceService;
    private final UserProfileService userProfileService;

    public UserAccountController(
            UserMapper userMapper,
            DailyCheckInService dailyCheckInService,
            UserBalanceService userBalanceService,
            UserProfileService userProfileService
    ) {
        this.userMapper = userMapper;
        this.dailyCheckInService = dailyCheckInService;
        this.userBalanceService = userBalanceService;
        this.userProfileService = userProfileService;
    }

    @GetMapping("/balance")
    public ApiResponse<UserBalanceResponse> balance(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        User user = userMapper.selectById(authenticatedUser.id());
        if (user == null || user.getDeleted() == 1 || user.getStatus() != 1) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "账号不可用");
        }
        return ApiResponse.ok(new UserBalanceResponse(user.getBalance()));
    }

    @GetMapping("/profile")
    public ApiResponse<UserProfileResponse> profile(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ApiResponse.ok(userProfileService.profile(authenticatedUser));
    }

    @PutMapping("/password")
    @SensitiveOperation("PASSWORD_CHANGE")
    public ApiResponse<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        userProfileService.changePassword(authenticatedUser, request);
        return ApiResponse.ok(null);
    }

    @GetMapping("/check-in/status")
    public ApiResponse<DailyCheckInStatusResponse> checkInStatus(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ApiResponse.ok(new DailyCheckInStatusResponse(dailyCheckInService.hasCheckedInToday(authenticatedUser.id())));
    }

    @GetMapping("/balance-changes")
    public ApiResponse<PageResponse<BalanceChangeRecordResponse>> listBalanceChanges(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ApiResponse.ok(userBalanceService.listBalanceChanges(page, size, authenticatedUser));
    }

    @PostMapping("/check-in")
    @SensitiveOperation("DAILY_CHECK_IN")
    public ApiResponse<Void> checkIn(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        dailyCheckInService.checkIn(authenticatedUser.id());
        return ApiResponse.ok(null);
    }
}

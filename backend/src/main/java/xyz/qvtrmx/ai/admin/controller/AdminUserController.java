package xyz.qvtrmx.ai.admin.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.qvtrmx.ai.admin.dto.RechargeUserRequest;
import xyz.qvtrmx.ai.admin.service.AdminUserService;
import xyz.qvtrmx.ai.admin.vo.AdminUserResponse;
import xyz.qvtrmx.ai.admin.vo.RechargeUserResponse;
import xyz.qvtrmx.ai.common.api.ApiResponse;
import xyz.qvtrmx.ai.common.api.PageResponse;
import xyz.qvtrmx.ai.security.model.AuthenticatedUser;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminUserResponse>> listUsers(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(adminUserService.listUsers(page, size, keyword));
    }

    @PostMapping("/{userId}/recharges")
    public ApiResponse<RechargeUserResponse> rechargeUser(
            @PathVariable Long userId,
            @Valid @RequestBody RechargeUserRequest request,
            @AuthenticationPrincipal AuthenticatedUser operator
    ) {
        return ApiResponse.ok(adminUserService.rechargeUser(userId, request, operator));
    }
}

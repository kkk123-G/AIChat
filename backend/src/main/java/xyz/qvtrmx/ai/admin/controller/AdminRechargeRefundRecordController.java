package xyz.qvtrmx.ai.admin.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.qvtrmx.ai.admin.service.AdminUserService;
import xyz.qvtrmx.ai.admin.vo.RechargeRefundRecordResponse;
import xyz.qvtrmx.ai.common.api.ApiResponse;
import xyz.qvtrmx.ai.common.api.PageResponse;
import xyz.qvtrmx.ai.security.model.AuthenticatedUser;

@RestController
@RequestMapping("/api/admin/recharge-refund-records")
public class AdminRechargeRefundRecordController {

    private final AdminUserService adminUserService;

    public AdminRechargeRefundRecordController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ApiResponse<PageResponse<RechargeRefundRecordResponse>> listRechargeRefundRecords(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @AuthenticationPrincipal AuthenticatedUser operator
    ) {
        return ApiResponse.ok(adminUserService.listRechargeRefundRecords(page, size, operator));
    }
}

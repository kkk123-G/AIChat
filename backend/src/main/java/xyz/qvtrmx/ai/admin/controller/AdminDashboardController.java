package xyz.qvtrmx.ai.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.qvtrmx.ai.admin.service.AdminDashboardService;
import xyz.qvtrmx.ai.admin.vo.AdminDashboardResponse;
import xyz.qvtrmx.ai.common.api.ApiResponse;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping
    public ApiResponse<AdminDashboardResponse> overview() {
        return ApiResponse.ok(adminDashboardService.overview());
    }
}

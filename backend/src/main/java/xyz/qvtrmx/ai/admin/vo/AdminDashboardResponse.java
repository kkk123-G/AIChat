package xyz.qvtrmx.ai.admin.vo;

import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardResponse(
        long userCount,
        long newUsersToday,
        long todayRequests,
        long totalRequests,
        long todaySuccesses,
        long totalSuccesses,
        long todayFailures,
        long totalFailures,
        BigDecimal todaySuccessRate,
        BigDecimal totalSuccessRate,
        BigDecimal todayFailureRate,
        BigDecimal totalFailureRate,
        DashboardTrendResponse requestTrend,
        List<DashboardTopUserResponse> topUsers
) {
}

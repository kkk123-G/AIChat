package xyz.qvtrmx.ai.user.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserProfileResponse(
        String username,
        String role,
        String status,
        long totalCalls,
        BigDecimal balance,
        LocalDateTime createdAt
) {
}

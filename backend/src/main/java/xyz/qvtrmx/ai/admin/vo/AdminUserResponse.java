package xyz.qvtrmx.ai.admin.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminUserResponse(
        String id,
        String username,
        String role,
        BigDecimal balance,
        String status,
        LocalDateTime lastActiveAt,
        LocalDateTime lastUsedAt,
        LocalDateTime createdAt
) {
}

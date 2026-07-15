package xyz.qvtrmx.ai.admin.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RechargeRecordResponse(
        String id,
        String rechargeNo,
        String username,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String remark,
        String operatorUsername,
        LocalDateTime createdAt
) {
}

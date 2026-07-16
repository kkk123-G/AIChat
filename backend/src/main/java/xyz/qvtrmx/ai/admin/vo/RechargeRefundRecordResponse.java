package xyz.qvtrmx.ai.admin.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RechargeRefundRecordResponse(
        String id,
        String operationNo,
        String username,
        BigDecimal amount,
        String operationType,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String remark,
        String operatorUsername,
        LocalDateTime createdAt
) {
}

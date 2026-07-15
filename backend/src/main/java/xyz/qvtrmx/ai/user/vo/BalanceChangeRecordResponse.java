package xyz.qvtrmx.ai.user.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BalanceChangeRecordResponse(
        String id,
        BigDecimal amount,
        LocalDateTime createdAt,
        String type,
        String remark
) {
}

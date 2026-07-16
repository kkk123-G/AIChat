package xyz.qvtrmx.ai.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record RefundUserRequest(
        @DecimalMin(value = "0.01", message = "Refund amount must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "Refund amount must have at most 2 decimal places")
        BigDecimal amount,
        @Size(max = 200, message = "Remark must not exceed 200 characters")
        String remark
) {
}

package xyz.qvtrmx.ai.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record RefundUserRequest(
        @DecimalMin(value = "0.01", message = "退款金额必须大于 0")
        @Digits(integer = 10, fraction = 2, message = "退款金额最多保留 2 位小数")
        BigDecimal amount,
        @Size(max = 200, message = "备注长度不能超过 200 个字符")
        String remark
) {
}

package xyz.qvtrmx.ai.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "^[A-Za-z0-9_]{4,32}$", message = "用户名格式不正确")
        String username,
        @NotBlank(message = "密码不能为空")
        @Size(max = 72, message = "密码格式不正确")
        String password,
        @Size(max = 128, message = "设备标识长度不能超过 128 个字符")
        String deviceId
) {
}

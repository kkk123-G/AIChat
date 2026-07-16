package xyz.qvtrmx.ai.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "^[A-Za-z0-9_]{4,32}$", message = "用户名必须由 4-32 位字母、数字或下划线组成")
        String username,
        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 72, message = "密码长度必须为 8-72 个字符")
        String password,
        @Size(max = 64, message = "昵称长度不能超过 64 个字符")
        String nickname
) {
}

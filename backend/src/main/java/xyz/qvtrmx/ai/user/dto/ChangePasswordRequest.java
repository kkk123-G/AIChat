package xyz.qvtrmx.ai.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "当前密码不能为空")
        @Size(max = 72, message = "当前密码格式不正确")
        String currentPassword,
        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, max = 72, message = "新密码长度必须为 8-72 个字符")
        String newPassword
) {
}

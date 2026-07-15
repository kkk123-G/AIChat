package xyz.qvtrmx.ai.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        @Size(max = 72, message = "Current password is invalid")
        String currentPassword,
        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 72, message = "New password must be 8-72 characters")
        String newPassword
) {
}

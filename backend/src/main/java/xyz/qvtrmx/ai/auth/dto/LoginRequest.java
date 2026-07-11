package xyz.qvtrmx.ai.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Username is required")
        @Pattern(regexp = "^[A-Za-z0-9_]{4,32}$", message = "Invalid username")
        String username,
        @NotBlank(message = "Password is required")
        @Size(max = 72, message = "Invalid password")
        String password,
        @Size(max = 128, message = "Device id must not exceed 128 characters")
        String deviceId
) {
}

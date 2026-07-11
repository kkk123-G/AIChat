package xyz.qvtrmx.ai.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Pattern(regexp = "^[A-Za-z0-9_]{4,32}$", message = "Username must be 4-32 letters, numbers, or underscores")
        String username,
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
        String password,
        @Size(max = 64, message = "Nickname must not exceed 64 characters")
        String nickname
) {
}

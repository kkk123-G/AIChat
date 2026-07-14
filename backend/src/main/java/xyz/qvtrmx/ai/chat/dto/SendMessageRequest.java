package xyz.qvtrmx.ai.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "Message content is required")
        @Size(max = 12000, message = "Message content must not exceed 12000 characters")
        String content
) {
}

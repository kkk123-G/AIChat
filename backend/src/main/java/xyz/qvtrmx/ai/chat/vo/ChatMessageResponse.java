package xyz.qvtrmx.ai.chat.vo;

import java.time.LocalDateTime;

public record ChatMessageResponse(String id, String role, String content, LocalDateTime createdAt) {
}

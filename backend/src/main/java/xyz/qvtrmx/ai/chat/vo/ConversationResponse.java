package xyz.qvtrmx.ai.chat.vo;

import java.time.LocalDateTime;

public record ConversationResponse(String id, String title, LocalDateTime lastMessageAt, LocalDateTime createdAt) {
}

package xyz.qvtrmx.ai.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "消息内容不能为空")
        @Size(max = 12000, message = "消息内容长度不能超过 12000 个字符")
        String content
) {
}

package xyz.qvtrmx.ai.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("ai_chat_message")
@Getter
@Setter
public class ChatMessage {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long conversationId;
    private Long userId;
    private Integer sequenceNo;
    private String role;
    private String content;
    private String model;
    private Integer inputTokens;
    private Integer outputTokens;
    private LocalDateTime createdAt;
}

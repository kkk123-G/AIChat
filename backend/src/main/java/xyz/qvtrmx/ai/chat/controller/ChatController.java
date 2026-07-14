package xyz.qvtrmx.ai.chat.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import xyz.qvtrmx.ai.chat.dto.SendMessageRequest;
import xyz.qvtrmx.ai.chat.service.ChatService;
import xyz.qvtrmx.ai.chat.vo.ChatMessageResponse;
import xyz.qvtrmx.ai.chat.vo.ChatStreamEvent;
import xyz.qvtrmx.ai.chat.vo.ConversationResponse;
import xyz.qvtrmx.ai.common.api.ApiResponse;
import xyz.qvtrmx.ai.security.model.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/chat/conversations")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ApiResponse<ConversationResponse> createConversation(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(chatService.createConversation(user));
    }

    @GetMapping
    public ApiResponse<List<ConversationResponse>> listConversations(
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(chatService.listConversations(user, keyword));
    }

    @GetMapping("/{conversationId}/messages")
    public ApiResponse<List<ChatMessageResponse>> listMessages(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(chatService.listMessages(conversationId, user));
    }

    @DeleteMapping("/{conversationId}")
    public ApiResponse<Void> deleteConversation(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        chatService.deleteConversation(conversationId, user);
        return ApiResponse.ok(null);
    }

    @PostMapping(value = "/{conversationId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatStreamEvent> streamMessage(
            @PathVariable Long conversationId,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return chatService.streamAnswer(chatService.prepareTurn(conversationId, request.content(), user));
    }
}

package xyz.qvtrmx.ai.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import xyz.qvtrmx.ai.chat.client.OpenAiResponsesClient;
import xyz.qvtrmx.ai.chat.entity.ChatMessage;
import xyz.qvtrmx.ai.chat.entity.Conversation;
import xyz.qvtrmx.ai.chat.mapper.ChatMessageMapper;
import xyz.qvtrmx.ai.chat.mapper.ConversationMapper;
import xyz.qvtrmx.ai.chat.vo.ChatMessageResponse;
import xyz.qvtrmx.ai.chat.vo.ChatStreamEvent;
import xyz.qvtrmx.ai.chat.vo.ConversationResponse;
import xyz.qvtrmx.ai.common.exception.BusinessException;
import xyz.qvtrmx.ai.security.model.AuthenticatedUser;
import xyz.qvtrmx.ai.user.entity.User;
import xyz.qvtrmx.ai.user.mapper.UserMapper;
import xyz.qvtrmx.ai.user.service.UserBalanceService;

@Service
public class ChatService {

    private static final int ACTIVE_STATUS = 1;
    private static final String NEW_CONVERSATION_TITLE = "新对话";
    private static final int CONTEXT_MESSAGE_LIMIT = 20;

    private final ConversationMapper conversationMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final UserMapper userMapper;
    private final UserBalanceService userBalanceService;
    private final OpenAiResponsesClient responsesClient;
    private final ChatTitleService chatTitleService;

    public ChatService(
            ConversationMapper conversationMapper,
            ChatMessageMapper chatMessageMapper,
            UserMapper userMapper,
            UserBalanceService userBalanceService,
            OpenAiResponsesClient responsesClient,
            ChatTitleService chatTitleService
    ) {
        this.conversationMapper = conversationMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.userMapper = userMapper;
        this.userBalanceService = userBalanceService;
        this.responsesClient = responsesClient;
        this.chatTitleService = chatTitleService;
    }

    @Transactional
    public ConversationResponse createConversation(AuthenticatedUser user) {
        requireActiveUser(user.id());
        Conversation conversation = new Conversation();
        conversation.setUserId(user.id());
        conversation.setTitle(NEW_CONVERSATION_TITLE);
        conversation.setStatus(ACTIVE_STATUS);
        conversation.setDeleted(0);
        conversationMapper.insert(conversation);
        return toConversationResponse(conversation);
    }

    public List<ConversationResponse> listConversations(AuthenticatedUser user, String keyword) {
        requireActiveUser(user.id());
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return conversationMapper.searchByUser(user.id(), normalizedKeyword, 100).stream()
                .map(this::toConversationResponse)
                .toList();
    }

    public List<ConversationResponse> searchConversations(AuthenticatedUser user, String keyword) {
        return listConversations(user, keyword);
    }

    public List<ChatMessageResponse> listMessages(Long conversationId, AuthenticatedUser user) {
        requireActiveUser(user.id());
        requireConversation(conversationId, user.id());
        return chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getConversationId, conversationId)
                        .orderByAsc(ChatMessage::getSequenceNo))
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Transactional
    public void deleteConversation(Long conversationId, AuthenticatedUser user) {
        requireActiveUser(user.id());
        int affectedRows = conversationMapper.update(null, new LambdaUpdateWrapper<Conversation>()
                .eq(Conversation::getId, conversationId)
                .eq(Conversation::getUserId, user.id())
                .eq(Conversation::getDeleted, 0)
                .set(Conversation::getDeleted, 1));
        if (affectedRows == 0) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "会话不存在");
        }
    }

    @Transactional
    public ChatTurn prepareTurn(Long conversationId, String content, AuthenticatedUser user) {
        requireActiveUser(user.id());
        Conversation conversation = conversationMapper.selectOwnedForUpdate(conversationId, user.id());
        if (conversation == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "会话不存在");
        }
        int sequenceNo = nextSequenceNumber(conversationId);
        LocalDateTime now = LocalDateTime.now();
        ChatMessage userMessage = new ChatMessage();
        userMessage.setConversationId(conversationId);
        userMessage.setUserId(user.id());
        userMessage.setSequenceNo(sequenceNo);
        userMessage.setRole("user");
        userMessage.setContent(content.trim());
        userMessage.setCreatedAt(now);
        chatMessageMapper.insert(userMessage);
        userBalanceService.chargeAiChat(user.id(), userMessage.getId());

        conversationMapper.update(null, new LambdaUpdateWrapper<Conversation>()
                .eq(Conversation::getId, conversationId)
                .set(Conversation::getLastMessageAt, now));
        List<OpenAiResponsesClient.InputMessage> context = loadContext(conversationId);
        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setConversationId(conversationId);
        assistantMessage.setUserId(user.id());
        assistantMessage.setSequenceNo(sequenceNo + 1);
        assistantMessage.setRole("assistant");
        assistantMessage.setContent("");
        assistantMessage.setCreatedAt(now);
        chatMessageMapper.insert(assistantMessage);
        boolean shouldGenerateTitle = NEW_CONVERSATION_TITLE.equals(conversation.getTitle()) && sequenceNo == 1;
        return new ChatTurn(
                conversationId,
                user.id(),
                assistantMessage.getId(),
                userMessage.getId(),
                userMessage.getContent(),
                shouldGenerateTitle,
                context
        );
    }

    public Flux<ChatStreamEvent> streamAnswer(ChatTurn turn) {
        StringBuilder answer = new StringBuilder();
        AtomicBoolean finalized = new AtomicBoolean();
        return responsesClient.stream(turn.context())
                .map(chunk -> {
                    answer.append(chunk);
                    return ChatStreamEvent.delta(chunk);
                })
                .concatWith(Mono.defer(() -> {
                    if (answer.toString().isBlank()) {
                        return Mono.error(new IllegalStateException("The AI service did not return output text"));
                    }
                    return Mono.just(ChatStreamEvent.done(finalizeTurn(turn, answer.toString(), finalized)));
                }))
                .doOnCancel(() -> finalizeTurn(turn, answer.toString(), finalized))
                .onErrorResume(exception -> {
                    finalizeTurn(turn, answer.toString(), finalized);
                    return Mono.just(ChatStreamEvent.error("AI 服务暂时不可用"));
                });
    }

    private ChatMessageResponse finalizeTurn(ChatTurn turn, String content, AtomicBoolean finalized) {
        return finalized.compareAndSet(false, true) ? persistAssistantMessage(turn, content) : null;
    }

    private ChatMessageResponse persistAssistantMessage(ChatTurn turn, String content) {
        if (content.isBlank()) {
            chatMessageMapper.deleteById(turn.assistantMessageId());
            userBalanceService.refundAiChat(turn.userId(), turn.userMessageId());
            return null;
        }
        chatMessageMapper.update(null, new LambdaUpdateWrapper<ChatMessage>()
                .eq(ChatMessage::getId, turn.assistantMessageId())
                .set(ChatMessage::getContent, content));
        conversationMapper.update(null, new LambdaUpdateWrapper<Conversation>()
                .eq(Conversation::getId, turn.conversationId())
                .set(Conversation::getLastMessageAt, LocalDateTime.now()));
        if (turn.shouldGenerateTitle()) {
            chatTitleService.generateTitle(turn.conversationId(), turn.userContent());
        }
        return new ChatMessageResponse(String.valueOf(turn.assistantMessageId()), "assistant", content, LocalDateTime.now());
    }

    private Conversation requireConversation(Long conversationId, Long userId) {
        Conversation conversation = conversationMapper.selectOne(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getId, conversationId)
                .eq(Conversation::getUserId, userId)
                .eq(Conversation::getDeleted, 0)
                .eq(Conversation::getStatus, ACTIVE_STATUS));
        if (conversation == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "会话不存在");
        }
        return conversation;
    }

    private void requireActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1 || user.getStatus() != ACTIVE_STATUS) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "账号不可用");
        }
    }

    private int nextSequenceNumber(Long conversationId) {
        ChatMessage latest = chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId)
                .orderByDesc(ChatMessage::getSequenceNo)
                .last("LIMIT 1"));
        return latest == null ? 1 : latest.getSequenceNo() + 1;
    }

    private List<OpenAiResponsesClient.InputMessage> loadContext(Long conversationId) {
        List<ChatMessage> messages = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId)
                .orderByDesc(ChatMessage::getSequenceNo)
                .last("LIMIT " + CONTEXT_MESSAGE_LIMIT));
        List<OpenAiResponsesClient.InputMessage> context = new ArrayList<>();
        for (int index = messages.size() - 1; index >= 0; index--) {
            ChatMessage message = messages.get(index);
            if ("assistant".equals(message.getRole())) {
                context.add(new OpenAiResponsesClient.InputMessage("assistant", message.getContent()));
            } else if ("user".equals(message.getRole())) {
                context.add(new OpenAiResponsesClient.InputMessage("user", message.getContent()));
            }
        }
        return context;
    }

    private ConversationResponse toConversationResponse(Conversation conversation) {
        return new ConversationResponse(
                String.valueOf(conversation.getId()),
                conversation.getTitle(),
                conversation.getLastMessageAt(),
                conversation.getCreatedAt()
        );
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        return new ChatMessageResponse(String.valueOf(message.getId()), message.getRole(), message.getContent(), message.getCreatedAt());
    }

    public record ChatTurn(
            Long conversationId,
            Long userId,
            Long assistantMessageId,
            Long userMessageId,
            String userContent,
            boolean shouldGenerateTitle,
            List<OpenAiResponsesClient.InputMessage> context
    ) {
    }
}

package xyz.qvtrmx.ai.chat.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import xyz.qvtrmx.ai.chat.client.OpenAiResponsesClient;
import xyz.qvtrmx.ai.chat.entity.Conversation;
import xyz.qvtrmx.ai.chat.mapper.ConversationMapper;

@Service
public class ChatTitleService {

    private static final String NEW_CONVERSATION_TITLE = "新对话";
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatTitleService.class);
    private final OpenAiResponsesClient responsesClient;
    private final ConversationMapper conversationMapper;

    public ChatTitleService(OpenAiResponsesClient responsesClient, ConversationMapper conversationMapper) {
        this.responsesClient = responsesClient;
        this.conversationMapper = conversationMapper;
    }

    @Async
    public void generateTitle(Long conversationId, String firstUserMessage) {
        String fallbackTitle = normalizeTitle(null, firstUserMessage);
        updateTitle(conversationId, NEW_CONVERSATION_TITLE, fallbackTitle);
        try {
            String generated = responsesClient.complete(
                    List.of(new OpenAiResponsesClient.InputMessage("user", firstUserMessage)),
                    "Generate a concise Chinese chat title from the user's message. "
                            + "Return only the title, with no quotes, punctuation, or explanation. Keep it within 20 Chinese characters."
            );
            String generatedTitle = normalizeTitle(generated, firstUserMessage);
            updateTitle(conversationId, fallbackTitle, generatedTitle);
        } catch (Exception exception) {
            LOGGER.warn("AI title generation failed for conversation {}: {}", conversationId, exception.getMessage());
        }
    }

    private void updateTitle(Long conversationId, String expectedTitle, String title) {
        conversationMapper.update(null, new LambdaUpdateWrapper<Conversation>()
                .eq(Conversation::getId, conversationId)
                .eq(Conversation::getDeleted, 0)
                .eq(Conversation::getTitle, expectedTitle)
                .set(Conversation::getTitle, title));
    }

    private String normalizeTitle(String value, String fallback) {
        String candidate = value == null ? "" : value.replaceAll("[\\r\\n]+", " ").trim();
        if (candidate.isBlank()) {
            candidate = fallback.trim();
        }
        return candidate.substring(0, Math.min(candidate.length(), 40));
    }
}

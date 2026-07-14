package xyz.qvtrmx.ai.chat.vo;

public record ChatStreamEvent(String type, String delta, ChatMessageResponse message, String error) {

    public static ChatStreamEvent delta(String value) {
        return new ChatStreamEvent("delta", value, null, null);
    }

    public static ChatStreamEvent done(ChatMessageResponse message) {
        return new ChatStreamEvent("done", null, message, null);
    }

    public static ChatStreamEvent error(String message) {
        return new ChatStreamEvent("error", null, null, message);
    }
}

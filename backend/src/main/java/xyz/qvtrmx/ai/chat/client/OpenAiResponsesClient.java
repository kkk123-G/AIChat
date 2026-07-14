package xyz.qvtrmx.ai.chat.client;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class OpenAiResponsesClient {

    private static final String DEFAULT_INSTRUCTIONS = "You are a helpful AI assistant. Answer accurately and clearly. "
            + "Use Markdown when it makes the response easier to read. Respect the conversation context.";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final URI responsesUri;
    private final String apiKey;
    private final String model;
    private final double temperature;
    private final Duration timeout;

    public OpenAiResponsesClient(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.chat.model}") String model,
            @Value("${spring.ai.openai.chat.temperature:0.7}") double temperature,
            @Value("${spring.ai.openai.chat.timeout:60s}") Duration timeout
    ) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
        this.responsesUri = responsesUri(baseUrl);
        this.apiKey = apiKey;
        this.model = model;
        this.temperature = temperature;
        this.timeout = timeout;
    }

    public Flux<String> stream(List<InputMessage> input) {
        return stream(input, DEFAULT_INSTRUCTIONS);
    }

    public String complete(List<InputMessage> input, String instructions) {
        return stream(input, instructions)
                .reduce(new StringBuilder(), StringBuilder::append)
                .map(StringBuilder::toString)
                .block(timeout);
    }

    private Flux<String> stream(List<InputMessage> input, String instructions) {
        ResponsesRequest request = new ResponsesRequest(model, input, instructions, true, temperature);
        return webClient.post()
                .uri(responsesUri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> new IllegalStateException(
                                "Responses API request failed with HTTP " + response.statusCode().value())))
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .map(this::parseEvent)
                .takeUntil(ResponseEvent::completed)
                .mapNotNull(ResponseEvent::delta);
    }

    private ResponseEvent parseEvent(ServerSentEvent<String> event) {
        String data = event.data();
        if (!StringUtils.hasText(data)) {
            return new ResponseEvent(null, false);
        }
        if ("[DONE]".equals(data)) {
            return new ResponseEvent(null, true);
        }
        try {
            JsonNode payload = objectMapper.readTree(data);
            String type = payload.path("type").asText(event.event());
            if ("response.output_text.delta".equals(type)) {
                return new ResponseEvent(payload.path("delta").asText(), false);
            }
            if ("response.completed".equals(type) || "response.output_text.done".equals(type)) {
                return new ResponseEvent(null, true);
            }
            if ("error".equals(type) || "response.failed".equals(type) || "response.incomplete".equals(type)) {
                String message = payload.path("error").path("message").asText("The AI service returned an error");
                throw new IllegalStateException(message);
            }
            return new ResponseEvent(null, false);
        } catch (JacksonException exception) {
            throw new IllegalStateException("The Responses API returned an invalid event", exception);
        }
    }

    private static URI responsesUri(String baseUrl) {
        String normalized = baseUrl.replaceAll("/+$", "");
        String path = normalized.endsWith("/v1") ? "/responses" : "/v1/responses";
        return URI.create(normalized + path);
    }

    public record InputMessage(String role, String content) {
    }

    private record ResponseEvent(String delta, boolean completed) {
    }

    private record ResponsesRequest(
            String model,
            List<InputMessage> input,
            String instructions,
            boolean stream,
            double temperature
    ) {
    }
}

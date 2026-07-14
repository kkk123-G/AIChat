package xyz.qvtrmx.ai.chat.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.ObjectMapper;

class OpenAiResponsesClientTest {

    private HttpServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/responses", exchange -> {
            String response = "event: response.output_text.delta\n"
                    + "data: {\"type\":\"response.output_text.delta\",\"delta\":\"Hello\"}\n\n"
                    + "event: response.output_text.delta\n"
                    + "data: {\"type\":\"response.output_text.delta\",\"delta\":\" world\"}\n\n"
                    + "event: response.output_text.done\n"
                    + "data: {\"type\":\"response.output_text.done\",\"text\":\"Hello world\"}\n\n";
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().write(response.getBytes(StandardCharsets.UTF_8));
            exchange.getResponseBody().flush();
            try {
                Thread.sleep(3_000);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void streamsOutputTextDeltasFromResponsesApi() {
        OpenAiResponsesClient client = new OpenAiResponsesClient(
                WebClient.builder(),
                new ObjectMapper(),
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "test-key",
                "test-model",
                0.2,
                Duration.ofSeconds(3)
        );

        List<String> deltas = client.stream(List.of(new OpenAiResponsesClient.InputMessage("user", "Hi")))
                .collectList()
                .block(Duration.ofSeconds(1));

        assertEquals(List.of("Hello", " world"), deltas);
    }
}

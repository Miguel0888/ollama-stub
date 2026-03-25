package de.example.ollamaspike.infra.ollama;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OllamaStubServer {

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final String INSTALLED_MODEL = "gpt-oss:20b";
    private static final String STUB_RESPONSE = "Verstanden!";

    private final int port;
    private final StubChatObserver chatObserver;

    private HttpServer server;
    private ExecutorService executorService;

    public OllamaStubServer(int port, StubChatObserver chatObserver) {
        this.port = port;
        this.chatObserver = chatObserver;
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            executorService = Executors.newCachedThreadPool();
            server.setExecutor(executorService);

            server.createContext("/api/tags", new TagsHandler());
            server.createContext("/api/chat", new ChatHandler());

            server.start();
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot start Ollama stub server", ex);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    private final class TagsHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }

            chatObserver.onSystemMessage("Modelle wurden abgefragt.");

            String responseBody =
                    "{"
                            + "\"models\":["
                            + "{"
                            + "\"name\":\"" + INSTALLED_MODEL + "\","
                            + "\"model\":\"" + INSTALLED_MODEL + "\","
                            + "\"modified_at\":\"2026-03-25T00:00:00Z\","
                            + "\"size\":0,"
                            + "\"digest\":\"stub-digest\","
                            + "\"details\":{"
                            + "\"format\":\"gguf\","
                            + "\"family\":\"gpt-oss\","
                            + "\"families\":[\"gpt-oss\"],"
                            + "\"parameter_size\":\"20B\","
                            + "\"quantization_level\":\"Q4_K_M\""
                            + "}"
                            + "}"
                            + "]"
                            + "}";

            sendJson(exchange, 200, responseBody);
        }
    }

    private final class ChatHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }

            String requestBody = readRequestBody(exchange.getRequestBody());
            String userMessage = extractLastMessageContent(requestBody);

            if (userMessage == null || userMessage.trim().isEmpty()) {
                userMessage = "<keine Nachricht gefunden>";
            }

            chatObserver.onUserMessageReceived(userMessage);

            Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", "application/x-ndjson; charset=utf-8");

            exchange.sendResponseHeaders(200, 0); // streaming

            OutputStream os = exchange.getResponseBody();

            try {
                // first chunk
                writeChunk(os, false);

                // final chunk
                writeChunk(os, true);

                os.flush();
            } finally {
                os.close();
            }

            chatObserver.onAssistantMessageSent(STUB_RESPONSE);
        }

        private void writeChunk(OutputStream os, boolean done) throws IOException {
            String chunk =
                    "{"
                            + "\"model\":\"" + INSTALLED_MODEL + "\","
                            + "\"message\":{"
                            + "\"role\":\"assistant\","
                            + "\"content\":\"" + escapeJson(STUB_RESPONSE) + "\""
                            + "},"
                            + "\"done\":" + done
                            + "}\n";

            os.write(chunk.getBytes(UTF_8));
        }
    }

    private void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
    }

    private void sendJson(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] responseBytes = body.getBytes(UTF_8);

        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");

        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        OutputStream outputStream = exchange.getResponseBody();
        try {
            outputStream.write(responseBytes);
            outputStream.flush();
        } finally {
            outputStream.close();
        }
    }

    private String readRequestBody(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int length;

        while ((length = inputStream.read(data)) != -1) {
            buffer.write(data, 0, length);
        }

        return new String(buffer.toByteArray(), UTF_8);
    }

    private String extractLastMessageContent(String requestBody) {
        Pattern pattern = Pattern.compile("\"content\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");
        Matcher matcher = pattern.matcher(requestBody);

        String lastMatch = null;
        while (matcher.find()) {
            lastMatch = matcher.group(1);
        }

        if (lastMatch == null) {
            return null;
        }

        return unescapeJson(lastMatch);
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private String unescapeJson(String value) {
        return value
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
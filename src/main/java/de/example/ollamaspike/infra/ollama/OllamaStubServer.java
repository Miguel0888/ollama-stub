package de.example.ollamaspike.infra.ollama;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public final class OllamaStubServer {

    private final int port;
    private HttpServer httpServer;

    public OllamaStubServer(int port) {
        this.port = port;
    }

    public void start() {
        try {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            httpServer.createContext("/api/version", new StaticJsonHandler("{\"version\":\"0.0.0-stub\"}"));
            httpServer.createContext("/api/tags", new StaticJsonHandler(createTagsResponse()));
            httpServer.createContext("/api/show", new StaticJsonHandler("{\"modelfile\":\"FROM gpt-oss:20b\",\"parameters\":\"\"}"));
            httpServer.createContext("/api/chat", new StaticJsonHandler(createChatResponse()));
            httpServer.setExecutor(Executors.newCachedThreadPool());
            httpServer.start();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not start Ollama stub server on port " + port, exception);
        }
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    public String getBaseUrl() {
        return "http://localhost:" + port;
    }

    private String createTagsResponse() {
        return "{"
                + "\"models\":[{"
                + "\"name\":\"gpt-oss:20b\"," 
                + "\"model\":\"gpt-oss:20b\"," 
                + "\"modified_at\":\"2026-03-25T12:00:00Z\"," 
                + "\"size\":21474836480," 
                + "\"digest\":\"stub-digest\"," 
                + "\"details\":{"
                + "\"format\":\"gguf\"," 
                + "\"family\":\"gpt-oss\"," 
                + "\"parameter_size\":\"20B\"," 
                + "\"quantization_level\":\"Q4_K_M\""
                + "}"
                + "}]"
                + "}";
    }

    private String createChatResponse() {
        return "{"
                + "\"model\":\"gpt-oss:20b\"," 
                + "\"created_at\":\"2026-03-25T12:00:00Z\"," 
                + "\"message\":{"
                + "\"role\":\"assistant\"," 
                + "\"content\":\"Verstanden!\""
                + "},"
                + "\"done\":true"
                + "}";
    }

    private static final class StaticJsonHandler implements HttpHandler {

        private final String responseBody;

        private StaticJsonHandler(String responseBody) {
            this.responseBody = responseBody;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Content-Type", "application/json; charset=UTF-8");
            byte[] payload = responseBody.getBytes("UTF-8");
            exchange.sendResponseHeaders(200, payload.length);
            OutputStream outputStream = exchange.getResponseBody();
            try {
                outputStream.write(payload);
                outputStream.flush();
            } finally {
                outputStream.close();
            }
        }
    }
}

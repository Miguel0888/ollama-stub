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
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OllamaStubServer {

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final String MODEL = "gpt-oss:20b";
    private static final String RESPONSE = "Verstanden!";

    private final int port;
    private HttpServer server;
    private volatile StubChatObserver observer;

    public OllamaStubServer(int port) {
        this.port = port;
    }

    public void setObserver(StubChatObserver observer) {
        this.observer = observer;
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(Executors.newCachedThreadPool());

            server.createContext("/api/tags", new TagsHandler());
            server.createContext("/api/generate", new GenerateHandler());
            server.createContext("/api/chat", new ChatHandler());

            server.start();

            System.out.println("Stub läuft auf " + getBaseUrl());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    public String getBaseUrl() {
        return "http://localhost:" + port;
    }

    // =========================
    // /api/tags
    // =========================
    private static class TagsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {

            String json =
                    "{"
                            + "\"models\":[{"
                            + "\"name\":\"" + MODEL + "\","
                            + "\"model\":\"" + MODEL + "\""
                            + "}]"
                            + "}";

            sendJson(exchange, json);
        }
    }

    // =========================
    // /api/generate
    // =========================
    private class GenerateHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {

            String body = read(exchange.getRequestBody());
            boolean stream = isStream(body);

            System.out.println("GENERATE stream=" + stream);

            // Eingehende User-Nachricht (prompt) extrahieren und Observer benachrichtigen
            String prompt = extractPrompt(body);
            if (prompt != null) {
                System.out.println("USER (generate): " + prompt);
                StubChatObserver obs = observer;
                if (obs != null) {
                    obs.onUserMessageReceived(prompt);
                }
            }

            if (stream) {
                streamGenerate(exchange);
            } else {
                singleGenerate(exchange);
            }

            // Observer über die Antwort informieren
            StubChatObserver obs = observer;
            if (obs != null) {
                obs.onAssistantMessageSent(RESPONSE);
            }
        }

        private void streamGenerate(HttpExchange exchange) throws IOException {
            Headers h = exchange.getResponseHeaders();
            h.set("Content-Type", "application/x-ndjson");

            exchange.sendResponseHeaders(200, 0);
            OutputStream os = exchange.getResponseBody();

            try {
                write(os, false);
                write(os, true);
                os.flush();
            } finally {
                os.close();
            }
        }

        private void singleGenerate(HttpExchange exchange) throws IOException {
            String json =
                    "{"
                            + "\"model\":\"" + MODEL + "\","
                            + "\"response\":\"" + RESPONSE + "\","
                            + "\"done\":true"
                            + "}";

            sendJson(exchange, json);
        }

        private void write(OutputStream os, boolean done) throws IOException {
            String json =
                    "{"
                            + "\"model\":\"" + MODEL + "\","
                            + "\"response\":\"" + RESPONSE + "\","
                            + "\"done\":" + done
                            + "}\n";

            os.write(json.getBytes(UTF_8));
        }
    }

    // =========================
    // /api/chat
    // =========================
    private class ChatHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {

            String body = read(exchange.getRequestBody());
            boolean stream = isStream(body);

            System.out.println("CHAT stream=" + stream);

            // Eingehende User-Nachricht extrahieren und Observer benachrichtigen
            String userContent = extractLastUserContent(body);
            if (userContent != null) {
                System.out.println("USER: " + userContent);
                StubChatObserver obs = observer;
                if (obs != null) {
                    obs.onUserMessageReceived(userContent);
                }
            }

            if (stream) {
                streamChat(exchange);
            } else {
                singleChat(exchange);
            }

            // Observer über die Antwort informieren
            StubChatObserver obs = observer;
            if (obs != null) {
                obs.onAssistantMessageSent(RESPONSE);
            }
        }

        private void streamChat(HttpExchange exchange) throws IOException {
            Headers h = exchange.getResponseHeaders();
            h.set("Content-Type", "application/x-ndjson");

            exchange.sendResponseHeaders(200, 0);
            OutputStream os = exchange.getResponseBody();

            try {
                write(os, false);
                write(os, true);
                os.flush();
            } finally {
                os.close();
            }
        }

        private void singleChat(HttpExchange exchange) throws IOException {
            String json =
                    "{"
                            + "\"model\":\"" + MODEL + "\","
                            + "\"message\":{"
                            + "\"role\":\"assistant\","
                            + "\"content\":\"" + RESPONSE + "\""
                            + "},"
                            + "\"done\":true"
                            + "}";

            sendJson(exchange, json);
        }

        private void write(OutputStream os, boolean done) throws IOException {
            String json =
                    "{"
                            + "\"model\":\"" + MODEL + "\","
                            + "\"message\":{"
                            + "\"role\":\"assistant\","
                            + "\"content\":\"" + RESPONSE + "\""
                            + "},"
                            + "\"done\":" + done
                            + "}\n";

            os.write(json.getBytes(UTF_8));
        }
    }

    // =========================
    // helpers
    // =========================
    private static void sendJson(HttpExchange ex, String body) throws IOException {
        byte[] bytes = body.getBytes(UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(200, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    private static String read(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int r;

        while ((r = is.read(buf)) != -1) {
            out.write(buf, 0, r);
        }

        return new String(out.toByteArray(), UTF_8);
    }

    private static boolean isStream(String body) {
        return body != null && body.contains("\"stream\":true");
    }

    /**
     * Extrahiert den "prompt"-Wert aus dem /api/generate Request-Body.
     * Erwartet JSON mit "prompt":"..."
     */
    static String extractPrompt(String body) {
        if (body == null) {
            return null;
        }
        Pattern p = Pattern.compile("\"prompt\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        Matcher m = p.matcher(body);
        if (m.find()) {
            return m.group(1)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        }
        return null;
    }

    /**
     * Extrahiert den Inhalt der letzten User-Nachricht aus dem /api/chat Request-Body.
     * Erwartet JSON mit "messages":[{"role":"user","content":"..."},...]
     */
    static String extractLastUserContent(String body) {
        if (body == null) {
            return null;
        }
        Pattern p = Pattern.compile(
                "\"role\"\\s*:\\s*\"user\"[^}]*?\"content\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        Matcher m = p.matcher(body);
        String last = null;
        while (m.find()) {
            last = m.group(1);
        }
        if (last == null) {
            // Fallback: content kann vor role stehen
            Pattern p2 = Pattern.compile(
                    "\"content\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"[^}]*?\"role\"\\s*:\\s*\"user\"");
            Matcher m2 = p2.matcher(body);
            while (m2.find()) {
                last = m2.group(1);
            }
        }
        if (last != null) {
            return last
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        }
        return null;
    }
}
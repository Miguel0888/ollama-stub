package de.example.ollamaspike.infra.ollama;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OllamaStubServer {

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final String MODEL = "gpt-oss:20b";

    private final int port;
    private HttpServer server;

    public OllamaStubServer(int port) {
        this.port = port;
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(Executors.newCachedThreadPool());

            server.createContext("/api/tags", new TagsHandler());
            server.createContext("/api/chat", new ChatHandler());
            server.createContext("/api/generate", new GenerateHandler());

            server.start();

            System.out.println("Stub läuft auf http://localhost:" + port);

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

    private static String extractPrompt(String body) {
        Pattern p = Pattern.compile("\"prompt\"\\s*:\\s*\"(.*?)\"");
        Matcher m = p.matcher(body);

        if (m.find()) {
            return m.group(1);
        }
        return "";
    }
    
    // -------------------------
    // /api/tags
    // -------------------------
    private static class TagsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {

            String response =
                    "{"
                            + "\"models\":[{"
                            + "\"name\":\"" + MODEL + "\","
                            + "\"model\":\"" + MODEL + "\""
                            + "}]"
                            + "}";

            byte[] bytes = response.getBytes(UTF_8);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);

            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    // -------------------------
    // /api/chat (WICHTIG!)
    // -------------------------
    private static class ChatHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {

            String body = read(exchange.getRequestBody());
            String userMessage = extractLastMessage(body);

            System.out.println("CLIENT: " + userMessage);

            Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", "application/x-ndjson");

            // VERY IMPORTANT: streaming response
            exchange.sendResponseHeaders(200, 0);

            OutputStream os = exchange.getResponseBody();

            try {
                // chunk 1
                writeChunk(os, false);

                // chunk 2 (done)
                writeChunk(os, true);

                os.flush();
            } finally {
                os.close();
            }

            System.out.println("STUB: Verstanden!");
        }

        private void writeChunk(OutputStream os, boolean done) throws IOException {

            String json =
                    "{"
                            + "\"model\":\"" + MODEL + "\","
                            + "\"message\":{"
                            + "\"role\":\"assistant\","
                            + "\"content\":\"Verstanden!\""
                            + "},"
                            + "\"done\":" + done
                            + "}\n";

            os.write(json.getBytes(UTF_8));
        }
    }

    private static class GenerateHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {

            String requestBody = read(exchange.getRequestBody());
            String prompt = extractPrompt(requestBody);

            System.out.println("CLIENT: " + prompt);

            Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", "application/x-ndjson");

            exchange.sendResponseHeaders(200, 0);

            OutputStream os = exchange.getResponseBody();

            try {
                writeChunk(os, false);
                writeChunk(os, true);
                os.flush();
            } finally {
                os.close();
            }

            System.out.println("STUB: Verstanden!");
        }

        private void writeChunk(OutputStream os, boolean done) throws IOException {

            String json =
                    "{"
                            + "\"model\":\"gpt-oss:20b\","
                            + "\"response\":\"Verstanden!\","
                            + "\"done\":" + done
                            + "}\n";

            os.write(json.getBytes(Charset.forName("UTF-8")));
        }
    }

    // -------------------------
    // helper
    // -------------------------
    private static String read(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int r;
        while ((r = is.read(buf)) != -1) {
            out.write(buf, 0, r);
        }
        return new String(out.toByteArray(), UTF_8);
    }

    private static String extractLastMessage(String body) {
        Pattern p = Pattern.compile("\"content\"\\s*:\\s*\"(.*?)\"");
        Matcher m = p.matcher(body);

        String last = null;
        while (m.find()) {
            last = m.group(1);
        }

        return last != null ? last : "";
    }
}
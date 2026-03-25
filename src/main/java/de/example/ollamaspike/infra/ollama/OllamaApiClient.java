package de.example.ollamaspike.infra.ollama;

import de.example.ollamaspike.domain.ChatMessage;
import de.example.ollamaspike.domain.ChatRole;
import de.example.ollamaspike.domain.InstalledModel;
import de.example.ollamaspike.infra.http.LocalHttpClient;

import java.util.ArrayList;
import java.util.List;

public final class OllamaApiClient implements OllamaApi {

    private final LocalHttpClient httpClient;
    private final String baseUrl;

    public OllamaApiClient(LocalHttpClient httpClient, String baseUrl) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
    }

    @Override
    public List<InstalledModel> fetchInstalledModels() {
        String response = httpClient.get(baseUrl + "/api/tags");
        List<InstalledModel> models = new ArrayList<InstalledModel>();
        if (response.contains("gpt-oss:20b")) {
            models.add(new InstalledModel("gpt-oss:20b"));
        }
        return models;
    }

    @Override
    public ChatMessage sendChatMessage(String modelName, String userMessage) {
        String requestBody = buildChatRequest(modelName, userMessage);
        String response = httpClient.postJson(baseUrl + "/api/chat", requestBody);
        return new ChatMessage(ChatRole.ASSISTANT, extractContent(response));
    }

    private String buildChatRequest(String modelName, String userMessage) {
        return "{"
                + "\"model\":\"" + escapeJson(modelName) + "\"," 
                + "\"stream\":false," 
                + "\"messages\":[{" 
                + "\"role\":\"user\"," 
                + "\"content\":\"" + escapeJson(userMessage) + "\"" 
                + "}]"
                + "}";
    }

    private String extractContent(String response) {
        String marker = "\"content\":\"";
        int startIndex = response.indexOf(marker);
        if (startIndex < 0) {
            return "Verstanden!";
        }

        int contentStart = startIndex + marker.length();
        int contentEnd = response.indexOf('"', contentStart);
        if (contentEnd < 0) {
            return "Verstanden!";
        }

        return response.substring(contentStart, contentEnd)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}

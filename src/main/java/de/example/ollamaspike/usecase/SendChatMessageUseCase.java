package de.example.ollamaspike.usecase;

import de.example.ollamaspike.domain.ChatMessage;
import de.example.ollamaspike.infra.ollama.OllamaApi;

public final class SendChatMessageUseCase {

    private final OllamaApi ollamaApi;

    public SendChatMessageUseCase(OllamaApi ollamaApi) {
        this.ollamaApi = ollamaApi;
    }

    public ChatMessage execute(String modelName, String userMessage) {
        return ollamaApi.sendChatMessage(modelName, userMessage);
    }
}

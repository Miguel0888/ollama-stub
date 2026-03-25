package de.example.ollamaspike.infra.ollama;

import de.example.ollamaspike.domain.ChatMessage;
import de.example.ollamaspike.domain.InstalledModel;

import java.util.List;

public interface OllamaApi {

    List<InstalledModel> fetchInstalledModels();

    ChatMessage sendChatMessage(String modelName, String userMessage);
}

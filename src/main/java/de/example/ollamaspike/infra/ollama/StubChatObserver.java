package de.example.ollamaspike.infra.ollama;

public interface StubChatObserver {

    void onUserMessageReceived(String message);

    void onAssistantMessageSent(String message);

    void onSystemMessage(String message);
}
package de.example.ollamaspike.domain;

public final class ChatMessage {

    private final ChatRole role;
    private final String text;

    public ChatMessage(ChatRole role, String text) {
        this.role = role;
        this.text = text;
    }

    public ChatRole getRole() {
        return role;
    }

    public String getText() {
        return text;
    }
}

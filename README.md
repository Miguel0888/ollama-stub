# Ollama Swing Spike

Kleiner Gradle-Spike mit zwei Bausteinen:

1. Einem lokalen Ollama-Stub-Endpunkt auf `http://localhost:11434`
2. Einer Swing-Chat-UI im Stil des MainframeMate-Chats

## Verhalten

- `GET /api/tags` meldet genau ein installiertes Modell: `gpt-oss:20b`
- `POST /api/chat` antwortet immer mit `Verstanden!`
- Die UI zeigt User- und Bot-Nachrichten als farbige Chat-Karten

## Start

```bash
gradle run
```

## Hinweis

Der Spike ist bewusst klein gehalten und vorbereitet, um später in eine größere Anwendung integriert zu werden.

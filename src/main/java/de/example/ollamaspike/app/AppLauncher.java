package de.example.ollamaspike.app;

import de.example.ollamaspike.infra.ollama.OllamaStubServer;
import de.example.ollamaspike.ui.MainWindow;

import javax.swing.SwingUtilities;

public final class AppLauncher {

    private AppLauncher() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MainWindow mainWindow = new MainWindow();
                mainWindow.setVisible(true);

                OllamaStubServer stubServer = new OllamaStubServer(11434, mainWindow);
                stubServer.start();

                mainWindow.showSystemMessage("Ollama-Stub läuft auf http://localhost:11434");
                mainWindow.showSystemMessage("GET /api/tags meldet Modell gpt-oss:20b");
                mainWindow.showSystemMessage("POST /api/chat antwortet immer mit \"Verstanden!\"");
            }
        });
    }
}
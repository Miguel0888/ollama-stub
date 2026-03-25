package de.example.ollamaspike.app;

import de.example.ollamaspike.infra.http.LocalHttpClient;
import de.example.ollamaspike.infra.ollama.OllamaApiClient;
import de.example.ollamaspike.infra.ollama.OllamaStubServer;
import de.example.ollamaspike.ui.MainWindow;
import de.example.ollamaspike.usecase.SendChatMessageUseCase;

import javax.swing.SwingUtilities;

public final class AppLauncher {

    private AppLauncher() {
    }

    public static void main(String[] args) {
        OllamaStubServer stubServer = new OllamaStubServer(11434);
        stubServer.start();

        LocalHttpClient httpClient = new LocalHttpClient();
        OllamaApiClient ollamaApiClient = new OllamaApiClient(httpClient, "http://localhost:11434");
        SendChatMessageUseCase sendChatMessageUseCase = new SendChatMessageUseCase(ollamaApiClient);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MainWindow mainWindow = new MainWindow(sendChatMessageUseCase, ollamaApiClient, stubServer);
                mainWindow.showWindow();
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                stubServer.stop();
            }
        }));
    }
}

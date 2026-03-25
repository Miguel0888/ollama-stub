package de.example.ollamaspike.ui;

import de.example.ollamaspike.domain.InstalledModel;
import de.example.ollamaspike.infra.ollama.OllamaApiClient;
import de.example.ollamaspike.infra.ollama.OllamaStubServer;
import de.example.ollamaspike.usecase.SendChatMessageUseCase;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

public final class MainWindow {

    private final JFrame frame;

    public MainWindow(SendChatMessageUseCase sendChatMessageUseCase,
                      OllamaApiClient ollamaApiClient,
                      OllamaStubServer stubServer) {
        this.frame = new JFrame("Ollama Swing Spike");
        configureFrame();

        List<InstalledModel> installedModels = ollamaApiClient.fetchInstalledModels();
        String[] modelNames = extractModelNames(installedModels);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("💬 chat-1", new ChatPanel(sendChatMessageUseCase, modelNames));
        tabs.addTab("＋", createAddTabPlaceholder());

        frame.add(createInfoPanel(stubServer.getBaseUrl(), modelNames), BorderLayout.NORTH);
        frame.add(tabs, BorderLayout.CENTER);
    }

    public void showWindow() {
        frame.setVisible(true);
    }

    private void configureFrame() {
        frame.setSize(960, 720);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
    }

    private JPanel createInfoPanel(String baseUrl, String[] modelNames) {
        JPanel infoPanel = new JPanel(new BorderLayout());
        String modelText = modelNames.length == 0 ? "kein Modell" : modelNames[0];
        infoPanel.add(new JLabel("Stub läuft auf " + baseUrl + " | Modell: " + modelText), BorderLayout.WEST);
        return infoPanel;
    }

    private JPanel createAddTabPlaceholder() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Weitere Tabs können im nächsten Schritt ergänzt werden.", SwingConstants.CENTER), BorderLayout.CENTER);
        return panel;
    }

    private String[] extractModelNames(List<InstalledModel> installedModels) {
        List<String> names = new ArrayList<String>();
        for (InstalledModel installedModel : installedModels) {
            names.add(installedModel.getName());
        }
        return names.toArray(new String[names.size()]);
    }
}

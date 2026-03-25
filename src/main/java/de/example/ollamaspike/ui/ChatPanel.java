package de.example.ollamaspike.ui;

import de.example.ollamaspike.domain.ChatMessage;
import de.example.ollamaspike.domain.ChatRole;
import de.example.ollamaspike.infra.ollama.StubChatObserver;
import de.example.ollamaspike.usecase.SendChatMessageUseCase;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public final class ChatPanel extends JPanel implements StubChatObserver {

    private final JPanel messageContainer;
    private final JTextArea inputArea;
    private final JLabel statusLabel;
    private final SendChatMessageUseCase sendChatMessageUseCase;
    private final JComboBox<String> modelSelection;

    public ChatPanel(SendChatMessageUseCase sendChatMessageUseCase, String[] installedModels) {
        this.sendChatMessageUseCase = sendChatMessageUseCase;
        this.messageContainer = new JPanel();
        this.inputArea = new JTextArea(3, 30);
        this.statusLabel = new JLabel("Bereit");
        this.modelSelection = new JComboBox<String>(installedModels);

        buildUi();
    }

    private void buildUi() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(createHeader(), BorderLayout.NORTH);
        add(createChatArea(), BorderLayout.CENTER);
        add(createInputArea(), BorderLayout.SOUTH);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());

        JLabel titleLabel = new JLabel("Chat");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));

        JPanel rightSide = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightSide.add(new JLabel("Modell:"));
        rightSide.add(modelSelection);

        header.add(titleLabel, BorderLayout.WEST);
        header.add(rightSide, BorderLayout.EAST);
        return header;
    }

    private JScrollPane createChatArea() {
        messageContainer.setLayout(new BoxLayout(messageContainer, BoxLayout.Y_AXIS));
        messageContainer.setBackground(new Color(245, 245, 245));

        JScrollPane scrollPane = new JScrollPane(messageContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    private JPanel createInputArea() {
        JPanel wrapper = new JPanel(new BorderLayout(8, 8));

        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.ITALIC, 11f));
        wrapper.add(statusLabel, BorderLayout.NORTH);

        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setFont(new Font("SansSerif", Font.PLAIN, 15));
        inputArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_ENTER && !event.isShiftDown()) {
                    event.consume();
                    sendMessage();
                }
            }
        });

        JScrollPane inputScrollPane = new JScrollPane(inputArea);
        inputScrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        wrapper.add(inputScrollPane, BorderLayout.CENTER);

        JButton sendButton = new JButton("⏎");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                sendMessage();
            }
        });
        wrapper.add(sendButton, BorderLayout.EAST);
        return wrapper;
    }

    private void sendMessage() {
        final String userText = inputArea.getText().trim();
        if (userText.isEmpty()) {
            return;
        }

        appendMessage(new ChatMessage(ChatRole.USER, userText));
        inputArea.setText("");
        statusLabel.setText("Stub antwortet...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                final ChatMessage response = sendChatMessageUseCase.execute((String) modelSelection.getSelectedItem(), userText);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        appendMessage(response);
                        statusLabel.setText("Bereit");
                    }
                });
            }
        }, "ollama-chat-request").start();
    }

    private void appendMessage(ChatMessage message) {
        String title;
        Color backgroundColor;
        if (message.getRole() == ChatRole.USER) {
            title = "👤 Du:";
            backgroundColor = new Color(230, 240, 255);
        } else if (message.getRole() == ChatRole.SYSTEM) {
            title = "⚙ System:";
            backgroundColor = new Color(255, 255, 210);
        } else {
            title = "🤖 Bot:";
            backgroundColor = new Color(240, 255, 230);
        }

        System.out.println("[ChatPanel] appendMessage: role=" + message.getRole() + " text=" + message.getText());

        messageContainer.add(new ChatBubble(title, message.getText(), backgroundColor));
        messageContainer.add(Box.createVerticalStrut(6));
        messageContainer.revalidate();
        messageContainer.repaint();
        scrollToBottom();
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (messageContainer.getParent() instanceof javax.swing.JViewport) {
                    javax.swing.JViewport viewport = (javax.swing.JViewport) messageContainer.getParent();
                    viewport.setViewPosition(new java.awt.Point(0, Math.max(0, messageContainer.getHeight())));
                }
            }
        });
    }

    // =========================
    // StubChatObserver – Nachrichten von MainframeMate anzeigen
    // =========================

    @Override
    public void onUserMessageReceived(final String message) {
        System.out.println("[ChatPanel.onUserMessageReceived] " + message);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                appendMessage(new ChatMessage(ChatRole.USER, message));
            }
        });
    }

    @Override
    public void onAssistantMessageSent(final String message) {
        System.out.println("[ChatPanel.onAssistantMessageSent] " + message);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                appendMessage(new ChatMessage(ChatRole.ASSISTANT, message));
            }
        });
    }

    @Override
    public void onSystemMessage(final String message) {
        System.out.println("[ChatPanel.onSystemMessage] " + message);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                appendMessage(new ChatMessage(ChatRole.SYSTEM, message));
                statusLabel.setText(message);
            }
        });
    }
}

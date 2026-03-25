package de.example.ollamaspike.ui;

import de.example.ollamaspike.infra.ollama.StubChatObserver;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

public final class MainWindow extends JFrame implements StubChatObserver {

    private final JPanel messageContainer;
    private final JScrollPane scrollPane;

    public MainWindow() {
        setTitle("MainframeMate - Ollama Stub Spike");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);

        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(new Color(24, 28, 34));
        setContentPane(rootPanel);

        rootPanel.add(createHeader(), BorderLayout.NORTH);

        messageContainer = new JPanel();
        messageContainer.setLayout(new BoxLayout(messageContainer, BoxLayout.Y_AXIS));
        messageContainer.setBackground(new Color(30, 34, 40));
        messageContainer.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        scrollPane = new JScrollPane(messageContainer);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        rootPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(18, 22, 28));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel titleLabel = new JLabel("Chat Monitor");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18.0f));

        JLabel subtitleLabel = new JLabel("Zeigt eingehende /api/chat Requests und Stub-Antworten");
        subtitleLabel.setForeground(new Color(170, 180, 190));
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(Font.PLAIN, 12.0f));

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(subtitleLabel);

        headerPanel.add(textPanel, BorderLayout.WEST);
        return headerPanel;
    }

    @Override
    public void onUserMessageReceived(final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                addMessage("Client", message, MessageAlignment.RIGHT, new Color(55, 110, 210));
            }
        });
    }

    @Override
    public void onAssistantMessageSent(final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                addMessage("Ollama Stub", message, MessageAlignment.LEFT, new Color(70, 76, 86));
            }
        });
    }

    @Override
    public void onSystemMessage(final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                addSystemInfo(message);
            }
        });
    }

    public void showSystemMessage(String message) {
        onSystemMessage(message);
    }

    private void addSystemInfo(String message) {
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setForeground(new Color(150, 160, 170));
        label.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        messageContainer.add(label);
        messageContainer.add(Box.createVerticalStrut(10));

        refreshMessages();
    }

    private void addMessage(String sender, String message, MessageAlignment alignment, Color bubbleColor) {
        JPanel rowPanel = new JPanel(new BorderLayout());
        rowPanel.setOpaque(false);
        rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel bubble = createBubble(sender, message, bubbleColor);

        if (alignment == MessageAlignment.LEFT) {
            rowPanel.add(bubble, BorderLayout.WEST);
        } else {
            rowPanel.add(bubble, BorderLayout.EAST);
        }

        messageContainer.add(rowPanel);
        messageContainer.add(Box.createVerticalStrut(12));

        refreshMessages();
    }

    private JPanel createBubble(String sender, String message, Color bubbleColor) {
        JPanel bubble = new JPanel(new BorderLayout());
        bubble.setBackground(bubbleColor);
        bubble.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(2, 2, 2, 2),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JLabel senderLabel = new JLabel(sender);
        senderLabel.setForeground(new Color(230, 235, 240));
        senderLabel.setFont(senderLabel.getFont().deriveFont(Font.BOLD, 12.0f));

        JLabel messageLabel = new JLabel(toHtml(message));
        messageLabel.setForeground(Color.WHITE);
        messageLabel.setFont(UIManager.getFont("Label.font"));
        messageLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        bubble.add(senderLabel, BorderLayout.NORTH);
        bubble.add(messageLabel, BorderLayout.CENTER);
        bubble.setMaximumSize(new Dimension(520, Integer.MAX_VALUE));

        return bubble;
    }

    private String toHtml(String text) {
        String escaped = text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br/>");

        return "<html><body style='width: 360px'>" + escaped + "</body></html>";
    }

    private void refreshMessages() {
        messageContainer.revalidate();
        messageContainer.repaint();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                scrollPane.getVerticalScrollBar().setValue(
                        scrollPane.getVerticalScrollBar().getMaximum()
                );
            }
        });
    }

    private enum MessageAlignment {
        LEFT,
        RIGHT
    }
}
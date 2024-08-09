import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Client extends JFrame {
    private static final char PREFIX = '/';
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 23456;
    private static final int MAX_MESSAGES_TO_KEEP = 30;

    private static Socket socket;
    private static BufferedReader in;
    private static PrintWriter out;

    private static String username = null;

    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JComboBox<String> commandComboBox;
    private List<String> messageHistory;
    private int messageHistoryIndex;

    private class EmojiKeyboard extends JPanel {
        private final String[] emojis = {"😊", "😂", "😍", "😎", "👍", "❤️", "🤗", "😑", "😉", "😋"};

        public EmojiKeyboard() {
            setLayout(new GridLayout(2, emojis.length / 2));

            for (String emoji : emojis) {
                JButton emojiButton = new JButton(emoji);
                emojiButton.addActionListener(new EmojiButtonListener());
                add(emojiButton);
            }
        }

        private class EmojiButtonListener implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                JButton button = (JButton) e.getSource();
                String emoji = button.getText();
                messageField.setText(messageField.getText() + emoji);
            }
        }



    }

    public Client() {
        setTitle("Chat Client");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        chatArea = new JTextArea(20, 40);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        inputPanel.add(messageField, BorderLayout.CENTER);

        JButton emojiButton = new JButton("😊");
        emojiButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showEmojiKeyboard();
            }
        });
        inputPanel.add(emojiButton, BorderLayout.WEST);

        sendButton = new JButton("Send");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        messageField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        inputPanel.add(sendButton, BorderLayout.EAST);

        commandComboBox = new JComboBox<>(new String[]{"/help", "/username ", "/clear", "/ping", "/msg", "/rmsg", "/quit", "/reconnect", "/list"});
        commandComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedCommand = (String) commandComboBox.getSelectedItem();
                messageField.setText(selectedCommand);
            }
        });
        inputPanel.add(commandComboBox, BorderLayout.NORTH);

        panel.add(inputPanel, BorderLayout.SOUTH);

        add(panel);

        setVisible(true);

        connectToServer();
        setUsername();

        messageHistory = new ArrayList<>();
        messageHistoryIndex = -1;

        try {
            String userID = in.readLine();
            chatArea.append("Kullanıcı Kimliğiniz: " + userID + "\n");

            new Thread(() -> {
                try {
                    while (true) {
                        String message = in.readLine();
                        if (message == null) {
                            chatArea.append("\u001B[31m- SERVER OFFLINE -\u001B[0m \n");
                            reconnect();
                        } else {
                            appendToMessageHistory(message);
                            chatArea.append(message + "\n");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Add key listener for message history navigation
        messageField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    navigateMessageHistoryBackward();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    navigateMessageHistoryForward();
                }
            }
        });
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            chatArea.append("Sunucuya bağlanılamadı. Tekrar denenecek...\n");
            reconnect();
        }
    }

    private void reconnect() {
        try {
            socket.close();
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        connectToServer();
        setUsername();
    }

    private boolean isValidUsername(String username) {
        return username.matches("^[a-zA-Z0-9]+#[0-9]{4}$");
    }

    private void setUsername() {
        while (username == null) {
            username = JOptionPane.showInputDialog("Kullanıcı Adınızı Girin:");
            if (!isValidUsername(username)) {
                JOptionPane.showMessageDialog(this, "Geçersiz kullanıcı adı. Kullanıcı adı formatı: username#tag (tag 4 karakter olmalı)", "Hata", JOptionPane.ERROR_MESSAGE);
                username = null;
            }
        }

        out.println(username);
    }

    private void sendMessage() {
        String message = messageField.getText();
        if (!message.trim().isEmpty()) {
            processClientCommand(message);
            messageField.setText("");
        }
    }

    private void processClientCommand(String message) {
        if (message.startsWith(String.valueOf(PREFIX))) {
            if (message.equals(PREFIX + "quit")) {
                disconnect();
            } else if (message.equals(PREFIX + "reconnect")) {
                reconnect();
            } else if (message.equals(PREFIX + "cls")) {
                clearChat();
            } else {
                out.println(message);
            }
        } else {
            out.println(username + " > " + message);
        }
    }

    private void disconnect() {
        try {
            socket.close();
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        JOptionPane.showMessageDialog(this, "Sunucudan başarıyla ayrıldınız.", "Bağlantı Kesildi", JOptionPane.INFORMATION_MESSAGE);
        System.exit(0);
    }

    private void clearChat() {
        chatArea.setText("");
    }

    private void showEmojiKeyboard() {
        JFrame emojiFrame = new JFrame("Emoji Keyboard");
        emojiFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        EmojiKeyboard emojiKeyboard = new EmojiKeyboard();
        emojiFrame.add(emojiKeyboard);

        emojiFrame.pack();
        emojiFrame.setLocationRelativeTo(messageField);
        emojiFrame.setVisible(true);
    }

    private void appendToMessageHistory(String message) {
        String[] parts = message.split(" > ", 2);

        if (parts.length == 2) {
            String lastMessage = parts[1];
            messageHistory.add(lastMessage);
            if (messageHistory.size() > MAX_MESSAGES_TO_KEEP) {
                messageHistory.remove(0);
            }
            messageHistoryIndex = messageHistory.size();
        }
    }


    private void navigateMessageHistoryBackward() {
        if (messageHistoryIndex > 0) {
            messageHistoryIndex--;
            String previousMessage = messageHistory.get(messageHistoryIndex);
            messageField.setText(previousMessage);
        }
    }

    private void navigateMessageHistoryForward() {
        if (messageHistoryIndex < messageHistory.size() - 1) {
            messageHistoryIndex++;
            String nextMessage = messageHistory.get(messageHistoryIndex);
            messageField.setText(nextMessage);
        } else if (messageHistoryIndex == messageHistory.size() - 1) {
            messageField.setText("");
            messageHistoryIndex++;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Client();
            }
        });
    }
}

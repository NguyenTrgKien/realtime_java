package client.chat;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.*;

public class ChatFrame extends JFrame {

    private JTextArea txtChatArea;
    private JTextField txtInput;
    private JButton btnSend;
    private JLabel lblStatus;
    private PrintWriter out;
    private String username;
    private JList<String> userList;
    private DefaultListModel<String> listModel;

    public ChatFrame(String username, String serverIP) {
        this.username = username;

        setTitle("Chat - " + username);
        setSize(500, 550);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(400, 400));

        buildUI();
        setVisible(true);

        // Kết nối server trong thread riêng
        new Thread(() -> connectToServer(serverIP)).start();
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        listModel = new DefaultListModel<>();
        userList = new JList<>(listModel);
        
        userList.setBorder(BorderFactory.createTitledBorder("Online Users"));
        add(new JScrollPane(userList), BorderLayout.WEST);

        // ── Header ──
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0, 120, 215));
        header.setBorder(new EmptyBorder(10, 15, 10, 15));

        JLabel lblTitle = new JLabel("💬 " + username);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 16));
        lblTitle.setForeground(Color.WHITE);

        lblStatus = new JLabel("⏳ Đang kết nối...");
        lblStatus.setFont(new Font("Arial", Font.PLAIN, 12));
        lblStatus.setForeground(new Color(200, 230, 255));

        header.add(lblTitle, BorderLayout.WEST);
        header.add(lblStatus, BorderLayout.EAST);

        // ── Chat Area ──
        txtChatArea = new JTextArea();
        txtChatArea.setEditable(false);
        txtChatArea.setFont(new Font("Arial", Font.PLAIN, 14));
        txtChatArea.setBackground(new Color(250, 250, 250));
        txtChatArea.setLineWrap(true);
        txtChatArea.setWrapStyleWord(true);
        txtChatArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(txtChatArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));

        // ── Bottom Panel ──
        JPanel bottomPanel = new JPanel(new BorderLayout(8, 0));
        bottomPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        bottomPanel.setBackground(Color.WHITE);

        txtInput = new JTextField();
        txtInput.setFont(new Font("Arial", Font.PLAIN, 14));
        txtInput.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 120, 215)),
            new EmptyBorder(5, 10, 5, 10)
        ));

        btnSend = new JButton("Gửi ➤");
        btnSend.setBackground(new Color(0, 120, 215));
        btnSend.setForeground(Color.WHITE);
        btnSend.setFont(new Font("Arial", Font.BOLD, 13));
        btnSend.setFocusPainted(false);
        btnSend.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSend.setPreferredSize(new Dimension(90, 36));

        bottomPanel.add(txtInput, BorderLayout.CENTER);
        bottomPanel.add(btnSend, BorderLayout.EAST);

        // ── Ghép layout ──
        add(header, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // ── Sự kiện ──
        btnSend.addActionListener(e -> sendMessage());
        txtInput.addActionListener(e -> sendMessage());
    }

    private void sendMessage() {
        String msg = txtInput.getText().trim();
        if (!msg.isEmpty() && out != null) {
            String receiver = userList.getSelectedValue();

            if(receiver == null){
                JOptionPane.showMessageDialog(this,"Chọn người để chat");
                return;
            }

            out.println("__MSG__" + username + "|" + receiver + "|" + msg);
            txtInput.setText("");
        }
    }

    private void appendMessage(String msg, Color color) {
        SwingUtilities.invokeLater(() -> {
            txtChatArea.append(msg + "\n");
            // Tự cuộn xuống cuối
            txtChatArea.setCaretPosition(txtChatArea.getDocument().getLength());
        });
    }

    private void connectToServer(String ip) {
        try {
            Socket socket = new Socket(ip, 5000);
            out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));

            SwingUtilities.invokeLater(() -> {
                lblStatus.setText("🟢 Đã kết nối");
                lblStatus.setForeground(new Color(180, 255, 180));
            });

            // Thông báo tên lên server
            out.println("__JOIN__" + username);

            String msg;
            while ((msg = in.readLine()) != null) {
                final String finalMsg = msg;
                
                if(msg.startsWith("__USERS__")){
    
                    String users = msg.substring(9);
                    String[] arr = users.split(",");

                    SwingUtilities.invokeLater(() -> {
                        listModel.clear();
                        for(String u : arr){
                            if(!u.equals(username)){
                                listModel.addElement(u);
                            }
                        }
                    });

                }
                else{
                    appendMessage(msg, Color.DARK_GRAY);
                }
                
            }

        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                lblStatus.setText("🔴 Mất kết nối");
                appendMessage("❌ Không thể kết nối tới server!", Color.RED);
            });
        }
    }
}
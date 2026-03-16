package client.chat;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    private JTextField txtUsername;
    private JTextField txtServerIP;
    private JButton btnConnect;

    public LoginFrame() {
        setTitle("Đăng nhập Chat");
        setSize(380, 220);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Panel chính
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(245, 245, 245));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 5, 8, 5);

        // Tiêu đề
        JLabel title = new JLabel("💬 Java Chat App", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setForeground(new Color(0, 120, 215));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(title, gbc);

        // Username
        gbc.gridwidth = 1; gbc.gridy = 1; gbc.gridx = 0;
        panel.add(new JLabel("Tên của bạn:"), gbc);
        txtUsername = new JTextField("User1");
        gbc.gridx = 1;
        panel.add(txtUsername, gbc);

        // Server IP
        gbc.gridy = 2; gbc.gridx = 0;
        panel.add(new JLabel("IP Server:"), gbc);
        txtServerIP = new JTextField("localhost");
        gbc.gridx = 1;
        panel.add(txtServerIP, gbc);

        // Nút kết nối
        btnConnect = new JButton("Kết nối");
        btnConnect.setBackground(new Color(0, 120, 215));
        btnConnect.setForeground(Color.WHITE);
        btnConnect.setFont(new Font("Arial", Font.BOLD, 14));
        btnConnect.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnConnect.setFocusPainted(false);
        gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 2;
        panel.add(btnConnect, gbc);

        add(panel);
        setVisible(true);

        // Sự kiện nút kết nối
        btnConnect.addActionListener(e -> {
            String username = txtUsername.getText().trim();
            String ip = txtServerIP.getText().trim();

            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập tên!");
                return;
            }

            dispose(); // Đóng LoginFrame
            new ChatFrame(username, ip); // Mở ChatFrame
        });
    }
}
package client.chat;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class LoginFrame extends JFrame {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JTextField txtServerIP;
    private JButton btnConnect;
    private JButton btnRegister;

    public LoginFrame() {
        setTitle("Đăng nhập");
        setSize(380, 260);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(245, 245, 245));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 5, 8, 5);

        // Title
        JLabel title = new JLabel("💬 Java Chat App", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setForeground(new Color(0, 120, 215));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(title, gbc);

        // Username
        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0;
        panel.add(new JLabel("Tên:"), gbc);

        txtUsername = new JTextField();
        gbc.gridx = 1;
        panel.add(txtUsername, gbc);

        // Password
        gbc.gridy = 2; gbc.gridx = 0;
        panel.add(new JLabel("Mật khẩu:"), gbc);

        txtPassword = new JPasswordField();
        gbc.gridx = 1;
        panel.add(txtPassword, gbc);

        // IP
        gbc.gridy = 3; gbc.gridx = 0;
        panel.add(new JLabel("IP Server:"), gbc);

        txtServerIP = new JTextField("127.0.0.1");
        gbc.gridx = 1;
        panel.add(txtServerIP, gbc);

        // Buttons
        btnConnect = new JButton("Đăng nhập");
        btnRegister = new JButton("Đăng ký");

        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        btnPanel.add(btnConnect);
        btnPanel.add(btnRegister);

        gbc.gridy = 4; gbc.gridx = 0; gbc.gridwidth = 2;
        panel.add(btnPanel, gbc);

        add(panel);
        setVisible(true);

        // LOGIN
        btnConnect.addActionListener(e -> login());

        // REGISTER
        btnRegister.addActionListener(e -> register());
    }

    private void login() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());
        String ip = txtServerIP.getText().trim();
        
          if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ!");
            return; // ← thiếu cái này
            }   

        try {
            Socket socket = new Socket(ip, 5000);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );

            out.println("__LOGIN__" + username + "|" + password);

            String response = in.readLine();


            if (response == null) {
                JOptionPane.showMessageDialog(this, "Server ngắt kết nối!");
                socket.close();
                return;
            }

            if ("__LOGIN_SUCCESS__".equals(response)) {
                dispose();
                new ChatFrame(username, socket);
            } else {
                JOptionPane.showMessageDialog(this, "Sai tài khoản!");
                socket.close();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Không kết nối được server!");
        }
    }

    private void register() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());
        String ip = txtServerIP.getText().trim();

        try {
            Socket socket = new Socket(ip, 5000);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );

            out.println("__REGISTER__" + username + "|" + password);

            String response = in.readLine();

            if ("__REGISTER_SUCCESS__".equals(response)) {
                JOptionPane.showMessageDialog(this, "Đăng ký thành công!");
            } else {
                JOptionPane.showMessageDialog(this, "User đã tồn tại!");
            }

            socket.close();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối!");
        }
    }
}
package client.chat;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class LoginFrame extends JFrame {

    private JTextField txtUsername;
    private JTextField txtServerIP;
    private JPasswordField txtPassword;
    private JButton btnConnect, btnRegister;

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
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(title, gbc);

        // Username
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        panel.add(new JLabel("Tên của bạn:"), gbc);
        txtUsername = new JTextField("User1");
        gbc.gridx = 1;
        panel.add(txtUsername, gbc);

        // ... Cấu trúc UI cũ, thêm ô Password ...
        gbc.gridy = 2;
        gbc.gridx = 0;
        panel.add(new JLabel("Mật khẩu:"), gbc);
        txtPassword = new JPasswordField();
        gbc.gridx = 1;
        panel.add(txtPassword, gbc);

        // Server IP chuyển xuống gridy = 3
        gbc.gridy = 3;
        gbc.gridx = 0;
        panel.add(new JLabel("IP Server:"), gbc);
        txtServerIP = new JTextField("10.0.8.72");
        gbc.gridx = 1;
        panel.add(txtServerIP, gbc);

        // Nút bấm
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        btnConnect = new JButton("Đăng nhập");
        btnRegister = new JButton("Đăng ký");
        btnPanel.add(btnConnect);
        btnPanel.add(btnRegister);

        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(btnPanel, gbc);

        // Sự kiện
        btnConnect.addActionListener(e -> handleAuth("__LOGIN__"));
        btnRegister.addActionListener(e -> handleAuth("__REGISTER__"));

        add(panel);
        setVisible(true);
    }
    
    private void handleAuth(String type) {
            String user = txtUsername.getText().trim();
            String pass = new String(txtPassword.getPassword());
            String ip = txtServerIP.getText().trim();

            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đủ thông tin!");
                return;
            }

            try {
                // Kết nối tạm thời để xác thực
                Socket socket = new Socket(ip, 5000);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                out.println(type + user + "|" + pass);
                String response = in.readLine();

                if (response.equals("__AUTH_SUCCESS__")) {
                    if (type.equals("__LOGIN__")) {
                        dispose();
                        new ChatFrame(user, ip); // Đăng nhập thành công
                    } else {
                        JOptionPane.showMessageDialog(this, "Đăng ký thành công! Hãy đăng nhập.");
                    }
                } else if (response.startsWith("__AUTH_FAIL__")) {
                    JOptionPane.showMessageDialog(this, response.replace("__AUTH_FAIL__", ""));
                }

                socket.close(); // Đóng kết nối tạm
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Không kết nối được tới Server!");
            }
        }
}

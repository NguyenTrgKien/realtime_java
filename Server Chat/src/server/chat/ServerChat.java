package server.chat;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.nio.file.*;

public class ServerChat extends JFrame {

    private JTextArea txtLog;
    private JTextField txtInput;
    private JButton btnSend;
    private JLabel lblStatus;

    // Lưu tất cả client đang kết nối
    private Map<String, PrintWriter> clients =
        Collections.synchronizedMap(new HashMap<>());

    public ServerChat() {
        setTitle("Server Chat");
        setSize(500, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        buildUI();
        setVisible(true);

        new Thread(this::startServer).start();
    }

    private void buildUI() {
        setLayout(new BorderLayout());

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(40, 167, 69));
        header.setBorder(new EmptyBorder(10, 15, 10, 15));

        JLabel lblTitle = new JLabel("🖥️ Server Chat");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 16));
        lblTitle.setForeground(Color.WHITE);

        lblStatus = new JLabel("⏳ Chưa có client");
        lblStatus.setFont(new Font("Arial", Font.PLAIN, 12));
        lblStatus.setForeground(new Color(200, 255, 200));

        header.add(lblTitle, BorderLayout.WEST);
        header.add(lblStatus, BorderLayout.EAST);

        // Log area
        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtLog.setBackground(new Color(30, 30, 30));
        txtLog.setForeground(new Color(200, 255, 200));
        txtLog.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scroll = new JScrollPane(txtLog);

        // Bottom
        JPanel bottomPanel = new JPanel(new BorderLayout(8, 0));
        bottomPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        txtInput = new JTextField();
        txtInput.setFont(new Font("Arial", Font.PLAIN, 14));

        btnSend = new JButton("Broadcast ➤");
        btnSend.setBackground(new Color(40, 167, 69));
        btnSend.setForeground(Color.WHITE);
        btnSend.setFont(new Font("Arial", Font.BOLD, 13));
        btnSend.setFocusPainted(false);
        btnSend.setCursor(new Cursor(Cursor.HAND_CURSOR));

        bottomPanel.add(txtInput, BorderLayout.CENTER);
        bottomPanel.add(btnSend, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        btnSend.addActionListener(e -> broadcastFromServer());
        txtInput.addActionListener(e -> broadcastFromServer());
    }

    private void broadcastFromServer() {
        String msg = txtInput.getText().trim();
        if (!msg.isEmpty()) {
            broadcast("[Server]: " + msg);
            log("[Server]: " + msg);
            txtInput.setText("");
        }
    }

    private void broadcast(String msg) {
        for (PrintWriter client : clients.values()) {
            client.println(msg);
        }
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append(msg + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }

    private void startServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(5000);
            log("✅ Server khởi động, đang lắng nghe port 5000...");

            while (true) {
                Socket socket = serverSocket.accept();
                log("🔗 Client mới kết nối: " + socket.getInetAddress());

                SwingUtilities.invokeLater(() ->
                    lblStatus.setText("🟢 " + clients.size() + " client"));

                // Mỗi client chạy 1 thread riêng
                new Thread(() -> handleClient(socket)).start();
            }
        } catch (IOException e) {
            log("❌ Lỗi server: " + e.getMessage());
        }
    }
    
    private void sendUserList() {

        String users = String.join(",", clients.keySet());

        for (PrintWriter client : clients.values()) {
            client.println("__USERS__" + users);
        }

    }

    private final String USER_FILE = "users.txt";

private synchronized boolean verifyLogin(String username, String password) {
    try {
        List<String> lines = Files.readAllLines(Paths.get(USER_FILE));
        for (String line : lines) {
            String[] parts = line.split(":");
            if (parts.length == 2 && parts[0].equals(username) && parts[1].equals(password)) {
                return true;
            }
        }
    } catch (IOException e) { log("Lỗi đọc file user"); }
    return false;
}

private synchronized boolean registerUser(String username, String password) {
    try {
        // Kiểm tra user tồn tại chưa
        List<String> lines = Files.readAllLines(Paths.get(USER_FILE));
        for (String line : lines) {
            if (line.startsWith(username + ":")) return false;
        }
        // Ghi thêm vào file
        Files.write(Paths.get(USER_FILE), (username + ":" + password + "\n").getBytes(), StandardOpenOption.APPEND);
        return true;
    } catch (IOException e) {
        // Nếu file chưa tồn tại, tạo mới
        try { Files.write(Paths.get(USER_FILE), (username + ":" + password + "\n").getBytes(), StandardOpenOption.CREATE); return true; } 
        catch (IOException ex) { return false; }
    }
}
    
    private void handleClient(Socket socket) {
    String username = null;
    try {
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        String msg;
        while ((msg = in.readLine()) != null) {
            // XỬ LÝ ĐĂNG KÝ
            if (msg.startsWith("__REGISTER__")) {
                String[] parts = msg.replace("__REGISTER__", "").split("\\|");
                if (registerUser(parts[0], parts[1])) {
                    out.println("__AUTH_SUCCESS__");
                    log("📝 Đăng ký mới: " + parts[0]);
                } else {
                    out.println("__AUTH_FAIL__Tên người dùng đã tồn tại!");
                }
            } 
            // XỬ LÝ ĐĂNG NHẬP
            else if (msg.startsWith("__LOGIN__")) {
                String[] parts = msg.replace("__LOGIN__", "").split("\\|");
                String userReq = parts[0];
                String passReq = parts[1];

                if (clients.containsKey(userReq)) {
                    out.println("__AUTH_FAIL__Tài khoản đang đăng nhập ở nơi khác!");
                } else if (verifyLogin(userReq, passReq)) {
                    username = userReq;
                    clients.put(username, out);
                    out.println("__AUTH_SUCCESS__");
                    log("👤 " + username + " đã đăng nhập!");
                    sendUserList();
                } else {
                    out.println("__AUTH_FAIL__Sai tên đăng nhập hoặc mật khẩu!");
                }
            }
            // XỬ LÝ TIN NHẮN (Giữ nguyên logic cũ của bạn nhưng bọc trong if username != null)
            else if (msg.startsWith("__MSG__") && username != null) {
                // ... logic gửi tin nhắn cũ ...
            }
        }
        } catch (IOException e) {
            log("⚠️ Một client đã ngắt kết nối.");
        } finally {
            if (username != null) {

                clients.remove(username);

                log(username + " đã rời phòng");

                sendUserList();

            }

        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServerChat::new);
    }
}
package server.chat;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.security.MessageDigest;

public class ServerChat extends JFrame {

    private JTextArea txtLog;
    private JTextField txtInput;
    private JButton btnSend;
    private JLabel lblStatus;
    private Map<String, String> userAccounts = new HashMap<>();
    private final String USER_FILE = "users.txt";

    // Lưu tất cả client đang kết nối
    private Map<String, PrintWriter> clients =
        Collections.synchronizedMap(new HashMap<>());

    public ServerChat() {
        loadUsers();
        setTitle("Server Chat");
        setSize(500, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        buildUI();
        setVisible(true);

        new Thread(this::startServer).start();
    }

    private String hashMD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return input; // fallback
        }
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
    
    private void saveUser(String username, String hashedPassword) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(USER_FILE, true))) {
           bw.write(username + "|" + hashedPassword);
            bw.newLine();
            log("Đã lưu user: " + username);
        } catch (IOException e) {
            log("Lỗi lưu file: " + e.getMessage());
        }
    }
    
    private void loadUsers() {
        try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 2) {
                    userAccounts.put(parts[0], parts[1]);
                }
            }
            log("Đã load user từ file");
        } catch (IOException e) {
            log("Không tìm thấy file users.txt, sẽ tạo mới");
        }
    }

    
    private void sendUserList() {

        String users = String.join(",", clients.keySet());

        for (PrintWriter client : clients.values()) {
            client.println("__USERS__" + users);
        }

    }

    private void handleClient(Socket socket) {
        String username = null;

        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));


            String msg;

            while ((msg = in.readLine()) != null) {

                if (msg.startsWith("__JOIN__")) {

                    username = msg.replace("__JOIN__", "");

                    clients.put(username, out);

                    sendUserList(); 

                }
                else if (msg.startsWith("__LOGIN__")) {

                    String data = msg.replace("__LOGIN__", "");
                    String[] parts = data.split("\\|");

                    if (parts.length < 2) continue;

                    String usernameInput = parts[0];
                    String passwordInput = parts[1];

                    String realPassword = userAccounts.get(usernameInput);

                    if (realPassword != null && realPassword.equals(hashMD5(passwordInput))) {

                        out.println("__LOGIN_SUCCESS__");

                        username = usernameInput;
                        clients.put(username, out);

                        log("✅ " + username + " đăng nhập");

                        sendUserList();

                    } else {
                        out.println("__LOGIN_FAIL__");
                        socket.close();
                    }
                }
                else if (msg.startsWith("__MSG__")) {

                    String data = msg.substring(7);

                    String[] parts = data.split("\\|");
                    if(parts.length < 3){
                        continue;
                    }

                    String sender = parts[0];
                    String receiver = parts[1];
                    String message = parts[2];

                    PrintWriter receiverOut = clients.get(receiver);
                    PrintWriter senderOut = clients.get(sender);
                    if (receiverOut != null) {
                         receiverOut.println(sender + ": " + message);

                    }
                    if (senderOut != null) {
                        senderOut.println("Bạn → " + receiver + ": " + message);
                    }

                }else if (msg.startsWith("__REGISTER__")) {
                    String data = msg.replace("__REGISTER__", "");
                    String[] parts = data.split("\\|");

                    if (parts.length < 2) continue;

                    String usernameInput = parts[0];
                    String passwordInput = parts[1];

                    if (userAccounts.containsKey(usernameInput)) {
                        out.println("__REGISTER_FAIL__");
                    } else {
                        userAccounts.put(usernameInput, hashMD5(passwordInput));
                        saveUser(usernameInput, hashMD5(passwordInput));
                        out.println("__REGISTER_SUCCESS__");
                        log("🆕 User mới: " + usernameInput);
                    }
                }

                SwingUtilities.invokeLater(() ->
                    lblStatus.setText("🟢 " + clients.size() + " client"));
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
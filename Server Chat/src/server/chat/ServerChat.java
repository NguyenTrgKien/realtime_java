package server.chat;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.Base64;
import java.security.MessageDigest;

public class ServerChat extends JFrame {

    private JTextArea txtLog;
    private JTextField txtInput;
    private JButton btnSend;
    private JLabel lblStatus;
    private Map<String, String> userAccounts = new HashMap<>();
    private final String USER_FILE   = "users.txt";
    private final String AVATAR_DIR  = "avatars/";

    private Map<String, PrintWriter> clients =
        Collections.synchronizedMap(new HashMap<>());

    public ServerChat() {
        new File(AVATAR_DIR).mkdirs();
        loadUsers();

        setTitle("Server Chat");
        setSize(500, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        buildUI();
        setVisible(true);

        new Thread(this::startServer).start();
    }

    // ══════════════════════════════════════════
    //  UI
    // ══════════════════════════════════════════
    private void buildUI() {
        setLayout(new BorderLayout());

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

        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtLog.setBackground(new Color(30, 30, 30));
        txtLog.setForeground(new Color(200, 255, 200));
        txtLog.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scroll = new JScrollPane(txtLog);

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

        add(header,      BorderLayout.NORTH);
        add(scroll,      BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        btnSend.addActionListener(e -> broadcastFromServer());
        txtInput.addActionListener(e -> broadcastFromServer());
    }

    // ══════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════
    private String hashMD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return input;
        }
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
        for (PrintWriter pw : clients.values()) pw.println(msg);
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append(msg + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }

    // ══════════════════════════════════════════
    //  USER PERSISTENCE
    // ══════════════════════════════════════════
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
                if (parts.length == 2) userAccounts.put(parts[0], parts[1]);
            }
        } catch (IOException e) {
            // file chưa tồn tại — bình thường
        }
    }

    // ══════════════════════════════════════════
    //  USER / AVATAR BROADCAST
    // ══════════════════════════════════════════
    private void sendUserList() {
        String users = String.join(",", clients.keySet());
        for (PrintWriter pw : clients.values()) pw.println("__USERS__" + users);
    }

  
    private void sendAllAvatarsTo(PrintWriter out) {
        File dir = new File(AVATAR_DIR);
        File[] files = dir.listFiles((d, name) ->
            name.endsWith(".png") || name.endsWith(".jpg"));
        if (files == null) return;

        for (File f : files) {
            try {
                byte[] bytes = Files.readAllBytes(f.toPath());
                String b64   = Base64.getEncoder().encodeToString(bytes);
                // Tên file = username.png  hoặc  username.jpg
                String uname = f.getName().replaceAll("\\.(png|jpg)$", "");
                out.println("__AVATAR__" + uname + "|" + b64);
            } catch (IOException ignored) {}
        }
    }

    // ══════════════════════════════════════════
    //  SERVER LOOP
    // ══════════════════════════════════════════
    private void startServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(5000);
            log("✅ Server khởi động, đang lắng nghe port 5000...");

            while (true) {
                Socket socket = serverSocket.accept();
                log("🔗 Client mới kết nối: " + socket.getInetAddress());
                new Thread(() -> handleClient(socket)).start();
            }
        } catch (IOException e) {
            log("❌ Lỗi server: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════
    //  CLIENT HANDLER
    // ══════════════════════════════════════════
    private void handleClient(Socket socket) {
        String username = null;

        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in  = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));

            String msg;
            while ((msg = in.readLine()) != null) {

                // ── ĐĂNG KÝ ──────────────────────────────
                if (msg.startsWith("__REGISTER__")) {
                    String data  = msg.substring(12);
                    String[] parts = data.split("\\|");
                    if (parts.length < 2) continue;

                    String uname = parts[0].trim();
                    String pass  = parts[1];
                    System.out.println(userAccounts.keySet());
                    if (userAccounts.containsKey(uname)) {
                        out.println("__REGISTER_FAIL__");
                    } else {
                        userAccounts.put(uname, hashMD5(pass));
                        saveUser(uname, hashMD5(pass));
                        out.println("__REGISTER_SUCCESS__");
                        log("🆕 User mới: " + uname);
                    }
                }

                // ── ĐĂNG NHẬP ────────────────────────────
                else if (msg.startsWith("__LOGIN__")) {
                    String data  = msg.substring(9);
                    String[] parts = data.split("\\|");
                    if (parts.length < 2) continue;

                    String uname = parts[0];
                    String pass  = parts[1];
                    String real  = userAccounts.get(uname);

                    if (real != null && real.equals(hashMD5(pass))) {
                        out.println("__LOGIN_SUCCESS__");
                        username = uname;
                        clients.put(username, out);
                        log("✅ " + username + " đăng nhập");
                        sendUserList();
                        sendAllAvatarsTo(out);   // ← gửi avatar hiện có
                    } else {
                        out.println("__LOGIN_FAIL__");
                        socket.close();
                        return;
                    }
                }

                // ── JOIN (không qua login) ────────────────
                else if (msg.startsWith("__JOIN__")) {
                    username = msg.substring(8);
                    clients.put(username, out);
                    sendUserList();
                    sendAllAvatarsTo(out);       // ← gửi avatar hiện có
                }

                // ── TIN NHẮN ─────────────────────────────
                else if (msg.startsWith("__MSG__")) {
                    String data  = msg.substring(7);
                    String[] parts = data.split("\\|", 3);
                    if (parts.length < 3) continue;

                    String sender   = parts[0];
                    String receiver = parts[1];
                    String message  = parts[2];

                    // Relay đúng format để client parse
                    String formatted = "__MSG__" + sender + "|" + receiver + "|" + message;

                    PrintWriter receiverOut = clients.get(receiver);
                    PrintWriter senderOut   = clients.get(sender);
                    if (receiverOut != null) receiverOut.println(formatted);
                    if (senderOut   != null) senderOut.println(formatted);

                    log("💬 " + sender + " → " + receiver + ": " + message);
                }

                // ── AVATAR ───────────────────────────────
                else if (msg.startsWith("__AVATAR__")) {
                    String data = msg.substring(10);
                    int sep = data.indexOf('|');
                    if (sep < 0) continue;

                    String uname = data.substring(0, sep);
                    String b64   = data.substring(sep + 1);

                    // Lưu file vào avatars/username.png
                    try {
                        byte[] bytes = Base64.getDecoder().decode(b64);
                        File outFile = new File(AVATAR_DIR + uname + ".png");
                        try (FileOutputStream fos = new FileOutputStream(outFile)) {
                            fos.write(bytes);
                        }
                        log("🖼️ Avatar cập nhật: " + uname);
                    } catch (Exception e) {
                        log("❌ Lỗi lưu avatar: " + e.getMessage());
                        continue;
                    }

                    // Broadcast cho tất cả client đang online
                    String broadcastMsg = "__AVATAR__" + uname + "|" + b64;
                    for (PrintWriter pw : clients.values()) pw.println(broadcastMsg);
                }

            }

        } catch (IOException e) {
            log("⚠️ Client ngắt kết nối.");
        } finally {
            if (username != null) {
                clients.remove(username);
                log(username + " đã rời phòng");
                sendUserList();
            }
            SwingUtilities.invokeLater(() ->
                lblStatus.setText("🟢 " + clients.size() + " client"));
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServerChat::new);
    }
}
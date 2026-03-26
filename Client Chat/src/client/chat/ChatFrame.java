package client.chat;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Base64;

public class ChatFrame extends JFrame {

    private static final Color ZALO_BLUE       = new Color(0, 104, 255);
    private static final Color ZALO_BLUE_LIGHT = new Color(232, 244, 251);
    private static final Color BG_SIDEBAR      = new Color(255, 255, 255);
    private static final Color BG_CHAT         = new Color(243, 245, 247);
    private static final Color BG_HEADER       = new Color(255, 255, 255);
    private static final Color BG_INPUT        = new Color(255, 255, 255);
    private static final Color BUBBLE_ME       = new Color(0, 104, 255);
    private static final Color BUBBLE_OTHER    = new Color(255, 255, 255);
    private static final Color TEXT_PRIMARY    = new Color(30, 30, 30);
    private static final Color TEXT_SECONDARY  = new Color(130, 130, 130);
    private static final Color DIVIDER         = new Color(230, 230, 230);
    private static final Color HOVER_BG        = new Color(240, 246, 255);
    private static final Color ACTIVE_BG       = new Color(225, 238, 255);

    private static final Color[] AVATAR_COLORS = {
        new Color(0, 104, 255), new Color(45, 183, 245),
        new Color(82, 196, 26), new Color(250, 140, 22),
        new Color(235, 47, 150)
    };

    private static final String[] EMOJIS = {
        "😀","😂","😍","😢","😡","😎","🥰","😭",
        "👍","👎","👏","🙏","🤝","✌️","🤞","💪",
        "❤️","💔","💯","🔥","⭐","🎉","🎊","🎁",
        "😅","🤣","😇","🤔","😴","🤯","🥳","😱"
    };

    // ── State ────────────────────────────────
    private String username;
    private PrintWriter out;
    private String selectedUser = null;

    // ── UI components ────────────────────────
    private JPanel friendListPanel;
    private JPanel chatContentPanel;
    private JPanel messageArea;
    private JScrollPane messageScroll;
    private JTextField txtInput;
    private JLabel lblChatName;
    private JLabel lblChatAvatar;   // header avatar (JLabel hiển thị ImageIcon)
    private JLabel lblOnlineStatus;
    private AvatarLabel selfAvatarLabel;

    // ── Data ─────────────────────────────────
    private Map<String, Color>     userColors  = new HashMap<>();
    private Map<String, ImageIcon> avatarCache = new HashMap<>(); // 42px sidebar
    // Lưu tham chiếu AvatarLabel của từng friend để cập nhật khi có avatar mới
    private Map<String, AvatarLabel> friendAvatarLabels = new HashMap<>();

    // ═════════════════════════════════════════
    public ChatFrame(String username, Socket socket) {
        this.username = username;
        setTitle("Zalo - " + username);
        setSize(820, 580);
        setMinimumSize(new Dimension(600, 400));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        buildUI();
        setVisible(true);

        new Thread(() -> connectToServer(socket)).start();
    }
    
    private void saveMessageToFile(String sender, String receiver, String msg) {
        try {
            FileWriter fw = new FileWriter("chat.txt", true); // true = append
            BufferedWriter bw = new BufferedWriter(fw);

            // format: sender|receiver|message|time
            String line = sender + "|" + receiver + "|" + msg + "|" + new Date();
            bw.write(line);
            bw.newLine();

            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════
    //  BUILD UI
    // ══════════════════════════════════════════
    private void buildUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_CHAT);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            buildSidebar(), buildChatArea());
        splitPane.setDividerLocation(260);
        splitPane.setDividerSize(1);
        splitPane.setBorder(null);
        splitPane.setBackground(DIVIDER);

        add(splitPane, BorderLayout.CENTER);
    }

    // ── SIDEBAR ──────────────────────────────
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(BG_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(260, 0));

        // Header
        JPanel sidebarHeader = new JPanel(new BorderLayout());
        sidebarHeader.setBackground(BG_SIDEBAR);
        sidebarHeader.setBorder(new MatteBorder(0, 0, 1, 0, DIVIDER));
        sidebarHeader.setPreferredSize(new Dimension(0, 56));
        JLabel lblTitle = new JLabel("  Tin nhắn");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(TEXT_PRIMARY);
        sidebarHeader.add(lblTitle, BorderLayout.WEST);

        // Search
        JPanel searchPanel = new JPanel(new BorderLayout(6, 0));
        searchPanel.setBackground(BG_SIDEBAR);
        searchPanel.setBorder(new EmptyBorder(8, 10, 8, 10));
        RoundPanel searchBox = new RoundPanel(20);
        searchBox.setBackground(new Color(242, 243, 245));
        searchBox.setLayout(new BorderLayout(6, 0));
        searchBox.setBorder(new EmptyBorder(6, 10, 6, 10));
        JLabel searchIcon = new JLabel("🔍");
        searchIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        JTextField searchField = new JTextField();
        searchField.setBorder(null);
        searchField.setBackground(new Color(242, 243, 245));
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setForeground(TEXT_SECONDARY);
        searchBox.add(searchIcon, BorderLayout.WEST);
        searchBox.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchBox, BorderLayout.CENTER);

        JPanel sidebarTop = new JPanel(new BorderLayout());
        sidebarTop.setBackground(BG_SIDEBAR);
        sidebarTop.add(sidebarHeader, BorderLayout.NORTH);
        sidebarTop.add(searchPanel, BorderLayout.CENTER);

        // Friend list
        friendListPanel = new JPanel();
        friendListPanel.setLayout(new BoxLayout(friendListPanel, BoxLayout.Y_AXIS));
        friendListPanel.setBackground(BG_SIDEBAR);
        JScrollPane scrollFriends = new JScrollPane(friendListPanel);
        scrollFriends.setBorder(null);
        scrollFriends.getVerticalScrollBar().setUnitIncrement(12);
        scrollFriends.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        sidebar.add(sidebarTop,    BorderLayout.NORTH);
        sidebar.add(scrollFriends, BorderLayout.CENTER);

        // Self info + avatar đổi được
        JPanel selfPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        selfPanel.setBackground(BG_SIDEBAR);
        selfPanel.setBorder(new MatteBorder(1, 0, 0, 0, DIVIDER));

        // Avatar tự chụp — click để đổi
        selfAvatarLabel = new AvatarLabel(getInitials(username), ZALO_BLUE, 34);
        selfAvatarLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        selfAvatarLabel.setToolTipText("Click để đổi avatar");
        selfAvatarLabel.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { pickAndUploadAvatar(); }
        });

        JPanel selfInfo = new JPanel();
        selfInfo.setLayout(new BoxLayout(selfInfo, BoxLayout.Y_AXIS));
        selfInfo.setBackground(BG_SIDEBAR);

        JLabel selfName = new JLabel(username);
        selfName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        selfName.setForeground(TEXT_PRIMARY);

        JLabel selfHint = new JLabel("Click avatar để thay đổi");
        selfHint.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        selfHint.setForeground(TEXT_SECONDARY);

        selfInfo.add(selfName);
        selfInfo.add(selfHint);

        selfPanel.add(selfAvatarLabel);
        selfPanel.add(selfInfo);
        sidebar.add(selfPanel, BorderLayout.SOUTH);

        return sidebar;
    }

    // ── CHAT AREA ────────────────────────────
    private JPanel buildChatArea() {
        JPanel wrapper = new JPanel(new CardLayout());
        wrapper.setBackground(BG_CHAT);

        // Empty state
        JPanel emptyStatePanel = new JPanel(new GridBagLayout());
        emptyStatePanel.setBackground(BG_CHAT);
        JPanel emptyBox = new JPanel();
        emptyBox.setLayout(new BoxLayout(emptyBox, BoxLayout.Y_AXIS));
        emptyBox.setBackground(BG_CHAT);
        JLabel emptyIcon = new JLabel("💬", SwingConstants.CENTER);
        emptyIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        emptyIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel emptyText = new JLabel("Chọn một người để bắt đầu trò chuyện");
        emptyText.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        emptyText.setForeground(TEXT_SECONDARY);
        emptyText.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyBox.add(emptyIcon);
        emptyBox.add(Box.createVerticalStrut(12));
        emptyBox.add(emptyText);
        emptyStatePanel.add(emptyBox);

        // Chat content
        chatContentPanel = new JPanel(new BorderLayout());
        chatContentPanel.setBackground(BG_CHAT);

        // Header
        JPanel chatHeader = new JPanel(new BorderLayout(10, 0));
        chatHeader.setBackground(BG_HEADER);
        chatHeader.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, DIVIDER),
            new EmptyBorder(10, 16, 10, 16)
        ));
        chatHeader.setPreferredSize(new Dimension(0, 58));

        lblChatAvatar = new JLabel();
        lblChatAvatar.setPreferredSize(new Dimension(38, 38));

        JPanel nameBox = new JPanel();
        nameBox.setLayout(new BoxLayout(nameBox, BoxLayout.Y_AXIS));
        nameBox.setBackground(BG_HEADER);

        lblChatName = new JLabel("...");
        lblChatName.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblChatName.setForeground(TEXT_PRIMARY);

        lblOnlineStatus = new JLabel("Đang hoạt động");
        lblOnlineStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblOnlineStatus.setForeground(ZALO_BLUE);

        nameBox.add(lblChatName);
        nameBox.add(lblOnlineStatus);
        chatHeader.add(lblChatAvatar, BorderLayout.WEST);
        chatHeader.add(nameBox, BorderLayout.CENTER);

        // Message area
        messageArea = new JPanel();
        messageArea.setLayout(new BoxLayout(messageArea, BoxLayout.Y_AXIS));
        messageArea.setBackground(BG_CHAT);
        messageArea.setBorder(new EmptyBorder(12, 16, 12, 16));
        messageScroll = new JScrollPane(messageArea);
        messageScroll.setBorder(null);
        messageScroll.getVerticalScrollBar().setUnitIncrement(14);

        // Input bar
        JPanel inputBar = new JPanel(new BorderLayout(8, 0));
        inputBar.setBackground(BG_INPUT);
        inputBar.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, DIVIDER),
            new EmptyBorder(10, 12, 10, 12)
        ));
        txtInput = new JTextField();
        txtInput.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtInput.setBorder(new CompoundBorder(
            new MatteBorder(1, 1, 1, 1, DIVIDER),
            new EmptyBorder(7, 12, 7, 12)
        ));
        txtInput.setBackground(new Color(245, 246, 248));

        JButton btnEmoji = new JButton("😊");
        btnEmoji.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        btnEmoji.setBorderPainted(false);
        btnEmoji.setContentAreaFilled(false);
        btnEmoji.setFocusPainted(false);
        btnEmoji.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEmoji.setPreferredSize(new Dimension(38, 38));
        btnEmoji.addActionListener(e -> showEmojiPicker(btnEmoji));

        JButton btnSend = new RoundButton("Gửi", ZALO_BLUE, Color.WHITE);
        btnSend.setPreferredSize(new Dimension(72, 36));
        btnSend.addActionListener(e -> sendMessage());
        txtInput.addActionListener(e -> sendMessage());

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightButtons.setBackground(BG_INPUT);
        rightButtons.add(btnEmoji);
        rightButtons.add(btnSend);

        inputBar.add(txtInput, BorderLayout.CENTER);
        inputBar.add(rightButtons, BorderLayout.EAST);

        chatContentPanel.add(chatHeader,   BorderLayout.NORTH);
        chatContentPanel.add(messageScroll,BorderLayout.CENTER);
        chatContentPanel.add(inputBar,     BorderLayout.SOUTH);

        wrapper.add(emptyStatePanel, "empty");
        wrapper.add(chatContentPanel, "chat");
        ((CardLayout) wrapper.getLayout()).show(wrapper, "empty");

        chatContentPanel.putClientProperty("wrapper", wrapper);
        return wrapper;
    }

    // ══════════════════════════════════════════
    //  FRIEND LIST ITEM
    // ══════════════════════════════════════════
    private void addFriendItem(String name) {
        Color avatarColor = userColors.computeIfAbsent(name,
            k -> AVATAR_COLORS[Math.abs(k.hashCode()) % AVATAR_COLORS.length]);

        JPanel item = new JPanel(new BorderLayout(10, 0));
        item.setBackground(BG_SIDEBAR);
        item.setBorder(new EmptyBorder(10, 12, 10, 12));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        AvatarLabel avatar = new AvatarLabel(getInitials(name), avatarColor, 42);
        friendAvatarLabels.put(name, avatar); // đăng ký để cập nhật sau

        // Nếu đã có avatar trong cache → hiển thị ngay
        if (avatarCache.containsKey(name)) {
            avatar.setImageIcon(avatarCache.get(name));
        }

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(BG_SIDEBAR);

        JLabel lblName = new JLabel(name);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblName.setForeground(TEXT_PRIMARY);

        JLabel lblLast = new JLabel("Bắt đầu trò chuyện");
        lblLast.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblLast.setForeground(TEXT_SECONDARY);

        info.add(lblName);
        info.add(Box.createVerticalStrut(2));
        info.add(lblLast);

        item.add(avatar, BorderLayout.WEST);
        item.add(info,   BorderLayout.CENTER);

        item.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (!name.equals(selectedUser)) setItemBg(item, info, HOVER_BG);
            }
            @Override public void mouseExited(MouseEvent e) {
                if (!name.equals(selectedUser)) setItemBg(item, info, BG_SIDEBAR);
            }
            @Override public void mouseClicked(MouseEvent e) {
                openChat(name, avatarColor, item, info);
            }
        });

        SwingUtilities.invokeLater(() -> {
            friendListPanel.add(item);
            friendListPanel.revalidate();
        });
    }

    private void loadMessages(String user1, String user2) {
        File file = new File("chat.txt");
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader("chat.txt"))) {
            String line;

            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|", 4);
                if (parts.length >= 4) {
                    String sender = parts[0];
                    String receiver = parts[1];
                    String msg = parts[2];  

                    boolean match =
                        (sender.equals(user1) && receiver.equals(user2)) ||
                        (sender.equals(user2) && receiver.equals(user1));

                    if (match) {
                        appendBubble(msg, sender.equals(username));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void setItemBg(JPanel item, JPanel info, Color bg) {
        item.setBackground(bg);
        info.setBackground(bg);
    }

    private void openChat(String name, Color avatarColor,
                          JPanel clickedItem, JPanel clickedInfo) {
        // Reset tất cả item
        for (Component c : friendListPanel.getComponents()) {
            if (c instanceof JPanel p) {
                p.setBackground(BG_SIDEBAR);
                for (Component child : p.getComponents())
                    if (child instanceof JPanel inner) inner.setBackground(BG_SIDEBAR);
            }
        }
        setItemBg(clickedItem, clickedInfo, ACTIVE_BG);
        selectedUser = name;

        // Cập nhật header
        lblChatName.setText(name);
        if (avatarCache.containsKey(name)) {
            lblChatAvatar.setIcon(scaleToIcon(avatarCache.get(name), 38));
        } else {
            lblChatAvatar.setIcon(createAvatarIcon(getInitials(name), avatarColor, 38));
        }

        // Hiển thị panel chat
        JPanel wrapper = (JPanel) chatContentPanel.getParent();
        ((CardLayout) wrapper.getLayout()).show(wrapper, "chat");

        messageArea.removeAll();
        loadMessages(username, name);
        messageArea.revalidate();
        messageArea.repaint();
        txtInput.requestFocus();  
    }

    // ══════════════════════════════════════════
    //  MESSAGES
    // ══════════════════════════════════════════
    private void sendMessage() {
        String msg = txtInput.getText().trim();
        if (msg.isEmpty() || out == null || selectedUser == null) return;

        out.println("__MSG__" + username + "|" + selectedUser + "|" + msg);

        txtInput.setText("");
    }

    private void appendBubble(String text, boolean isMe) {
        SwingUtilities.invokeLater(() -> {
            JPanel row = new JPanel(new FlowLayout(
                isMe ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 2));
            row.setBackground(BG_CHAT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            row.add(new BubbleLabel(text, isMe));
            messageArea.add(row);
            messageArea.add(Box.createVerticalStrut(4));
            messageArea.revalidate();
            messageArea.repaint();
            SwingUtilities.invokeLater(() -> {
                JScrollBar sb = messageScroll.getVerticalScrollBar();
                sb.setValue(sb.getMaximum());
            });
        });
    }

    // ══════════════════════════════════════════
    //  EMOJI PICKER
    // ══════════════════════════════════════════
    private void showEmojiPicker(JButton anchor) {
        JDialog picker = new JDialog(this, false);
        picker.setUndecorated(true);
        picker.setLayout(new GridLayout(4, 8, 3, 3));
        picker.getRootPane().setBorder(BorderFactory.createLineBorder(DIVIDER, 1));

        for (String emoji : EMOJIS) {
            JButton btn = new JButton(emoji);
            btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setBackground(Color.WHITE);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { btn.setBackground(ZALO_BLUE_LIGHT); }
                @Override public void mouseExited(MouseEvent e)  { btn.setBackground(Color.WHITE); }
            });
            btn.addActionListener(e -> {
                int pos = txtInput.getCaretPosition();
                String t = txtInput.getText();
                txtInput.setText(t.substring(0, pos) + emoji + t.substring(pos));
                txtInput.setCaretPosition(pos + emoji.length());
                picker.dispose();
                txtInput.requestFocus();
            });
            picker.add(btn);
        }

        picker.pack();
        Point loc = anchor.getLocationOnScreen();
        picker.setLocation(loc.x, loc.y - picker.getHeight() - 4);
        picker.setVisible(true);
        picker.addWindowFocusListener(new WindowFocusListener() {
            @Override public void windowLostFocus(WindowEvent e) { picker.dispose(); }
            @Override public void windowGainedFocus(WindowEvent e) {}
        });
    }

    // ══════════════════════════════════════════
    //  AVATAR — chọn & upload
    // ══════════════════════════════════════════
    private void pickAndUploadAvatar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn ảnh avatar");
        chooser.setFileFilter(new FileNameExtensionFilter(
            "Ảnh (JPG, PNG)", "jpg", "jpeg", "png"));

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();

        new Thread(() -> {
            try {
                // Đọc ảnh gốc
                java.awt.image.BufferedImage orig =
                    javax.imageio.ImageIO.read(file);
                if (orig == null) {
                    showError("Không đọc được file ảnh.");
                    return;
                }

                // Crop hình vuông từ tâm rồi scale xuống 128×128
                java.awt.image.BufferedImage scaled =
                    new java.awt.image.BufferedImage(128, 128,
                        java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = scaled.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                int sz = Math.min(orig.getWidth(), orig.getHeight());
                int ox = (orig.getWidth()  - sz) / 2;
                int oy = (orig.getHeight() - sz) / 2;
                g2.drawImage(orig, 0, 0, 128, 128, ox, oy, ox + sz, oy + sz, null);
                g2.dispose();

                // Encode Base64
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                javax.imageio.ImageIO.write(scaled, "png", baos);
                String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());

                // Gửi lên server
                if (out != null) out.println("__AVATAR__" + username + "|" + b64);

                // Cập nhật local ngay (không cần chờ server echo)
                ImageIcon icon42 = toCircleIcon(scaled, 42);
                ImageIcon icon34 = toCircleIcon(scaled, 34);
                avatarCache.put(username, icon42);
                SwingUtilities.invokeLater(() ->
                    selfAvatarLabel.setImageIcon(icon34));

            } catch (Exception ex) {
                showError("Lỗi xử lý ảnh: " + ex.getMessage());
            }
        }).start();
    }

    private void showError(String msg) {
        SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE));
    }

    // ══════════════════════════════════════════
    //  NETWORK
    // ══════════════════════════════════════════
    private void connectToServer(Socket socket) {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println("__JOIN__" + username);
            
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));

            String msg;
            while ((msg = in.readLine()) != null) {
                final String line = msg;

                // ── Danh sách user ────────────────────────
                if (line.startsWith("__USERS__")) {
                    String users = line.substring(9);
                    String[] arr = users.isEmpty()
                        ? new String[0] : users.split(",");

                    SwingUtilities.invokeLater(() -> {
                        friendListPanel.removeAll();
                        friendAvatarLabels.clear();
                        for (String u : arr) {
                            if (!u.equals(username)) addFriendItem(u);
                        }
                        friendListPanel.revalidate();
                        friendListPanel.repaint();
                    });
                }

                // ── Tin nhắn ─────────────────────────────
                else if (line.startsWith("__MSG__")) {
                    String content = line.substring(7);
                    String[] parts = content.split("\\|", 3);

                    if (parts.length == 3) {
                        String sender   = parts[0];
                        String receiver = parts[1];
                        String message  = parts[2];

                        // 🔥 lưu vào file
                        saveMessageToFile(sender, receiver, message);

                        boolean relevant =
                            (sender.equals(selectedUser) && receiver.equals(username)) ||
                            (sender.equals(username) && receiver.equals(selectedUser));

                        if (relevant) {
                            appendBubble(message, sender.equals(username));
                        }
                    }   
                }

                // ── Avatar ───────────────────────────────
                else if (line.startsWith("__AVATAR__")) {
                    String data = line.substring(10);
                    int sep = data.indexOf('|');
                    if (sep < 0) continue;

                    String uname = data.substring(0, sep);
                    String b64   = data.substring(sep + 1);

                    try {
                        byte[] bytes = Base64.getDecoder().decode(b64);
                        java.awt.image.BufferedImage img =
                            javax.imageio.ImageIO.read(
                                new ByteArrayInputStream(bytes));
                        if (img == null) continue;

                        // Tạo icon ở các kích thước cần dùng
                        ImageIcon icon42 = toCircleIcon(img, 42);
                        ImageIcon icon38 = toCircleIcon(img, 38);
                        ImageIcon icon34 = toCircleIcon(img, 34);

                        avatarCache.put(uname, icon42);

                        SwingUtilities.invokeLater(() -> {
                            // Cập nhật avatar trong sidebar
                            AvatarLabel al = friendAvatarLabels.get(uname);
                            if (al != null) al.setImageIcon(icon42);

                            // Cập nhật header nếu đang chat với người này
                            if (uname.equals(selectedUser)) {
                                lblChatAvatar.setIcon(icon38);
                            }

                            // Cập nhật avatar bản thân
                            if (uname.equals(username)) {
                                selfAvatarLabel.setImageIcon(icon34);
                            }
                        });
                    } catch (Exception ignored) {}
                }
            }

        } catch (IOException e) {
            SwingUtilities.invokeLater(() ->
                appendBubble("❌ Mất kết nối tới server", false));
        }
    }

    // ══════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════
    private String getInitials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2)
            return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    /** Tạo icon tròn từ BufferedImage */
    private ImageIcon toCircleIcon(java.awt.image.BufferedImage src, int size) {
        java.awt.image.BufferedImage out2 =
            new java.awt.image.BufferedImage(size, size,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out2.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setClip(new Ellipse2D.Float(0, 0, size, size));
        g2.drawImage(src, 0, 0, size, size, null);
        g2.dispose();
        return new ImageIcon(out2);
    }

    /** Scale ImageIcon đã có sang kích thước khác */
    private ImageIcon scaleToIcon(ImageIcon src, int size) {
        Image scaled = src.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
        java.awt.image.BufferedImage buf =
            new java.awt.image.BufferedImage(size, size,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = buf.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setClip(new Ellipse2D.Float(0, 0, size, size));
        g2.drawImage(scaled, 0, 0, null);
        g2.dispose();
        return new ImageIcon(buf);
    }

    private ImageIcon createAvatarIcon(String initials, Color bg, int size) {
        java.awt.image.BufferedImage img =
            new java.awt.image.BufferedImage(size, size,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(bg);
        g2.fillOval(0, 0, size, size);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, size / 3));
        FontMetrics fm = g2.getFontMetrics();
        int x = (size - fm.stringWidth(initials)) / 2;
        int y = (size - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(initials, x, y);
        g2.dispose();
        return new ImageIcon(img);
    }

    // ══════════════════════════════════════════
    //  INNER CLASSES
    // ══════════════════════════════════════════

    /** Avatar tròn — hỗ trợ cả chữ cái và ảnh thật */
    static class AvatarLabel extends JLabel {
        private final Color bg;
        private final int size;
        private final String initials;
        private ImageIcon photoIcon;

        AvatarLabel(String initials, Color bg, int size) {
            this.initials = initials;
            this.bg       = bg;
            this.size     = size;
            setPreferredSize(new Dimension(size, size));
            setMinimumSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
        }

        public void setImageIcon(ImageIcon icon) {
            this.photoIcon = icon;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

            if (photoIcon != null) {
                // Vẽ ảnh thật dạng tròn
                g2.setClip(new Ellipse2D.Float(0, 0, size, size));
                g2.drawImage(photoIcon.getImage(), 0, 0, size, size, null);
            } else {
                // Fallback: chữ viết tắt trên nền màu
                g2.setColor(bg);
                g2.fillOval(0, 0, size, size);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, size / 3));
                FontMetrics fm = g2.getFontMetrics();
                int x = (size - fm.stringWidth(initials)) / 2;
                int y = (size - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(initials, x, y);
            }
            g2.dispose();
        }
    }

    /** Bong bóng tin nhắn */
    static class BubbleLabel extends JPanel {
        private final String text;
        private final boolean isMe;
        private static final int ARC   = 18;
        private static final int PAD_H = 14;
        private static final int PAD_V = 8;
        private static final int MAX_W = 320;
        private static final Font BUBBLE_FONT =
            new Font("Segoe UI Emoji", Font.PLAIN, 14);

        BubbleLabel(String text, boolean isMe) {
            this.text = text;
            this.isMe = isMe;
            setOpaque(false);
            // KHÔNG set PreferredSize cứng ở đây
            // — Swing sẽ gọi getPreferredSize() khi cần
        }

        /**
         * Tách text thành danh sách dòng đã word-wrap.
         * Dùng chung cho getPreferredSize() và paintComponent()
         * → kích thước và nội dung vẽ LUÔN khớp nhau.
         */
        private java.util.List<String> buildLines(FontMetrics fm) {
            java.util.List<String> lines = new java.util.ArrayList<>();
            int maxTextW = MAX_W - PAD_H * 2;

            for (String paragraph : text.split("\n", -1)) {
                if (paragraph.isEmpty()) { lines.add(""); continue; }
                String[] words = paragraph.split(" ", -1);
                StringBuilder cur = new StringBuilder();
                for (String w : words) {
                    String candidate = cur.isEmpty() ? w : cur + " " + w;
                    if (fm.stringWidth(candidate) > maxTextW && cur.length() > 0) {
                        lines.add(cur.toString());
                        cur = new StringBuilder(w);
                    } else {
                        cur = new StringBuilder(candidate);
                    }
                }
                if (cur.length() > 0) lines.add(cur.toString());
            }
            if (lines.isEmpty()) lines.add("");
            return lines;
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(BUBBLE_FONT);
            if (fm == null)
                fm = Toolkit.getDefaultToolkit().getFontMetrics(BUBBLE_FONT);

            java.util.List<String> lines = buildLines(fm);

            int maxLineW = 0;
            for (String l : lines) maxLineW = Math.max(maxLineW, fm.stringWidth(l));

            int w = Math.min(maxLineW + PAD_H * 2, MAX_W);
            int h = lines.size() * fm.getHeight()
                  + Math.max(0, lines.size() - 1) * 3
                  + PAD_V * 2;
            return new Dimension(w, h);
        }

        @Override public Dimension getMinimumSize() { return getPreferredSize(); }
        @Override public Dimension getMaximumSize() { return getPreferredSize(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();

            // Nền bubble
            if (!isMe) {
                g2.setColor(new Color(220, 220, 220));
                g2.setStroke(new BasicStroke(0.8f));
                g2.draw(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, ARC, ARC));
            }
            g2.setColor(isMe ? BUBBLE_ME : BUBBLE_OTHER);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, ARC, ARC));

            // Text — dùng cùng buildLines() để đảm bảo khớp với kích thước
            g2.setFont(BUBBLE_FONT);
            g2.setColor(isMe ? Color.WHITE : TEXT_PRIMARY);
            FontMetrics fm = g2.getFontMetrics();

            java.util.List<String> lines = buildLines(fm);
            int y = PAD_V + fm.getAscent();
            for (String line : lines) {
                g2.drawString(line, PAD_H, y);
                y += fm.getHeight() + 3;
            }
            g2.dispose();
        }
    }

    static class RoundPanel extends JPanel {
        private final int radius;
        RoundPanel(int radius) { this.radius = radius; setOpaque(false); }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
        }
    }

    static class RoundButton extends JButton {
        private final Color bg, fg;
        RoundButton(String text, Color bg, Color fg) {
            super(text);
            this.bg = bg; this.fg = fg;
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getModel().isPressed() ? bg.darker() :
                        getModel().isRollover() ? bg.brighter() : bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            g2.setColor(fg);
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth()  - fm.stringWidth(getText())) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
    }
}
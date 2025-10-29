package chatapp_client;

import java.awt.*;
import java.awt.event.*;
import java.util.logging.*;
import javax.crypto.SecretKey;
import javax.swing.*;
import model.*;
import org.java_websocket.client.WebSocketClient;
import service.*;

/**
 * Main JFrame. Quản lý các panel và kết nối WebSocket chung.
 * ✅ ĐÃ CẬP NHẬT: Xử lý E2EE Lớp 2 (Chuyển tiếp tin nhắn, không giải mã).
 */
public class MainForm extends javax.swing.JFrame {

    // --- (Constants, components, variables) ---
    private static final Logger logger = Logger.getLogger(MainForm.class.getName());
    public static final String LOGIN_PANEL = "LOGIN";
    public static final String REGISTER_PANEL = "REGISTER";
    public static final String MAIN_APP_PANEL = "MAIN_APP";
    private CardLayout cardLayout;
    private JPanel pnMainContainer;
    private MainChatPanel mainChatPanel;
    private LoginForm loginPanel;
    private RegisterForm registerPanel;
    private int loggedInUserId;
    private User loggedInUser;
    private WebSocketClient sharedWebSocketClient;
    private NetworkService.WSMessageListener wsListener;

    public MainForm() {
        /* (Giữ nguyên UI constructor) */
        setTitle("Chat App"); setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE); setMinimumSize(new Dimension(800, 600)); setLocationRelativeTo(null); cardLayout = new CardLayout(); pnMainContainer = new JPanel(cardLayout); loginPanel = new LoginForm(this); registerPanel = new RegisterForm(this); pnMainContainer.add(loginPanel, LOGIN_PANEL); pnMainContainer.add(registerPanel, REGISTER_PANEL); this.setContentPane(pnMainContainer); cardLayout.show(pnMainContainer, LOGIN_PANEL); pack(); setExtendedState(javax.swing.JFrame.MAXIMIZED_BOTH); createWebSocketListener();
    }

    public void showCard(String cardName) { /* (Giữ nguyên) */ cardLayout.show(pnMainContainer, cardName); if (cardName.equals(LOGIN_PANEL) && loginPanel != null) { loginPanel.clearFields(); } else if (cardName.equals(REGISTER_PANEL) && registerPanel != null) { registerPanel.clearFields(); } }
    public void showMainApp(User user) { /* (Giữ nguyên) */ this.loggedInUser = user; this.loggedInUserId = user.getId(); connectSharedWebSocket(); mainChatPanel = new MainChatPanel(this); pnMainContainer.add(mainChatPanel, MAIN_APP_PANEL); cardLayout.show(pnMainContainer, MAIN_APP_PANEL); }
    public void showPrivateChatForm(int friendId, String username) { /* (Giữ nguyên) */ if (mainChatPanel != null) mainChatPanel.showPrivateChat(friendId, username); else showCard(LOGIN_PANEL); }
    public void showGroupChatForm(int groupId, String groupName) { /* (Giữ nguyên) */ if (mainChatPanel != null) mainChatPanel.showGroupChat(groupId, groupName); else showCard(LOGIN_PANEL); }
    public User getLoggedInUser() { return loggedInUser; }
    public int getLoggedInUserId() { return loggedInUserId; }
    public void setLoggedInUserId(int userId) { this.loggedInUserId = userId; }
    public WebSocketClient getSharedWebSocketClient() { return sharedWebSocketClient; }
    private void connectSharedWebSocket() { /* (Giữ nguyên) */ if (sharedWebSocketClient != null && sharedWebSocketClient.isOpen()) return; if (loggedInUserId > 0 && wsListener != null) { NetworkService.connectWS(loggedInUserId, wsListener); } else { System.err.println("Không thể kết nối WS: Chưa đăng nhập hoặc listener null."); } }

    /**
     * ✅ ĐÃ CẬP NHẬT: Tạo listener WebSocket chung.
     * Chỉ chuyển tiếp tin nhắn Lớp 2 (không giải mã).
     */
    private void createWebSocketListener() {
        wsListener = new NetworkService.WSMessageListener() {
            @Override
            public void onStompConnected() {
                // Logic subscribe nhóm (VẪN CÒN LỖI 404 DO THIẾU API BACKEND)
                System.out.println("MainForm Listener: STOMP Connected. Subscribing groups...");
                NetworkService.getGroupsForUser(loggedInUserId).thenAccept(groups -> {
                    if (sharedWebSocketClient != null && sharedWebSocketClient.isOpen() && groups != null) {
                        System.out.println("Subscribing to " + groups.size() + " group topics...");
                        for (Group group : groups) {
                            String subFrame = "SUBSCRIBE\nid:sub-group-" + group.getId() + "\ndestination:/topic/group/" + group.getId() + "\nack:auto\n\n\0";
                            sharedWebSocketClient.send(subFrame);
                        }
                    }
                }).exceptionally(ex -> { System.err.println("Error getting groups: " + ex.getMessage()); return null; });
            }

            @Override
            public void onMessageReceived(String headers, String body) {
                String destination = "";
                int destIndex = headers.indexOf("destination:");
                if (destIndex != -1) {
                    int lineEnd = headers.indexOf('\n', destIndex);
                    if (lineEnd == -1) lineEnd = headers.length();
                    destination = headers.substring(destIndex + "destination:".length(), lineEnd).trim();
                }

                // --- Xử lý Chat 1:1 ---
                if (destination.startsWith("/queue/messages/" + loggedInUserId)) {
                    // 1. Parse tin nhắn đến (dùng ChatMessage model Lớp 2)
                    ChatMessage msg = NetworkService.parseStompMessage(body);
                    
                    if (msg == null) {
                         System.err.println("MainForm: Không thể parse tin nhắn STOMP: " + body);
                         return;
                    }
                    if (msg.getSenderId() == loggedInUserId) {
                         System.out.println("MainForm: Bỏ qua tin nhắn của chính mình (đồng bộ thiết bị)");
                         return;
                    }

                    // 2. CHUYỂN TIẾP (KHÔNG GIẢI MÃ)
                    // ChatForm sẽ tự giải mã bằng private key
                    if (mainChatPanel != null) {
                        ChatForm chatForm = mainChatPanel.getOpenPrivateChatForm(msg.getSenderId());
                        if (chatForm != null) {
                            SwingUtilities.invokeLater(() -> chatForm.handleIncomingMessage(msg));
                        } else { 
                            Toolkit.getDefaultToolkit().beep(); // Báo có tin nhắn mới
                        }
                    }
                }
                // --- Xử lý Chat Nhóm ---
                else if (destination.startsWith("/topic/group/")) {
                    // (Tạm thời logic nhóm vẫn dùng Lớp 1 - AES Chung)
                    GroupMessage msg = NetworkService.parseGroupStompMessage(body);
                    if (msg != null && msg.getSenderId() != loggedInUserId) {
                        if (mainChatPanel != null) {
                            GroupChatForm groupChatForm = mainChatPanel.getOpenGroupChatForm(msg.getGroupId());
                            if (groupChatForm != null) {
                                SwingUtilities.invokeLater(() -> groupChatForm.handleIncomingGroupMessage(msg));
                            } else { 
                                Toolkit.getDefaultToolkit().beep();
                            }
                        }
                    }
                }
            }

            @Override 
            public void onClose() { 
                System.out.println("MainForm Listener: WS Closed."); 
                sharedWebSocketClient = null; 
                CryptoService.clearKeys();
            }
            @Override 
            public void onError(Exception ex) { 
                System.err.println("MainForm Listener: WS Error: " + ex.getMessage()); 
                sharedWebSocketClient = null; 
                CryptoService.clearKeys();
                SwingUtilities.invokeLater(()->JOptionPane.showMessageDialog(MainForm.this, "Lỗi WebSocket: " + ex.getMessage(), "Lỗi", 0));
            }
            @Override 
            public void setWebSocketClient(WebSocketClient client) { 
                System.out.println("MainForm Listener: WS client assigned."); 
                sharedWebSocketClient = client; 
            }
        };
    }

    @SuppressWarnings("unchecked") private void initComponents() {} // Giữ nguyên
}
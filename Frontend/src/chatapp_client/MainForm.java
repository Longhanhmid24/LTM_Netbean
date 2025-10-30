package chatapp_client;

import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.util.logging.*;
import javax.crypto.SecretKey;
import javax.swing.*;
import model.*;
import org.java_websocket.client.WebSocketClient;
import service.*;
import model.CallSignal;

/**
 * Main JFrame. Quản lý các panel và kết nối WebSocket chung. ✅ ĐÃ CẬP NHẬT: Xử
 * lý E2EE Lớp 2 (Chuyển tiếp tin nhắn, không giải mã).
 */
public class MainForm extends javax.swing.JFrame {

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
        setTitle("Chat App");
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);
        cardLayout = new CardLayout();
        pnMainContainer = new JPanel(cardLayout);
        loginPanel = new LoginForm(this);
        registerPanel = new RegisterForm(this);
        pnMainContainer.add(loginPanel, LOGIN_PANEL);
        pnMainContainer.add(registerPanel, REGISTER_PANEL);
        this.setContentPane(pnMainContainer);
        cardLayout.show(pnMainContainer, LOGIN_PANEL);
        pack();
        setExtendedState(javax.swing.JFrame.MAXIMIZED_BOTH);
        createWebSocketListener();
    }

    public void showCard(String cardName) {
        cardLayout.show(pnMainContainer, cardName);
        if (cardName.equals(LOGIN_PANEL) && loginPanel != null) {
            loginPanel.clearFields();
        } else if (cardName.equals(REGISTER_PANEL) && registerPanel != null) {
            registerPanel.clearFields();
        }
    }

    public void showMainApp(User user) {
        this.loggedInUser = user;
        this.loggedInUserId = user.getId();
        connectSharedWebSocket();
        mainChatPanel = new MainChatPanel(this);
        pnMainContainer.add(mainChatPanel, MAIN_APP_PANEL);
        cardLayout.show(pnMainContainer, MAIN_APP_PANEL);
    }

    public void showPrivateChatForm(int friendId, String username) {
        if (mainChatPanel != null) {
            mainChatPanel.showPrivateChat(friendId, username);
        } else {
            showCard(LOGIN_PANEL);
        }
    }

    public void showGroupChatForm(int groupId, String groupName) {
        if (mainChatPanel != null) {
            mainChatPanel.showGroupChat(groupId, groupName);
        } else {
            showCard(LOGIN_PANEL);
        }
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }

    public int getLoggedInUserId() {
        return loggedInUserId;
    }

    public void setLoggedInUserId(int userId) {
        this.loggedInUserId = userId;
    }

    public WebSocketClient getSharedWebSocketClient() {
        return sharedWebSocketClient;
    }

    private void connectSharedWebSocket() {
        if (sharedWebSocketClient != null && sharedWebSocketClient.isOpen()) {
            sharedWebSocketClient.close(); // ❗ đóng WS cũ nếu có
        }

        if (loggedInUserId > 0 && wsListener != null) {
            NetworkService.connectWS(loggedInUserId, wsListener);
            System.out.println("✅ WebSocket connected for userId: " + loggedInUserId);
        } else {
            System.err.println("❌ Cannot connect WS: missing userId or listener");
        }
    }

    private void createWebSocketListener() {
        wsListener = new NetworkService.WSMessageListener() {
            @Override
            public void onStompConnected() {
                System.out.println("MainForm Listener: STOMP Connected. Subscribing groups...");

                // ✅ Subscribe queue nhận cuộc gọi đến
                String subCall = "SUBSCRIBE\nid:call-" + loggedInUserId
                        + "\ndestination:/queue/call/" + loggedInUserId
                        + "\nack:auto\n\n\0";
                sharedWebSocketClient.send(subCall);
                System.out.println("📡 Subscribed to call queue for user " + loggedInUserId);

                // ✅ Giữ nguyên phần subscribe group
                NetworkService.getGroupsForUser(loggedInUserId).thenAccept(groups -> {
                    if (sharedWebSocketClient != null && sharedWebSocketClient.isOpen() && groups != null) {
                        System.out.println("Subscribing to " + groups.size() + " group topics...");
                        for (Group group : groups) {
                            String subFrame
                                    = "SUBSCRIBE\nid:sub-group-" + group.getId()
                                    + "\ndestination:/topic/group/" + group.getId()
                                    + "\nack:auto\n\n\0";
                            sharedWebSocketClient.send(subFrame);
                        }
                    }
                }).exceptionally(ex -> {
                    System.err.println("Error getting groups: " + ex.getMessage());
                    return null;
                });
            }

            @Override
            public void onMessageReceived(String headers, String body) {
                String destination = "";
                int destIndex = headers.indexOf("destination:");
                if (destIndex != -1) {
                    int lineEnd = headers.indexOf('\n', destIndex);
                    if (lineEnd == -1) {
                        lineEnd = headers.length();
                    }
                    destination = headers.substring(destIndex + "destination:".length(), lineEnd).trim();
                }

                if (destination.startsWith("/queue/messages/" + loggedInUserId)) {
                    ChatMessage msg = NetworkService.parseStompMessage(body);
                    if (msg == null) {
                        return;
                    }
                    if (msg.getSenderId() == loggedInUserId) {
                        return;
                    }
                    if (mainChatPanel != null) {
                        ChatForm chatForm = mainChatPanel.getOpenPrivateChatForm(msg.getSenderId());
                        if (chatForm != null) {
                            SwingUtilities.invokeLater(() -> chatForm.handleIncomingMessage(msg));
                        } else {
                            Toolkit.getDefaultToolkit().beep();
                        }
                    }
                } else if (destination.startsWith("/topic/group/")) {
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

                if (destination.startsWith("/queue/call/" + loggedInUserId)) {
                    CallSignal signal = NetworkService.parseCallSignal(body);

                    if ("call_request".equals(signal.getType())) {
                        SwingUtilities.invokeLater(() -> {
                            new IncomingCallDialog(
                                    MainForm.this,
                                    "User " + signal.getCallerId(),
                                    () -> acceptCall(signal),
                                    () -> rejectCall(signal)
                            ).setVisible(true);
                        });
                    }
                }
            }

            private void acceptCall(CallSignal signal) {
                try {
                    String callUrl = "http://localhost:8080/call.html"
                            + "?callId=" + signal.getCallId()
                            + "&userId=" + loggedInUserId
                            + "&peerId=" + signal.getCallerId()
                            + "&type=" + signal.getType();

                    Desktop.getDesktop().browse(new URI(callUrl));
                } catch (Exception e) { e.printStackTrace(); }

                // gửi answer signal
                String frame = """
                    SEND
                    destination:/app/call.send
                    content-type:application/json

                    {"type":"answer","callerId":%d,"receiverId":%d,"callId":%d}\0
                    """.formatted(signal.getReceiverId(), signal.getCallerId(), signal.getCallId());

                sharedWebSocketClient.send(frame);
            }

            private void rejectCall(CallSignal signal) {
                String frame = """
                SEND
                destination:/app/call.send
                content-type:application/json

                {"type":"reject","callerId":%d,"receiverId":%d}\0
                """.formatted(signal.getReceiverId(), signal.getCallerId());
                sharedWebSocketClient.send(frame);
            }

            @Override
            public void onClose() {
                sharedWebSocketClient = null;
                CryptoService.clearKeys();
            }

            @Override
            public void onError(Exception ex) {
                sharedWebSocketClient = null;
                CryptoService.clearKeys();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(MainForm.this, "Lỗi WebSocket: " + ex.getMessage(), "Lỗi", 0));
            }

            @Override
            public void setWebSocketClient(WebSocketClient client) {
                sharedWebSocketClient = client;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
    }
}

package chatapp_client;

import model.Group;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import service.KeyService;
import service.CryptoService;

/**
 * Panel chính của ứng dụng sau khi đăng nhập.
 * ✅ ĐÃ CẬP NHẬT: Thêm tên người dùng cạnh nút Đăng xuất.
 */
public class MainChatPanel extends JPanel {

    private MainForm mainForm;
    private JSplitPane splitPane;
    private JTabbedPane listTabs;
    
    private UserListPanel userListPanel;
    private FriendListPanel friendListPanel;
    private FriendRequestPanel friendRequestPanel;
    private GroupListPanel groupListPanel;

    private JPanel chatAreaPanel;
    private CardLayout chatCardLayout;

    private Map<Integer, ChatForm> openPrivateChats;
    private Map<Integer, GroupChatForm> openGroupChats;
    private Map<String, JPanel> chatViewPanels;

    public MainChatPanel(MainForm mainForm) {
        this.mainForm = mainForm;
        this.openPrivateChats = new HashMap<>();
        this.openGroupChats = new HashMap<>();
        this.chatViewPanels = new HashMap<>();

        setLayout(new BorderLayout());

        splitPane = new JSplitPane();
        splitPane.setResizeWeight(0.3);
        splitPane.setDividerSize(5);
        splitPane.setContinuousLayout(true);

        // --- Panel Bên Trái (Side Panel - Tab) ---
        listTabs = new JTabbedPane();
        listTabs.setFont(new Font("Segoe UI", Font.BOLD, 14));

        userListPanel = new UserListPanel(mainForm);
        listTabs.addTab("Tất cả", userListPanel); 

        friendListPanel = new FriendListPanel(mainForm); 
        listTabs.addTab("Bạn bè", friendListPanel); 
        
        friendRequestPanel = new FriendRequestPanel(mainForm); 
        listTabs.addTab("Lời mời", friendRequestPanel); 

        groupListPanel = new GroupListPanel(mainForm);
        listTabs.addTab("Nhóm", groupListPanel); 
        
        // Tự động tải lại danh sách khi nhấn vào tab
        listTabs.addChangeListener(e -> {
            Component selected = listTabs.getSelectedComponent();
            if (selected == friendListPanel) {
                friendListPanel.loadFriends();
            } else if (selected == friendRequestPanel) {
                friendRequestPanel.loadFriendRequests();
            } else if (selected == userListPanel) {
                userListPanel.loadUsers();
            } else if (selected == groupListPanel) {
                groupListPanel.loadGroups();
            }
        });

        listTabs.setMinimumSize(new Dimension(250, 100));
        listTabs.setPreferredSize(new Dimension(300, 600));
        
        JPanel sidePanelWrapper = new JPanel(new BorderLayout());
        sidePanelWrapper.add(listTabs, BorderLayout.CENTER);

        // --- ✅ SỬA ĐỔI: Panel Đăng xuất ---
        JPanel logoutPanel = new JPanel(new BorderLayout(10, 0)); // Dùng BorderLayout
        logoutPanel.setBackground(new Color(230, 230, 230));
        logoutPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); // Padding

        // Nút Đăng xuất (đặt ở bên TRÁI)
        JButton btnLogout = new JButton("Đăng Xuất");
        btnLogout.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogout.setBackground(new Color(220, 50, 50)); btnLogout.setForeground(Color.WHITE);
        btnLogout.setFocusPainted(false);
        btnLogout.addActionListener(evt -> { if (JOptionPane.showConfirmDialog(this,"Đăng xuất?","Xác nhận",JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) performLogout(); });
        
        // Tên người dùng (đặt ở bên PHẢI)
        JLabel lblCurrentUser = new JLabel();
        String username = mainForm.getLoggedInUser() != null ? mainForm.getLoggedInUser().getUsername() : "User";
        lblCurrentUser.setText("Chào, " + username + "!");
        lblCurrentUser.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblCurrentUser.setForeground(new Color(50, 50, 50));
        lblCurrentUser.setHorizontalAlignment(SwingConstants.RIGHT); // Căn phải

        logoutPanel.add(btnLogout, BorderLayout.WEST);
        logoutPanel.add(lblCurrentUser, BorderLayout.CENTER); // Đặt vào giữa (sẽ chiếm hết)
        // --- Hết sửa đổi ---

        sidePanelWrapper.add(logoutPanel, BorderLayout.SOUTH);

        splitPane.setLeftComponent(sidePanelWrapper);

        // --- Panel bên phải (Khu vực chat) ---
        chatCardLayout = new CardLayout();
        chatAreaPanel = new JPanel(chatCardLayout);
        chatAreaPanel.setBackground(Color.WHITE);
        WelcomePanel welcomePanel = new WelcomePanel(mainForm.getLoggedInUser().getUsername());
        chatAreaPanel.add(welcomePanel, "WELCOME");
        splitPane.setRightComponent(chatAreaPanel);

        add(splitPane, BorderLayout.CENTER);
        chatCardLayout.show(chatAreaPanel, "WELCOME");
    }

    private void performLogout() { /* (Giữ nguyên) */
        CryptoService.clearKeys();
        openPrivateChats.values().forEach(ChatForm::disposeWebSocket);
        openPrivateChats.clear();
        openGroupChats.clear();
        chatViewPanels.clear();
        mainForm.setLoggedInUserId(0);
        if (mainForm.getSharedWebSocketClient() != null && mainForm.getSharedWebSocketClient().isOpen()) {
             mainForm.getSharedWebSocketClient().close();
        }
        mainForm.showCard(MainForm.LOGIN_PANEL);
    }
    
    /**
     * ✅ HÀM MỚI: Cho phép FriendRequestPanel yêu cầu tải lại tab "Bạn bè"
     */
    public void reloadFriendList() {
        if (friendListPanel != null) {
            friendListPanel.loadFriends();
        }
    }

    public void showPrivateChat(int friendId, String username) { /* (Giữ nguyên) */
        String cardName = "CHAT_USER_" + friendId;
        if (!chatViewPanels.containsKey(cardName)) {
            System.out.println("Tạo ChatForm mới cho user: " + friendId);
            ChatForm chatForm = new ChatForm(mainForm.getLoggedInUserId(), friendId, username, mainForm.getSharedWebSocketClient());
            JPanel fullChatView = createFullChatView(username, chatForm);
            chatAreaPanel.add(fullChatView, cardName);
            openPrivateChats.put(friendId, chatForm);
            chatViewPanels.put(cardName, fullChatView);
        }
        chatCardLayout.show(chatAreaPanel, cardName);
    }

    public void showGroupChat(int groupId, String groupName) { /* (Giữ nguyên) */
        String cardName = "CHAT_GROUP_" + groupId;
        if (!chatViewPanels.containsKey(cardName)) {
            System.out.println("Tạo GroupChatForm mới cho nhóm: " + groupId);
            GroupChatForm groupChatForm = new GroupChatForm(mainForm.getLoggedInUserId(), groupId, groupName, mainForm.getSharedWebSocketClient());
            JPanel fullChatView = createFullChatView(groupName, groupChatForm);
            chatAreaPanel.add(fullChatView, cardName);
            openGroupChats.put(groupId, groupChatForm);
            chatViewPanels.put(cardName, fullChatView);
        }
        chatCardLayout.show(chatAreaPanel, cardName);
    }

     // Getters (Giữ nguyên)
     public GroupChatForm getOpenGroupChatForm(int groupId) { return openGroupChats.get(groupId); }
     public ChatForm getOpenPrivateChatForm(int friendId) { return openPrivateChats.get(friendId); }

    // Hàm helper tạo header (Giữ nguyên)
    private JPanel createFullChatView(String title, JComponent chatContentPanel) {
        JPanel container = new JPanel(new BorderLayout()); container.setBackground(Color.WHITE);
        JPanel topBar = new JPanel(new BorderLayout()); topBar.setBackground(new Color(248, 248, 248)); topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)));
        JLabel lblTitle = new JLabel(" " + title, SwingConstants.LEFT); lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18)); lblTitle.setForeground(new Color(50, 50, 50)); lblTitle.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15)); topBar.add(lblTitle, BorderLayout.CENTER);
        container.add(topBar, BorderLayout.NORTH); container.add(chatContentPanel, BorderLayout.CENTER); return container;
    }
}
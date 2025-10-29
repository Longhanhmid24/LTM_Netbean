package chatapp_client;

import model.User;
import model.Group; // ✅ IMPORT MỚI
import service.NetworkService;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Panel hiển thị danh sách TẤT CẢ người dùng VÀ NHÓM.
 * ✅ ĐÃ CẬP NHẬT: Tải và hiển thị cả User và Group.
 * ✅ ĐÃ SỬA LỖI: Xóa ký tự '_' lỗi.
 */
public class UserListPanel extends JPanel { 

    private JList<Object> lstItems; // ✅ Sửa: JList<Object>
    private MainForm mainForm;
    private DefaultListModel<Object> listModel; // ✅ Sửa: DefaultListModel<Object>
    private JTextField txtSearch;
    private List<Object> allItems; // ✅ Sửa: List<Object>

    public UserListPanel(MainForm main) {
        this.mainForm = main;
        this.allItems = new ArrayList<>();
        setLayout(new BorderLayout());
        setBackground(new Color(249, 249, 249));

        // --- Panel Tìm kiếm ---
        JPanel searchBarPanel = new JPanel(new BorderLayout(5, 0));
        searchBarPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        searchBarPanel.setBackground(new Color(230, 230, 230));
        txtSearch = new JTextField("Tìm kiếm...");
        // ... (Cấu hình txtSearch giữ nguyên) ...
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14)); txtSearch.setForeground(Color.GRAY);
        txtSearch.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1, true), BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        txtSearch.addFocusListener(new FocusAdapter() { @Override public void focusGained(FocusEvent e) { if (txtSearch.getText().equals("Tìm kiếm...")) { txtSearch.setText(""); txtSearch.setForeground(Color.BLACK); } } @Override public void focusLost(FocusEvent e) { if (txtSearch.getText().isEmpty()) { txtSearch.setText("Tìm kiếm..."); txtSearch.setForeground(Color.GRAY); } } });
        txtSearch.getDocument().addDocumentListener(new DocumentListener() { @Override public void insertUpdate(DocumentEvent e) { filterList(); } @Override public void removeUpdate(DocumentEvent e) { filterList(); } @Override public void changedUpdate(DocumentEvent e) { filterList(); } });
        searchBarPanel.add(txtSearch, BorderLayout.CENTER);

        // --- Danh sách (Tổng hợp) ---
        listModel = new DefaultListModel<>();
        lstItems = new JList<>(listModel); // ✅ Sửa: JList<Object>
        lstItems.setCellRenderer(new UserListRenderer()); // (Renderer đã được sửa)
        lstItems.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstItems.setBackground(new Color(249, 249, 249));
        lstItems.setFixedCellHeight(64);
        
        // ✅ SỬA ĐỔI: Xử lý click cho cả User và Group
        lstItems.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    Object selected = lstItems.getSelectedValue();
                    if (selected instanceof User) {
                        User selectedUser = (User) selected;
                        if (selectedUser.getId() != mainForm.getLoggedInUserId()) {
                            mainForm.showPrivateChatForm(selectedUser.getId(), selectedUser.getUsername());
                        }
                    } else if (selected instanceof Group) {
                        Group selectedGroup = (Group) selected;
                        mainForm.showGroupChatForm(selectedGroup.getId(), selectedGroup.getName());
                    }
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() && lstItems.getSelectedIndex() != -1) {
                    lstItems.setSelectedIndex(lstItems.locationToIndex(e.getPoint()));
                    showPopupMenu(e); // (Hàm popup menu cũng cần được sửa)
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(lstItems);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        add(searchBarPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        loadUsers();
    }

    /**
     * ✅ SỬA ĐỔI: Tải TẤT CẢ (User và Group)
     */
    public void loadUsers() {
       if (mainForm.getLoggedInUserId() <= 0) return;
       
       CompletableFuture<List<User>> usersFuture = NetworkService.getAllUsers();
       CompletableFuture<List<Group>> groupsFuture = NetworkService.getGroupsForUser(mainForm.getLoggedInUserId());

       CompletableFuture.allOf(usersFuture, groupsFuture).thenRun(() -> {
           SwingUtilities.invokeLater(() -> {
               allItems.clear();
               listModel.clear();
               
               List<User> users = usersFuture.join();
               List<Group> groups = groupsFuture.join();
               
               if (users != null) {
                   for (User user : users) {
                       if (user.getId() != mainForm.getLoggedInUserId()) {
                           allItems.add(user);
                       } // ✅ SỬA LỖI: Xóa ký tự '_' ở dòng dưới
                   }
               }
               if (groups != null) {
                   allItems.addAll(groups);
               }
               
               filterList(); // Áp dụng filter
           });
       }).exceptionally(ex -> {
           SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi tải danh bạ hoặc nhóm: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE));
           return null;
       });
    }

    /**
     * ✅ SỬA ĐỔI: Lọc danh sách TẤT CẢ (User và Group)
     */
    private void filterList() {
        String query = txtSearch.getText(); if (query.equals("Tìm kiếm...")) query = "";
        query = query.toLowerCase().trim();
        listModel.clear();
        
        for (Object item : allItems) {
            boolean matches = false;
            if (item instanceof User) {
                User user = (User) item;
                boolean matchesUsername = user.getUsername().toLowerCase().contains(query);
                boolean matchesSdt = (user.getSdt() != null && user.getSdt().contains(query));
                matches = matchesUsername || matchesSdt;
            } else if (item instanceof Group) {
                Group group = (Group) item;
                matches = group.getName().toLowerCase().contains(query);
            }
            
            if (matches) {
                listModel.addElement(item);
            }
        }
    }
    
    /**
     * ✅ SỬA ĐỔI: Hiển thị menu chuột phải dựa trên loại (User/Group)
     */
    private void showPopupMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        Object selectedItem = lstItems.getSelectedValue();
        if (selectedItem == null) return;

        if (selectedItem instanceof User) {
            User selectedUser = (User) selectedItem;
            if (selectedUser.getId() == mainForm.getLoggedInUserId()) return;

            JMenuItem openChat = new JMenuItem("Mở Trò chuyện");
            openChat.setFont(new Font("Segoe UI", Font.BOLD, 14));
            openChat.addActionListener(evt -> mainForm.showPrivateChatForm(selectedUser.getId(), selectedUser.getUsername()));
            menu.add(openChat);

            JMenuItem addItem = new JMenuItem("➕ Gửi lời mời kết bạn");
            addItem.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            addItem.addActionListener(evt -> sendRequest(selectedUser));
            menu.add(addItem);
            
        } else if (selectedItem instanceof Group) {
            Group selectedGroup = (Group) selectedItem;
            
            JMenuItem openChat = new JMenuItem("Mở Trò chuyện Nhóm");
            openChat.setFont(new Font("Segoe UI", Font.BOLD, 14));
            openChat.addActionListener(evt -> mainForm.showGroupChatForm(selectedGroup.getId(), selectedGroup.getName()));
            menu.add(openChat);
        }

        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    /**
     * Gửi lời mời kết bạn (chỉ dùng cho User)
     */
    private void sendRequest(User userToRequest) {
        int myId = mainForm.getLoggedInUserId();
        int friendId = userToRequest.getId();
        
        NetworkService.sendFriendRequest(myId, friendId)
            .thenAccept(success -> {
                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        JOptionPane.showMessageDialog(this, "Đã gửi lời mời kết bạn đến " + userToRequest.getUsername(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, "Gửi lời mời thất bại. (Có thể đã tồn tại)", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }).exceptionally(ex -> {
                 SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi Mạng", JOptionPane.ERROR_MESSAGE));
                 return null;
            });
    }
}
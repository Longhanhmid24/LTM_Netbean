package chatapp_client;

import model.FriendRequest;
import model.Friendship;
import model.User;
import service.NetworkService;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * Panel hiển thị danh sách LỜI MỜI KẾT BẠN (pending)
 * và cho phép Chấp nhận / Từ chối.
 */
public class FriendRequestPanel extends JPanel { 

    private JList<User> lstRequests;
    private MainForm mainForm;
    private DefaultListModel<User> listModel;
    private List<User> allRequests; // Danh sách các user đã gửi lời mời

    public FriendRequestPanel(MainForm main) {
        this.mainForm = main;
        this.allRequests = new ArrayList<>();
        setLayout(new BorderLayout());
        setBackground(new Color(249, 249, 249));

        // --- Panel Header (Chỉ là tiêu đề) ---
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        headerPanel.setBackground(new Color(230, 230, 230));
        JLabel lblTitle = new JLabel("Các lời mời đã nhận");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        headerPanel.add(lblTitle);

        // --- Danh sách Lời mời ---
        listModel = new DefaultListModel<>();
        lstRequests = new JList<>(listModel);
        lstRequests.setCellRenderer(new UserListRenderer()); // Dùng chung Renderer
        lstRequests.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstRequests.setBackground(new Color(249, 249, 249));
        lstRequests.setFixedCellHeight(64);
        
        // Thêm Menu Chuột phải (Chấp nhận / Từ chối)
        lstRequests.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() && lstRequests.getSelectedIndex() != -1) {
                    showPopupMenu(e);
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(lstRequests);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        loadFriendRequests();
    }
    
    // Tải danh sách lời mời
    public void loadFriendRequests() {
         if (mainForm.getLoggedInUserId() <= 0) return;
         
         NetworkService.getFriendRequests(mainForm.getLoggedInUserId()).thenAccept(requests -> {
            SwingUtilities.invokeLater(() -> {
                allRequests.clear();
                listModel.clear();
                if (requests != null) {
                    for (FriendRequest req : requests) {
                        User user = req.toUser(); // Chuyển đổi thành User để hiển thị
                        allRequests.add(user);
                        listModel.addElement(user);
                    }
                }
            });
         }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi tải lời mời: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE));
            return null;
         });
    }

    // Hiển thị Popup Menu
    private void showPopupMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        User selectedUser = lstRequests.getSelectedValue();
        if (selectedUser == null) return;

        // 1. Nút Chấp nhận
        JMenuItem acceptItem = new JMenuItem("✅ Chấp nhận");
        acceptItem.setFont(new Font("Segoe UI", Font.BOLD, 14));
        acceptItem.setForeground(new Color(0, 150, 0));
        acceptItem.addActionListener(evt -> acceptRequest(selectedUser));
        menu.add(acceptItem);

        // 2. Nút Từ chối (Xóa)
        JMenuItem rejectItem = new JMenuItem("❌ Từ chối (Xóa)");
        rejectItem.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        rejectItem.addActionListener(evt -> rejectRequest(selectedUser));
        menu.add(rejectItem);

        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    // Xử lý Chấp nhận
    private void acceptRequest(User sender) {
        int myId = mainForm.getLoggedInUserId();
        int senderId = sender.getId();
        
        // (API /api/friendships/accept yêu cầu actionUserId là người chấp nhận)
        NetworkService.acceptFriendRequest(senderId, myId, myId)
            .thenAccept(success -> {
                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        JOptionPane.showMessageDialog(this, "Đã chấp nhận " + sender.getUsername(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        loadFriendRequests(); // Tải lại danh sách lời mời
                        // TODO: Yêu cầu MainChatPanel tải lại Tab "Bạn bè"
                    } else {
                        JOptionPane.showMessageDialog(this, "Chấp nhận thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }).exceptionally(ex -> {
                 SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi Mạng", JOptionPane.ERROR_MESSAGE));
                 return null;
            });
    }
    
    // Xử lý Từ chối
    private void rejectRequest(User sender) {
        int myId = mainForm.getLoggedInUserId();
        int senderId = sender.getId();

        // (API /api/friendships/remove không quan tâm actionUserId)
        NetworkService.removeFriend(senderId, myId)
            .thenAccept(success -> {
                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        JOptionPane.showMessageDialog(this, "Đã từ chối " + sender.getUsername(), "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                        loadFriendRequests(); // Tải lại danh sách lời mời
                    } else {
                        JOptionPane.showMessageDialog(this, "Từ chối thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }).exceptionally(ex -> {
                 SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi Mạng", JOptionPane.ERROR_MESSAGE));
                 return null;
            });
    }
}
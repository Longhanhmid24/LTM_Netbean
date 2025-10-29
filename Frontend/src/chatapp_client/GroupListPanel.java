package chatapp_client;

import model.Group;
import model.GroupMember; // ✅ IMPORT MỚI
import model.User; // ✅ IMPORT MỚI
import service.NetworkService;
import java.awt.*;
import java.awt.event.*;
import java.io.File; // ✅ IMPORT MỚI
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture; // ✅ IMPORT MỚI
import java.util.stream.Collectors; // ✅ IMPORT MỚI
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Panel hiển thị danh sách nhóm chat và thanh tìm kiếm.
 * ✅ ĐÃ CẬP NHẬT: Xử lý tạo nhóm (với avatar) và thêm/xóa thành viên.
 */
public class GroupListPanel extends JPanel {

    // ... (Components và variables giữ nguyên) ...
    private JList<Group> lstGroups;
    private MainForm mainForm;
    private DefaultListModel<Group> listModel;
    private JTextField txtSearch;
    private List<Group> allGroups;
    private JButton btnCreateGroup;

    public GroupListPanel(MainForm main) {
        this.mainForm = main;
        this.allGroups = new ArrayList<>();
        setLayout(new BorderLayout());
        setBackground(new Color(249, 249, 249));

        // --- Panel Header (Tìm kiếm và Nút Tạo) ---
        JPanel headerPanel = new JPanel(new BorderLayout(5, 5));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        headerPanel.setBackground(new Color(230, 230, 230));
        txtSearch = new JTextField("Tìm nhóm...");
        // ... (Cấu hình txtSearch giữ nguyên) ...
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14)); txtSearch.setForeground(Color.GRAY);
        txtSearch.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1, true), BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        txtSearch.addFocusListener(new FocusAdapter() { @Override public void focusGained(FocusEvent e) { if (txtSearch.getText().equals("Tìm nhóm...")) { txtSearch.setText(""); txtSearch.setForeground(Color.BLACK); } } @Override public void focusLost(FocusEvent e) { if (txtSearch.getText().isEmpty()) { txtSearch.setText("Tìm nhóm..."); txtSearch.setForeground(Color.GRAY); } } });
        txtSearch.getDocument().addDocumentListener(new DocumentListener() { @Override public void insertUpdate(DocumentEvent e) { filterList(); } @Override public void removeUpdate(DocumentEvent e) { filterList(); } @Override public void changedUpdate(DocumentEvent e) { filterList(); } });

        btnCreateGroup = new JButton("+");
        // ... (Cấu hình btnCreateGroup giữ nguyên) ...
        btnCreateGroup.setFont(new Font("Segoe UI", Font.BOLD, 18)); btnCreateGroup.setToolTipText("Tạo nhóm chat mới"); btnCreateGroup.setMargin(new Insets(2, 8, 2, 8)); btnCreateGroup.setBackground(new Color(76, 175, 80)); btnCreateGroup.setForeground(Color.WHITE); btnCreateGroup.setFocusPainted(false);
        btnCreateGroup.addActionListener(e -> showCreateGroupDialog());

        headerPanel.add(txtSearch, BorderLayout.CENTER);
        headerPanel.add(btnCreateGroup, BorderLayout.EAST);

        // --- Danh sách Nhóm ---
        listModel = new DefaultListModel<>();
        lstGroups = new JList<>(listModel);
        lstGroups.setCellRenderer(new GroupListRenderer());
        lstGroups.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstGroups.setBackground(new Color(249, 249, 249));
        lstGroups.setFixedCellHeight(64);
        
        // ✅ SỬA ĐỔI: Thêm MouseAdapter cho cả click trái và phải
        lstGroups.addMouseListener(new MouseAdapter() {
             @Override
             public void mouseClicked(MouseEvent e) {
                 if (e.getButton() == MouseEvent.BUTTON1) {
                     Group sel = lstGroups.getSelectedValue();
                     if (sel != null) mainForm.showGroupChatForm(sel.getId(), sel.getName());
                 }
             }
             @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() && lstGroups.getSelectedIndex() != -1) {
                    lstGroups.setSelectedIndex(lstGroups.locationToIndex(e.getPoint()));
                    showPopupMenu(e);
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(lstGroups);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        loadGroups();
    }

    public void loadGroups() { /* Giữ nguyên */
        if (mainForm.getLoggedInUserId() <= 0) return;
        NetworkService.getGroupsForUser(mainForm.getLoggedInUserId()).thenAccept(groups -> {
            SwingUtilities.invokeLater(() -> { allGroups.clear(); listModel.clear(); if (groups != null) allGroups.addAll(groups); filterList(); });
        }).exceptionally(ex -> { SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi tải nhóm: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE)); return null; });
    }
    private void filterList() { /* Giữ nguyên */
        String query = txtSearch.getText(); if (query.equals("Tìm nhóm...")) query = ""; query = query.toLowerCase().trim();
        listModel.clear();
        for (Group group : allGroups) { if (group.getName().toLowerCase().contains(query)) listModel.addElement(group); }
    }

    /**
     * ✅ SỬA ĐỔI: Xử lý tạo nhóm (với avatar và mời bạn bè)
     */
    private void showCreateGroupDialog() {
        CreateGroupDialog dialog = new CreateGroupDialog(mainForm);
        dialog.setVisible(true); 

        String newGroupName = dialog.getGroupName();
        File avatarFile = dialog.getSelectedAvatarFile();
        List<User> membersToInvite = dialog.getSelectedMembers();

        if (newGroupName != null) {
            // Bước 1: Upload avatar (nếu có)
            CompletableFuture<String> avatarUrlFuture;
            if (avatarFile != null) {
                avatarUrlFuture = NetworkService.uploadFile(avatarFile);
            } else {
                avatarUrlFuture = CompletableFuture.completedFuture(null); // Không có avatar
            }

            // Bước 2: Sau khi có URL avatar, tạo nhóm
            avatarUrlFuture.thenCompose(avatarUrl -> {
                return NetworkService.createGroup(newGroupName, mainForm.getLoggedInUserId(), avatarUrl);
            
            }).thenAccept(createdGroup -> {
                if (createdGroup != null) {
                    // Bước 3: Mời các thành viên đã chọn (nếu có)
                    if (membersToInvite != null && !membersToInvite.isEmpty()) {
                        List<CompletableFuture<Boolean>> inviteFutures = new ArrayList<>();
                        for (User member : membersToInvite) {
                            inviteFutures.add(
                                NetworkService.addMemberToGroup(createdGroup.getId(), member.getId())
                            );
                        }
                        // Chờ tất cả lời mời hoàn tất (không bắt buộc)
                        CompletableFuture.allOf(inviteFutures.toArray(new CompletableFuture[0]))
                            .thenRun(() -> System.out.println("Đã gửi " + inviteFutures.size() + " lời mời."));
                    }
                    
                    // Bước 4: Cập nhật UI
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Tạo nhóm '" + createdGroup.getName() + "' thành công!", "Thành Công", 1);
                        loadGroups(); // Tải lại danh sách nhóm
                        // TODO: Tự động subscribe kênh WebSocket cho nhóm mới
                    });
                } else {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Tạo nhóm thất bại!", "Lỗi", 0));
                }
            }).exceptionally(ex -> {
                 SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi khi tạo nhóm: " + ex.getMessage(), "Lỗi Mạng", 0));
                 return null;
            });
        }
    }
    
    /**
     * ✅ HÀM MỚI: Hiển thị Popup Menu cho Nhóm (Thêm thành viên, Xóa nhóm)
     */
    private void showPopupMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        Group selectedGroup = lstGroups.getSelectedValue();
        if (selectedGroup == null) return;

        // 1. Nút Mở Trò chuyện
        JMenuItem openChat = new JMenuItem("Mở Trò chuyện Nhóm");
        openChat.setFont(new Font("Segoe UI", Font.BOLD, 14));
        openChat.addActionListener(evt -> mainForm.showGroupChatForm(selectedGroup.getId(), selectedGroup.getName()));
        menu.add(openChat);
        
        menu.addSeparator();

        // 2. Nút Thêm Thành viên
        JMenuItem addItem = new JMenuItem("➕ Mời thành viên...");
        addItem.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        addItem.addActionListener(evt -> showAddMemberDialog(selectedGroup));
        menu.add(addItem);
        
        // 3. Nút Xóa Nhóm (Chỉ hiển thị nếu là creator)
        if (selectedGroup.getCreatorId() == mainForm.getLoggedInUserId()) {
            menu.addSeparator();
            JMenuItem deleteItem = new JMenuItem("❌ Xóa Nhóm");
            deleteItem.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            deleteItem.setForeground(Color.RED);
            deleteItem.addActionListener(evt -> deleteGroup(selectedGroup));
            menu.add(deleteItem);
        }

        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    /**
     * ✅ HÀM MỚI: Mở dialog mời bạn bè
     */
    private void showAddMemberDialog(Group group) {
        // 1. Lấy danh sách thành viên HIỆN TẠI để lọc
        NetworkService.getGroupMembers(group.getId()).thenAccept(members -> {
            List<Integer> existingMemberIds = members.stream()
                                                .map(GroupMember::getMemberId)
                                                .collect(Collectors.toList());
            
            // 2. Mở Dialog với danh sách đã lọc
            SwingUtilities.invokeLater(() -> {
                AddMemberDialog dialog = new AddMemberDialog(mainForm, group.getId(), existingMemberIds);
                dialog.setVisible(true);
                
                List<User> membersToAdd = dialog.getSelectedMembers();
                
                if (membersToAdd != null && !membersToAdd.isEmpty()) {
                    // 3. Gửi yêu cầu thêm
                    List<CompletableFuture<Boolean>> addFutures = new ArrayList<>();
                    for (User member : membersToAdd) {
                        addFutures.add(
                            NetworkService.addMemberToGroup(group.getId(), member.getId())
                        );
                    }
                    CompletableFuture.allOf(addFutures.toArray(new CompletableFuture[0]))
                        .thenRun(() -> 
                            SwingUtilities.invokeLater(() -> 
                                JOptionPane.showMessageDialog(this, "Đã gửi " + membersToAdd.size() + " lời mời.", "Thành công", 1)
                            )
                        );
                }
            });
            
        }).exceptionally(ex -> {
             SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi tải thành viên nhóm: " + ex.getMessage(), "Lỗi Mạng", 0));
             return null;
        });
    }

    /**
     * ✅ HÀM MỚI: Xử lý xóa nhóm
     */
    private void deleteGroup(Group group) {
        if (JOptionPane.showConfirmDialog(this, 
                "Bạn có chắc muốn XÓA vĩnh viễn nhóm '" + group.getName() + "'?\n" +
                "Tất cả lịch sử chat nhóm sẽ bị mất.", 
                "Xác nhận Xóa Nhóm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) 
                != JOptionPane.YES_OPTION) {
            return;
        }

        NetworkService.deleteGroup(group.getId(), mainForm.getLoggedInUserId())
            .thenAccept(success -> {
                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        JOptionPane.showMessageDialog(this, "Đã xóa nhóm " + group.getName(), "Thành công", 1);
                        loadGroups(); // Tải lại danh sách nhóm
                    } else {
                        JOptionPane.showMessageDialog(this, "Xóa nhóm thất bại (Bạn phải là người tạo nhóm).", "Lỗi", 0);
                    }
                });
            }).exceptionally(ex -> {
                 SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi Mạng", 0));
                 return null;
            });
    }
}
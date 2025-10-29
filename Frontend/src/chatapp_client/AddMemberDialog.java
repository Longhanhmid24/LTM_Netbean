package chatapp_client;

import model.User;
import service.NetworkService;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;

/**
 * Dialog (hộp thoại) để thêm thành viên mới vào nhóm đã tồn tại.
 * ✅ SỬA ĐỔI: Hiển thị TẤT CẢ USER (không chỉ bạn bè).
 */
public class AddMemberDialog extends JDialog {

    private MainForm mainForm;
    private int groupId;
    
    private JList<User> lstUsers; // Đổi tên từ lstFriends
    private DefaultListModel<User> listModel;
    private List<User> userList; // Danh sách tất cả user (đã lọc)
    
    private JButton btnAdd, btnCancel;
    private List<User> selectedMembers = null;

    /**
     * @param owner MainForm
     * @param groupId ID của nhóm cần thêm
     * @param existingMemberIds Danh sách ID của những người đã ở trong nhóm (để lọc)
     */
    public AddMemberDialog(Frame owner, int groupId, List<Integer> existingMemberIds) {
        super(owner, "Mời Thành Viên", true);
        this.mainForm = (MainForm) owner;
        this.groupId = groupId;
        
        setSize(400, 400);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // --- Panel Mời bạn bè ---
        JPanel friendsPanel = new JPanel(new BorderLayout());
        friendsPanel.setBorder(BorderFactory.createTitledBorder("Chọn người dùng để mời"));
        
        listModel = new DefaultListModel<>();
        lstUsers = new JList<>(listModel);
        lstUsers.setCellRenderer(new UserListRenderer());
        lstUsers.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        lstUsers.setFixedCellHeight(50);
        
        JScrollPane scrollPane = new JScrollPane(lstUsers);
        friendsPanel.add(scrollPane, BorderLayout.CENTER);

        // Tải danh sách TẤT CẢ USER (và lọc)
        loadAllUsers(existingMemberIds);

        // --- Panel Nút bấm (OK/Cancel) ---
        btnAdd = new JButton("Thêm vào nhóm");
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnAdd.setBackground(new Color(46, 204, 113));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.addActionListener(e -> addMembers());

        btnCancel = new JButton("Hủy");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnCancel.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.add(btnCancel);
        buttonPanel.add(btnAdd);

        // --- Thêm vào Dialog ---
        add(friendsPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * ✅ SỬA ĐỔI: Tải danh sách TẤT CẢ USER (lọc người đã ở trong nhóm và chính mình)
     */
    private void loadAllUsers(List<Integer> existingMemberIds) {
        // Thêm cả ID của mình vào danh sách loại trừ
        existingMemberIds.add(mainForm.getLoggedInUserId());
        
        NetworkService.getAllUsers().thenAccept(users -> {
            SwingUtilities.invokeLater(() -> {
                listModel.clear();
                if (users != null) {
                    // Lọc ra những người chưa có trong nhóm VÀ không phải là mình
                    this.userList = users.stream()
                        .filter(u -> !existingMemberIds.contains(u.getId()))
                        .collect(Collectors.toList());
                        
                    if (this.userList.isEmpty()) {
                        listModel.addElement(null); // (Tạo 1 item giả để báo)
                        lstUsers.setEnabled(false);
                        btnAdd.setEnabled(false);
                        lstUsers.setCellRenderer(new DefaultListCellRenderer() {
                            @Override
                            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                                label.setText("Không còn ai để mời.");
                                label.setForeground(Color.GRAY);
                                label.setHorizontalAlignment(SwingConstants.CENTER);
                                return label;
                            }
                        });
                    } else {
                        this.userList.forEach(listModel::addElement);
                    }
                }
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Không thể tải danh sách người dùng.", "Lỗi", 0));
            return null;
        });
    }

    // Xử lý thêm thành viên
    private void addMembers() {
        this.selectedMembers = lstUsers.getSelectedValuesList();
        if (this.selectedMembers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bạn chưa chọn ai để mời.", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        dispose(); // Đóng dialog
    }

    // Getter để GroupListPanel lấy kết quả
    public List<User> getSelectedMembers() { 
        return selectedMembers; 
    }
}

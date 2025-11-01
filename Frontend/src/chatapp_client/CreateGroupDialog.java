package chatapp_client;

import model.User;
import service.NetworkService;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Dialog (hộp thoại) để tạo nhóm mới.
 * ✅ ĐÃ CẬP NHẬT: Hiển thị TẤT CẢ USER (thay vì chỉ bạn bè) để mời.
 */
public class CreateGroupDialog extends JDialog {

    private MainForm mainForm;
    private JTextField txtGroupName;
    private JButton btnCreate, btnCancel, btnChooseAvatar;
    private JLabel lblAvatarPreview;
    
    // Danh sách TẤT CẢ USER
    private JList<User> lstUsers; // Đổi tên
    private DefaultListModel<User> listModel;
    private List<User> userList; // Danh sách user đầy đủ

    // Kết quả
    private String groupName = null;
    private File selectedAvatarFile = null;
    private List<User> selectedMembers = null;

    public CreateGroupDialog(Frame owner) {
        super(owner, "Tạo Nhóm Mới", true);
        this.mainForm = (MainForm) owner;
        
        setSize(450, 500);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // --- Panel Thông tin cơ bản (Tên & Avatar) ---
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("Thông tin nhóm"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // (Giữ nguyên logic Tên nhóm và Avatar)
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; infoPanel.add(new JLabel("Tên nhóm:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0; txtGroupName = new JTextField(); txtGroupName.setFont(new Font("Segoe UI", Font.PLAIN, 16)); infoPanel.add(txtGroupName, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; btnChooseAvatar = new JButton("Chọn ảnh"); btnChooseAvatar.setFont(new Font("Segoe UI", Font.PLAIN, 12)); btnChooseAvatar.addActionListener(e -> chooseAvatar()); infoPanel.add(btnChooseAvatar, gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0; lblAvatarPreview = new JLabel("Preview (64x64)"); lblAvatarPreview.setPreferredSize(new Dimension(64, 64)); lblAvatarPreview.setBorder(BorderFactory.createEtchedBorder()); lblAvatarPreview.setHorizontalAlignment(SwingConstants.CENTER); infoPanel.add(lblAvatarPreview, gbc);

        // --- Panel Mời thành viên (TẤT CẢ USER) ---
        JPanel usersPanel = new JPanel(new BorderLayout()); // Đổi tên
        usersPanel.setBorder(BorderFactory.createTitledBorder("Mời thành viên (Tùy chọn)"));
        
        listModel = new DefaultListModel<>();
        lstUsers = new JList<>(listModel); // Đổi tên
        lstUsers.setCellRenderer(new UserListRenderer()); // Dùng chung renderer
        lstUsers.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION); // Cho phép chọn nhiều
        lstUsers.setFixedCellHeight(75);
        
        JScrollPane scrollPane = new JScrollPane(lstUsers);
        usersPanel.add(scrollPane, BorderLayout.CENTER);

        // ✅ SỬA ĐỔI: Tải TẤT CẢ user
        loadAllUsers();

        // --- Panel Nút bấm (OK/Cancel) ---
        btnCreate = new JButton("Tạo Nhóm");
        btnCreate.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCreate.setBackground(new Color(46, 204, 113));
        btnCreate.setForeground(Color.WHITE);
        btnCreate.addActionListener(e -> createGroup());

        btnCancel = new JButton("Hủy");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnCancel.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.add(btnCancel);
        buttonPanel.add(btnCreate);

        // --- Thêm vào Dialog ---
        add(infoPanel, BorderLayout.NORTH);
        add(usersPanel, BorderLayout.CENTER); // Đổi tên
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * ✅ SỬA ĐỔI: Tải TẤT CẢ USER (lọc chính mình)
     */
    private void loadAllUsers() {
        NetworkService.getAllUsers().thenAccept(users -> {
            SwingUtilities.invokeLater(() -> {
                listModel.clear();
                this.userList = new ArrayList<>();
                if (users != null) {
                    for (User user : users) {
                        // Lọc chính mình ra khỏi danh sách mời
                        if (user.getId() != mainForm.getLoggedInUserId()) {
                            this.userList.add(user);
                            listModel.addElement(user);
                        }
                    }
                }
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Không thể tải danh sách người dùng.", "Lỗi", 0));
            return null;
        });
    }

    // (Giữ nguyên: chooseAvatar)
    private void chooseAvatar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn Ảnh Đại Diện Nhóm");
        chooser.setFileFilter(new FileNameExtensionFilter("Ảnh (jpg, png, gif)", "jpg", "png", "gif", "jpeg"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedAvatarFile = chooser.getSelectedFile();
            ImageIcon icon = new ImageIcon(selectedAvatarFile.getAbsolutePath());
            Image scaledImg = icon.getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);
            lblAvatarPreview.setIcon(new ImageIcon(scaledImg));
            lblAvatarPreview.setText("");
        }
    }

    // (Giữ nguyên: createGroup)
    private void createGroup() {
        String name = txtGroupName.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tên nhóm không được để trống!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        this.groupName = name;
        this.selectedMembers = lstUsers.getSelectedValuesList(); // Lấy danh sách user đã chọn
        
        dispose(); // Đóng dialog
    }

    // Getters (Giữ nguyên)
    public String getGroupName() { return groupName; }
    public File getSelectedAvatarFile() { return selectedAvatarFile; }
    public List<User> getSelectedMembers() { return selectedMembers; }
}
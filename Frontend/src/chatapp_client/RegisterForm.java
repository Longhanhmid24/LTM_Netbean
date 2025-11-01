package chatapp_client;

import model.RegisterRequest; // ✅ IMPORT MỚI
import model.User;
import service.NetworkService;
import service.CryptoService; // ✅ IMPORT MỚI
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Map; // ✅ IMPORT MỚI
import java.util.concurrent.CompletableFuture;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Handles user registration.
 * ✅ ĐÃ CẬP NHẬT: Triển khai E2EE Lớp 2 (Mã hóa Private Key bằng Password).
 */
public class RegisterForm extends JPanel {

    // ... (Components giữ nguyên) ...
    private JTextField txtUser, txtSdt;
    private JPasswordField txtPass, txtConfirm;
    private JButton btnRegister, btnBack, btnChooseAvatar;
    private JLabel lblTitle, lblAvatarPreview;
    private MainForm mainForm;
    private File selectedAvatarFile;
    private String avatarUrl;

    public RegisterForm(MainForm main) { /* (Giữ nguyên UI constructor) */
        this.mainForm = main; setLayout(new BorderLayout()); setBackground(new Color(0, 102, 204)); JPanel cw = new JPanel(new GridBagLayout()); cw.setOpaque(false); JPanel cp = new JPanel(new GridBagLayout()); cp.setBackground(Color.WHITE); cp.setBorder(BorderFactory.createEmptyBorder(20, 30, 30, 30)); GridBagConstraints c = new GridBagConstraints(); c.insets = new Insets(8, 10, 8, 10); c.fill = 1; c.gridx = 0; c.weightx = 1.0; lblTitle = new JLabel("Tạo Tài Khoản", 0); lblTitle.setFont(new Font("Segoe UI", 1, 32)); lblTitle.setForeground(new Color(33, 33, 33)); c.gridy = 0; c.ipady = 10; cp.add(lblTitle, c); txtUser = new JTextField(20); txtUser.setFont(new Font("Segoe UI", 0, 16)); txtUser.setBorder(createTitledBorder("Tên đăng nhập")); txtUser.setPreferredSize(new Dimension(400, 50)); c.gridy = 1; c.ipady = 0; cp.add(txtUser, c); txtSdt = new JTextField(20); txtSdt.setFont(new Font("Segoe UI", 0, 16)); txtSdt.setBorder(createTitledBorder("Số điện thoại (SĐT)")); txtSdt.setPreferredSize(new Dimension(400, 50)); c.gridy = 2; cp.add(txtSdt, c); txtPass = new JPasswordField(20); txtPass.setFont(new Font("Segoe UI", 0, 16)); txtPass.setBorder(createTitledBorder("Mật khẩu")); txtPass.setPreferredSize(new Dimension(400, 50)); c.gridy = 3; cp.add(txtPass, c); txtConfirm = new JPasswordField(20); txtConfirm.setFont(new Font("Segoe UI", 0, 16)); txtConfirm.setBorder(createTitledBorder("Nhập lại mật khẩu")); txtConfirm.setPreferredSize(new Dimension(400, 50)); c.gridy = 4; cp.add(txtConfirm, c); JPanel avatarPanel = new JPanel(new BorderLayout(10, 0)); avatarPanel.setOpaque(false); btnChooseAvatar = new JButton("Chọn Ảnh Đại Diện"); btnChooseAvatar.setFont(new Font("Segoe UI", 0, 14)); btnChooseAvatar.setBackground(new Color(100, 149, 237)); btnChooseAvatar.setForeground(Color.WHITE); btnChooseAvatar.setFocusPainted(false); btnChooseAvatar.setCursor(Cursor.getPredefinedCursor(12)); btnChooseAvatar.setPreferredSize(new Dimension(120, 50)); btnChooseAvatar.addActionListener(e -> chooseAvatar()); lblAvatarPreview = new JLabel("Preview", 0); lblAvatarPreview.setFont(new Font("Segoe UI", 0, 12)); lblAvatarPreview.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1)); lblAvatarPreview.setPreferredSize(new Dimension(80, 80)); avatarPanel.add(btnChooseAvatar, BorderLayout.CENTER); avatarPanel.add(lblAvatarPreview, BorderLayout.EAST); c.gridy = 5; cp.add(avatarPanel, c); btnRegister = new JButton("Đăng ký"); btnRegister.setFont(new Font("Segoe UI", 1, 16)); btnRegister.setBackground(new Color(46, 204, 113)); btnRegister.setForeground(Color.WHITE); btnRegister.setFocusPainted(false); btnRegister.setCursor(Cursor.getPredefinedCursor(12)); btnRegister.setPreferredSize(new Dimension(400, 50)); btnRegister.addActionListener(e -> performRegister()); c.gridy = 6; c.insets = new Insets(20, 10, 10, 10); cp.add(btnRegister, c); btnBack = new JButton("< Quay lại đăng nhập"); btnBack.setFont(new Font("Segoe UI", 0, 14)); btnBack.setCursor(Cursor.getPredefinedCursor(12)); btnBack.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0)); btnBack.setContentAreaFilled(false); btnBack.setForeground(Color.GRAY); btnBack.addActionListener(e -> mainForm.showCard(MainForm.LOGIN_PANEL)); c.gridy = 7; c.insets = new Insets(10, 10, 10, 10); cp.add(btnBack, c); cw.add(cp); add(cw, BorderLayout.CENTER);
    }

    private javax.swing.border.TitledBorder createTitledBorder(String title){ /* Giữ nguyên */ return BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY, 1),title, 0, 0, new Font("Segoe UI", Font.PLAIN, 12)); }
    public void clearFields() { /* Giữ nguyên */ txtUser.setText(""); txtSdt.setText(""); txtPass.setText(""); txtConfirm.setText(""); selectedAvatarFile = null; avatarUrl = null; lblAvatarPreview.setIcon(null); lblAvatarPreview.setText("Preview"); }
    private void chooseAvatar() { /* Giữ nguyên */ JFileChooser c = new JFileChooser(); c.setDialogTitle("Chọn Ảnh Đại Diện"); c.setFileFilter(new FileNameExtensionFilter("Ảnh (jpg, png, gif)", "jpg", "png", "gif", "jpeg")); if (c.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) { selectedAvatarFile = c.getSelectedFile(); displayAvatarPreview(selectedAvatarFile); } }
    private void displayAvatarPreview(File file) { /* Giữ nguyên */ ImageIcon i = new ImageIcon(file.getAbsolutePath()); Image img = i.getImage().getScaledInstance(lblAvatarPreview.getPreferredSize().width, lblAvatarPreview.getPreferredSize().height, Image.SCALE_SMOOTH); lblAvatarPreview.setIcon(new ImageIcon(img)); lblAvatarPreview.setText(""); revalidate(); repaint(); }

    private void performRegister() {
        String username = txtUser.getText().trim();
        String sdt = txtSdt.getText().trim();
        String p1 = new String(txtPass.getPassword());
        String p2 = new String(txtConfirm.getPassword());

        if (username.isEmpty() || sdt.isEmpty() || p1.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tên, SĐT và Mật khẩu không được để trống!", "Lỗi", JOptionPane.WARNING_MESSAGE); return;
        }
        if (!p1.equals(p2)) {
            JOptionPane.showMessageDialog(this, "Mật khẩu không khớp!", "Lỗi", JOptionPane.WARNING_MESSAGE); return;
        }

        btnRegister.setEnabled(false); btnRegister.setText("ĐANG XỬ LÝ...");
        final Component parentComponent = this;

        // Nếu có avatar, upload trước
        if (selectedAvatarFile != null) {
            CompletableFuture.supplyAsync(() -> NetworkService.uploadFile(selectedAvatarFile))
             .thenCompose(future -> future)
             .thenAccept(resultUrlObject -> {
                 String url = (resultUrlObject instanceof String) ? (String) resultUrlObject : null;
                 if (url != null && !url.isEmpty()) avatarUrl = url;
                 else SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parentComponent, "Lỗi upload avatar, dùng mặc định."));
                 registerUser(username, sdt, p1);
             }).exceptionally(ex -> {
                 SwingUtilities.invokeLater(() -> { JOptionPane.showMessageDialog(parentComponent, "Lỗi upload avatar: " + ex.getMessage() + ". Dùng mặc định."); registerUser(username, sdt, p1); });
                 return null;
             });
        } else {
            registerUser(username, sdt, p1); // Đăng ký không avatar
        }
    }

    /**
     * ✅ SỬA ĐỔI: Tạo khóa RSA (Lớp 2), mã hóa private key, gọi NetworkService.createUser.
     */
    private void registerUser(String username, String sdt, String password) {
        
        System.out.println("Register: Đang tạo và mã hóa khóa E2EE Lớp 2...");
        
        // --- LOGIC E2EE (LỚP 2) ---
        // 1. TẠO cặp khóa RSA VÀ MÃ HÓA private key bằng password
        Map<String, String> e2eeKeys;
        try {
             e2eeKeys = CryptoService.generateAndEncryptKeys(password);
             if (e2eeKeys == null) throw new Exception("generateAndEncryptKeys trả về null");
        } catch (Exception e) {
             System.err.println("Lỗi nghiêm trọng khi tạo khóa E2EE: " + e.getMessage());
             JOptionPane.showMessageDialog(this, "Lỗi nghiêm trọng: Không thể tạo khóa E2EE.", "Lỗi", JOptionPane.ERROR_MESSAGE);
             btnRegister.setEnabled(true); btnRegister.setText("Đăng ký");
             return;
        }
        
        // 2. Tạo RegisterRequest DTO
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setSdt(sdt);
        request.setPassword(password); // Gửi plaintext, backend sẽ hash (để login)
        request.setAvatar(avatarUrl); 
        
        // Gửi 4 trường E2EE (Backend yêu cầu 4 trường này NOT NULL)
        request.setPublicKey(e2eeKeys.get("publicKey"));
        request.setEncPrivateKey(e2eeKeys.get("encPrivateKey")); // IV + Encrypted Key
        request.setSalt(e2eeKeys.get("salt"));
        request.setIv(e2eeKeys.get("iv")); // Gửi IV riêng
        
        final Component parentComponent = this;
        
        // 3. Gọi API /api/users
        NetworkService.createUser(request)
            .thenAccept(createdUser -> {
                // Đăng ký user thành công
                int newUserId = createdUser.getId();
                System.out.println("Register: Đăng ký user thành công (ID: " + newUserId + ").");

                // 4. Tải Public Key lên API /api/keys
                // (Backend /api/users *nên* lưu public key, nhưng ta gọi 
                // /api/keys để đảm bảo bảng user_keys cũng được cập nhật)
                NetworkService.uploadPublicKey(newUserId, request.getPublicKey())
                        .thenAccept(uploadSuccess -> System.out.println("Register: Upload public key lên /api/keys: " + uploadSuccess));

                // 5. Thông báo thành công
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(parentComponent, "Đăng ký thành công! Vui lòng đăng nhập.", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
                    mainForm.showCard(MainForm.LOGIN_PANEL);
                    clearFields();
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Đăng ký");
                });
            })
            .exceptionally(ex -> {
                // Đăng ký thất bại (ví dụ: SĐT trùng)
                String errorMessage = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                System.err.println("Lỗi đăng ký: " + errorMessage);
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(parentComponent, "Đăng ký thất bại: " + errorMessage, "Lỗi", JOptionPane.ERROR_MESSAGE);
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Đăng ký");
                });
                return null;
            });
    }
}
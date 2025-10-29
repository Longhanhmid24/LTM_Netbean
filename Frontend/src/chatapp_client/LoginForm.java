package chatapp_client;

import model.LoginResponse;
import model.User;
import service.NetworkService;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import service.CryptoService;

/**
 * Handles user login.
 * ✅ ĐÃ CẬP NHẬT: Sử dụng /api/auth/login và tải/giải mã khóa E2EE Lớp 2.
 */
public class LoginForm extends JPanel {

    private JTextField txtUser;
    private JPasswordField txtPass;
    private JButton btnLogin, btnRegister;
    private JLabel lblTitle;
    private MainForm mainForm;

    public LoginForm(MainForm main) { /* (Giữ nguyên UI constructor) */
        this.mainForm = main; setLayout(new BorderLayout()); setBackground(new Color(0, 102, 204)); JPanel cw = new JPanel(new GridBagLayout()); cw.setOpaque(false); JPanel cp = new JPanel(new GridBagLayout()); cp.setBackground(Color.WHITE); cp.setBorder(BorderFactory.createEmptyBorder(20, 30, 30, 30)); GridBagConstraints c = new GridBagConstraints(); c.insets = new Insets(10, 10, 10, 10); c.fill = 1; c.gridx = 0; c.weightx = 1.0; lblTitle = new JLabel("Đăng Nhập", 0); lblTitle.setFont(new Font("Segoe UI", 1, 32)); lblTitle.setForeground(new Color(33, 33, 33)); c.gridy = 0; c.ipady = 10; cp.add(lblTitle, c); txtUser = new JTextField(20); txtUser.setFont(new Font("Segoe UI", 0, 16)); txtUser.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY, 1),"Tên đăng nhập hoặc SĐT", 0, 0, new Font("Segoe UI", 0, 12))); txtUser.setPreferredSize(new Dimension(350, 50)); c.gridy = 1; c.ipady = 0; cp.add(txtUser, c); txtPass = new JPasswordField(20); txtPass.setFont(new Font("Segoe UI", 0, 16)); txtPass.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY, 1),"Mật khẩu", 0, 0, new Font("Segoe UI", 0, 12))); txtPass.setPreferredSize(new Dimension(350, 50)); c.gridy = 2; cp.add(txtPass, c); txtPass.addActionListener(e -> performLogin()); btnLogin = new JButton("Đăng nhập"); btnLogin.setFont(new Font("Segoe UI", 1, 16)); btnLogin.setBackground(new Color(0, 120, 215)); btnLogin.setForeground(Color.WHITE); btnLogin.setFocusPainted(false); btnLogin.setCursor(Cursor.getPredefinedCursor(12)); btnLogin.setPreferredSize(new Dimension(350, 50)); btnLogin.addActionListener(e -> performLogin()); c.gridy = 3; c.insets = new Insets(20, 10, 10, 10); cp.add(btnLogin, c); btnRegister = new JButton("Tạo tài khoản mới"); btnRegister.setFont(new Font("Segoe UI", 1, 14)); btnRegister.setBackground(new Color(76, 175, 80)); btnRegister.setForeground(Color.WHITE); btnRegister.setFocusPainted(false); btnRegister.setCursor(Cursor.getPredefinedCursor(12)); btnRegister.setPreferredSize(new Dimension(350, 45)); btnRegister.addActionListener(e -> mainForm.showCard(MainForm.REGISTER_PANEL)); c.gridy = 4; c.insets = new Insets(10, 10, 10, 10); cp.add(btnRegister, c); cw.add(cp); add(cw, BorderLayout.CENTER);
    }
    
    public void clearFields() { txtUser.setText(""); txtPass.setText(""); }

    /**
     * ✅ SỬA ĐỔI: Sử dụng NetworkService.login() và khởi tạo CryptoService Lớp 2.
     */
    private void performLogin() {
        String usernameOrSdt = txtUser.getText().trim();
        String password = new String(txtPass.getPassword()); // Plaintext

        if (usernameOrSdt.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên đăng nhập/SĐT và mật khẩu!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        final Component parentComponent = this;
        btnLogin.setEnabled(false);
        btnLogin.setText("Đang đăng nhập...");

        // 1. Gọi API đăng nhập
        NetworkService.login(usernameOrSdt, password)
            .thenAccept(loginResponse -> {
                // Đăng nhập thành công (trên luồng async)
                SwingUtilities.invokeLater(() -> {
                    
                    // 2. Khởi tạo/Tải khóa E2EE Lớp 2
                    // (Giải mã Private Key bằng mật khẩu)
                    boolean keysInitialized = CryptoService.initialize(
                        loginResponse.getUserId(),
                        password, // Dùng plaintext password
                        loginResponse.getEncPrivateKey(),
                        loginResponse.getSalt(),
                        loginResponse.getIv()
                    );
                    
                    if (!keysInitialized) {
                         System.err.println("Login: LỖI NGHIÊM TRỌNG! Giải mã Private Key thất bại!");
                         JOptionPane.showMessageDialog(parentComponent, "Lỗi E2EE: Không thể giải mã khóa cá nhân (có thể do sai mật khẩu hoặc dữ liệu hỏng).", "Lỗi E2EE", JOptionPane.ERROR_MESSAGE);
                         btnLogin.setEnabled(true);
                         btnLogin.setText("Đăng nhập");
                         return; // Không tiếp tục
                    }

                    // 3. Tải public key lên server (Để đảm bảo server có key mới nhất)
                    String pubKeyString = CryptoService.publicKeyToString(CryptoService.getPublicKey());
                    if (pubKeyString != null) {
                        NetworkService.uploadPublicKey(loginResponse.getUserId(), pubKeyString)
                            .thenAccept(success -> System.out.println("Login: Public Key Upload (Đồng bộ): " + success));
                    } else {
                        System.err.println("Login: Không thể lấy public key để tải lên.");
                        // (Tiếp tục vì private key đã được tải)
                    }

                    // 4. Chuyển sang màn hình chính
                    mainForm.showMainApp(loginResponse.toUser());
                    clearFields();
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Đăng nhập");
                });
            })
            .exceptionally(ex -> {
                // Xử lý lỗi (ví dụ: 401 Sai mật khẩu)
                String errorMessage = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                System.err.println("Lỗi đăng nhập: " + errorMessage);
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(parentComponent, "Đăng nhập thất bại: " + errorMessage, "Lỗi Đăng Nhập", JOptionPane.ERROR_MESSAGE);
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Đăng nhập");
                });
                return null;
            });
    }
}
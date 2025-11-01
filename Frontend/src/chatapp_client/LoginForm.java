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
 * ✅ ĐÃ CẬP NHẬT: Thêm nút Cài đặt IP (style JButton chuẩn) + Validation IPv4.
 * ✅ ĐÃ CẬP NHẬT: Nút Cài đặt nằm trên nền xanh (panel chứa nút trong suốt).
 * ✅ ĐÃ CẬP NHẬT: Sửa lỗi validation IPv4 (kiểm tra giá trị 0-255).
 */
public class LoginForm extends JPanel {

    private JTextField txtUser;
    private JPasswordField txtPass;
    private JButton btnLogin, btnRegister;
    private JLabel lblTitle;
    private MainForm mainForm;
    private JButton btnSettings; // Nút cài đặt

    public LoginForm(MainForm main) {
        this.mainForm = main;
        setLayout(new BorderLayout());
        setBackground(new Color(0, 102, 204));

        JPanel cw = new JPanel(new GridBagLayout());
        cw.setOpaque(false);
        JPanel cp = new JPanel(new GridBagLayout());
        cp.setBackground(Color.WHITE);
        cp.setBorder(BorderFactory.createEmptyBorder(20, 30, 30, 30));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 10, 10, 10);
        c.fill = 1;
        c.gridx = 0;
        c.weightx = 1.0;
        
        lblTitle = new JLabel("Đăng Nhập", 0);
        lblTitle.setFont(new Font("Segoe UI", 1, 32));
        lblTitle.setForeground(new Color(33, 33, 33));
        c.gridy = 0;
        c.ipady = 10;
        cp.add(lblTitle, c);
        
        txtUser = new JTextField(20);
        txtUser.setFont(new Font("Segoe UI", 0, 16));
        txtUser.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY, 1), "Tên đăng nhập hoặc SĐT", 0, 0, new Font("Segoe UI", 0, 12)));
        txtUser.setPreferredSize(new Dimension(350, 50));
        c.gridy = 1;
        c.ipady = 0;
        cp.add(txtUser, c);
        
        txtPass = new JPasswordField(20);
        txtPass.setFont(new Font("Segoe UI", 0, 16));
        txtPass.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY, 1), "Mật khẩu", 0, 0, new Font("Segoe UI", 0, 12)));
        txtPass.setPreferredSize(new Dimension(350, 50));
        c.gridy = 2;
        cp.add(txtPass, c);
        txtPass.addActionListener(e -> performLogin());
        
        btnLogin = new JButton("Đăng nhập");
        btnLogin.setFont(new Font("Segoe UI", 1, 16));
        btnLogin.setBackground(new Color(0, 120, 215));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setCursor(Cursor.getPredefinedCursor(12));
        btnLogin.setPreferredSize(new Dimension(350, 50));
        btnLogin.addActionListener(e -> performLogin());
        c.gridy = 3;
        c.insets = new Insets(20, 10, 10, 10);
        cp.add(btnLogin, c);
        
        btnRegister = new JButton("Tạo tài khoản mới");
        btnRegister.setFont(new Font("Segoe UI", 1, 14));
        btnRegister.setBackground(new Color(76, 175, 80));
        btnRegister.setForeground(Color.WHITE);
        btnRegister.setFocusPainted(false);
        btnRegister.setCursor(Cursor.getPredefinedCursor(12));
        btnRegister.setPreferredSize(new Dimension(350, 45));
        btnRegister.addActionListener(e -> mainForm.showCard(MainForm.REGISTER_PANEL));
        c.gridy = 4;
        c.insets = new Insets(10, 10, 10, 10);
        cp.add(btnRegister, c);

        cw.add(cp);
        add(cw, BorderLayout.CENTER);

        // ✅ THAY ĐỔI: Panel cho nút cài đặt
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        
        // ✅ THAY ĐỔI: Làm cho panel chứa nút TRONG SUỐT
        southPanel.setOpaque(false); 
        // (Đã xóa setBackground(Color.WHITE) và setBorder(...))

        // Nút "Setting" vẫn là style JButton chuẩn
        btnSettings = new JButton("Setting"); 
        btnSettings.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnSettings.setToolTipText("Thay đổi IP Máy chủ");
        btnSettings.setMargin(new Insets(2, 5, 2, 5));
        btnSettings.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSettings.setFocusPainted(false);
        
        btnSettings.addActionListener(e -> showSettingsDialog()); // Thêm sự kiện
        
        southPanel.add(btnSettings);
        add(southPanel, BorderLayout.SOUTH); // Thêm vào dưới cùng
    }
    
    /**
     * ✅ HÀM MỚI: Hiển thị dialog cài đặt IP (Đã SỬA LỖI Validation)
     */
    private void showSettingsDialog() {
        // 1. Tạo một panel cho nội dung dialog
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel lblInfo = new JLabel("Nhập IP máy chủ (ví dụ: 192.168.1.10):");
        lblInfo.setFont(new Font("Segoe UI", 0, 14));
        panel.add(lblInfo, BorderLayout.NORTH);

        // 2. Lấy IP hiện tại từ NetworkService và đặt vào text field
        String currentIp = NetworkService.getServerIp();
        JTextField ipField = new JTextField(currentIp);
        ipField.setFont(new Font("Segoe UI", 0, 16));
        panel.add(ipField, BorderLayout.CENTER);

        // 3. Hiển thị dialog xác nhận
        int result = JOptionPane.showConfirmDialog(
            mainForm, // Parent
            panel, // Nội dung tùy chỉnh
            "Cài đặt Máy chủ", // Tiêu đề
            JOptionPane.OK_CANCEL_OPTION, // Các nút
            JOptionPane.PLAIN_MESSAGE // Icon
        );

        // 4. Xử lý kết quả
        if (result == JOptionPane.OK_OPTION) {
            String newIp = ipField.getText().trim();
            
            if (newIp.isEmpty()) {
                JOptionPane.showMessageDialog(mainForm, "IP không được để trống.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return; // Dừng lại
            }

            // ✅ THAY ĐỔI: Kiểm tra IPv4 bằng cách parse giá trị
            if (!isValidIPv4(newIp)) {
                JOptionPane.showMessageDialog(mainForm, 
                        "Địa chỉ IP không hợp lệ! Mỗi phần phải là số từ 0 đến 255.", 
                        "Lỗi Định Dạng", 
                        JOptionPane.WARNING_MESSAGE);
                return; // Dừng lại
            }
            
            // Chỉ cập nhật nếu IP thực sự thay đổi
            if (!newIp.equals(currentIp)) {
                // Gọi hàm setServerIp trong NetworkService
                NetworkService.setServerIp(newIp);
                JOptionPane.showMessageDialog(mainForm, "Đã cập nhật IP máy chủ thành: " + newIp, "Thành công", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    /**
     * ✅ HÀM HELPER MỚI: Kiểm tra giá trị IPv4
     */
    private boolean isValidIPv4(String ip) {
        String[] octets = ip.split("\\."); // Phải escape dấu chấm
        
        // Phải có đúng 4 phần
        if (octets.length != 4) {
            return false;
        }

        try {
            for (String octet : octets) {
                // Kiểm tra xem có phải là số hay không
                int value = Integer.parseInt(octet);
                
                // Kiểm tra giá trị 0-255
                if (value < 0 || value > 255) {
                    return false;
                }
                
                // Kiểm tra trường hợp như "01" (không hợp lệ nếu > 1 chữ số và bắt đầu bằng 0)
                if (octet.length() > 1 && octet.startsWith("0")) {
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            // Nếu parse lỗi (ví dụ: "192.168.1.abc")
            return false;
        }
        
        // Vượt qua tất cả kiểm tra
        return true;
    }

    public void clearFields() {
        txtUser.setText("");
        txtPass.setText("");
    }

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
                    System.err.println("Login: LỖI NGHIÊM TRONG! Giải mã Private Key thất bại!");
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
            
            // ✅ FIX: Lấy error message an toàn, tránh null
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            String errorMessage = cause.getMessage();
            
            // ✅ Kiểm tra null trước khi sử dụng
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "Lỗi không xác định: " + cause.getClass().getSimpleName();
            }
            
            final String finalErrorMessage = errorMessage; // Để dùng trong lambda
            System.err.println("Lỗi đăng nhập: " + finalErrorMessage);
            
            SwingUtilities.invokeLater(() -> {
                // ✅ Hiển thị lỗi thân thiện hơn
                String displayError = finalErrorMessage;
                
                if (finalErrorMessage.contains("Connection refused") || finalErrorMessage.contains("timed out")) {
                    displayError = "Không thể kết nối đến máy chủ. Vui lòng kiểm tra IP và tường lửa.";
                } else if (finalErrorMessage.contains("401")) {
                    displayError = "Tên đăng nhập hoặc mật khẩu không đúng.";
                } else if (finalErrorMessage.contains("UnknownHostException")) {
                    displayError = "Không thể tìm thấy máy chủ. Vui lòng kiểm tra địa chỉ IP.";
                } else if (finalErrorMessage.contains("SocketTimeoutException")) {
                    displayError = "Kết nối bị timeout. Máy chủ không phản hồi.";
                }
            
                JOptionPane.showMessageDialog(parentComponent, 
                    "Đăng nhập thất bại: " + displayError, 
                    "Lỗi Đăng Nhập", 
                    JOptionPane.ERROR_MESSAGE);
                btnLogin.setEnabled(true);
                btnLogin.setText("Đăng nhập");
            });
            return null;
        });
}
}
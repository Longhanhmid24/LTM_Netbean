package chatapp_client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 * Panel hiển thị khi không có cuộc trò chuyện nào được chọn.
 */
public class WelcomePanel extends JPanel {

    public WelcomePanel(String username) {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE); // Nền trắng
        setBorder(new EmptyBorder(100, 100, 100, 100)); // Padding lớn

        // Nội dung chào mừng dùng HTML
        String welcomeText = String.format(
            "<html><div style='text-align: center; line-height: 1.5;'>" +
            "<h1 style='font-size: 24px; font-weight: bold;'>Chào mừng, %s!</h1><br>" +
            "<p style='color: #555555; font-size: 14px;'>" +
            "Hãy chọn một người bạn từ danh sách bên trái<br>để bắt đầu trò chuyện." +
            "</p><br><br>" +
            "<p style='color: #888888; font-size: 11px;'>" +
            "Nhấp chuột vào tên một người dùng để mở cửa sổ chat." +
            "</p>" +
            "</div></html>",
            (username != null ? username : "User") // Đề phòng user null
        );

        JLabel lblWelcome = new JLabel(welcomeText);
        lblWelcome.setFont(new Font("Segoe UI", Font.PLAIN, 16)); // Font cơ bản
        lblWelcome.setHorizontalAlignment(SwingConstants.CENTER);
        lblWelcome.setVerticalAlignment(SwingConstants.CENTER);

        // Thêm vào giữa
        add(lblWelcome, BorderLayout.CENTER);
    }
}
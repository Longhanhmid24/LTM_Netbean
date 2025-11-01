package chatapp_client;

import service.NetworkService;
import javax.imageio.ImageIO; // Import
import javax.swing.SwingUtilities; // Import
import javax.swing.UIManager; // Import

/**
 * Điểm khởi chạy chính cho ứng dụng Chat client.
 */
public class ChatApp {

    /**
     * @param args tham số dòng lệnh
     */
    public static void main(String[] args) {
        
        // Debug: Kiểm tra các định dạng ảnh ImageIO hỗ trợ
        System.out.println("---- Các Định dạng Ảnh ImageIO Hỗ trợ ----");
        String[] readerFormats = ImageIO.getReaderFormatNames();
        boolean webpFound = false;
        for (String format : readerFormats) {
            System.out.println("- " + format);
            if (format.equalsIgnoreCase("webp")) {
                webpFound = true;
            }
        }
        if (webpFound) {
            System.out.println("OK: Đã tìm thấy bộ đọc WebP (có thể do TwelveMonkeys).");
        } else {
            System.err.println("CẢNH BÁO: Không tìm thấy bộ đọc WebP. Ảnh .webp có thể sẽ không hiển thị.");
        }
        System.out.println("------------------------------------------");


        // Cài đặt Look and Feel (Giao diện)
        try {
            boolean nimbusFound = false;
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    nimbusFound = true;
                    break;
                }
            }
            if (!nimbusFound) {
                 UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
        } catch (Exception ex) {
            System.err.println("Không thể cài đặt Look and Feel: " + ex.getMessage());
        }

        // Khởi tạo NetworkService (kích hoạt các static mappers)
        NetworkService.init();
        
        // Chạy giao diện chính trên luồng Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new MainForm().setVisible(true));
    }
}

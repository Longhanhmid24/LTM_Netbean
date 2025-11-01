package chatapp_client;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 * Một JPanel tùy chỉnh có thể vẽ nền với các góc bo tròn.
 */
public class RoundedBubblePanel extends JPanel {
    private int arcWidth;
    private int arcHeight;

    /**
     * Tạo một panel với các góc bo tròn.
     * @param arcWidth Độ rộng của cung tròn ở góc.
     * @param arcHeight Độ cao của cung tròn ở góc.
     */
    public RoundedBubblePanel(int arcWidth, int arcHeight) {
        this.arcWidth = arcWidth;
        this.arcHeight = arcHeight;
        setOpaque(false); // Quan trọng: Phải tắt Opaque của JPanel
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Gọi hàm cha

        Graphics2D g2 = (Graphics2D) g.create(); // Tạo bản sao

        // Bật Anti-aliasing để góc bo mượt mà
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Lấy màu nền đã được set (setBackground)
        Color bgColor = getBackground();
        g2.setColor(bgColor);

        // Vẽ hình chữ nhật bo tròn với màu nền
        // Kích thước là toàn bộ component, trừ 1 pixel để tránh tràn viền
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arcWidth, arcHeight);

        g2.dispose(); // Giải phóng Graphics2D
    }
}
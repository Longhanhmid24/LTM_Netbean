package chatapp_client;

import model.User;
import model.Group; // ✅ IMPORT MỚI
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Class Renderer tùy chỉnh để hiển thị User VÀ Group trong JList.
 */
public class UserListRenderer extends JPanel implements ListCellRenderer<Object> { // ✅ Sửa thành ListCellRenderer<Object>

    private JLabel lblAvatar;
    private JLabel lblName; // Đổi tên từ lblUsername
    private JLabel lblInfo;
    private JPanel textPanel;

    private Color textSelectionColor = Color.WHITE;
    private Color backgroundSelectionColor = new Color(0, 120, 215);
    private Color backgroundNonSelectionColor = new Color(249, 249, 249);

    // Cache cho avatar (dùng chung cho cả user và group)
    private static final Map<String, Icon> avatarCache = new ConcurrentHashMap<>();

    public UserListRenderer() {
        setLayout(new BorderLayout(10, 0));
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        setOpaque(true);

        lblAvatar = new JLabel();
        lblAvatar.setPreferredSize(new Dimension(48, 48));
        lblAvatar.setHorizontalAlignment(SwingConstants.CENTER);
        lblAvatar.setVerticalAlignment(SwingConstants.CENTER);

        textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        textPanel.setOpaque(false);

        lblName = new JLabel();
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 15));

        lblInfo = new JLabel();
        lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblInfo.setForeground(Color.GRAY);

        textPanel.add(lblName);
        textPanel.add(lblInfo);

        add(lblAvatar, BorderLayout.WEST);
        add(textPanel, BorderLayout.CENTER);
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, // ✅ Sửa thành Object value
                                               int index, boolean isSelected, boolean cellHasFocus) {

        if (backgroundNonSelectionColor == null) {
            backgroundNonSelectionColor = list.getBackground();
        }
        
        String name = "N/A";
        String info = "";
        String avatarUrl = null;

        // --- ✅ LOGIC MỚI: Kiểm tra loại đối tượng ---
        if (value instanceof User) {
            User user = (User) value;
            name = user.getUsername();
            info = user.getSdt() != null ? user.getSdt() : "Người dùng";
            avatarUrl = user.getAvatar();
        } else if (value instanceof Group) {
            Group group = (Group) value;
            name = group.getName();
            info = "Nhóm chat"; // Thông tin cho nhóm
            avatarUrl = group.getAvatar();
        }
        // --- Hết logic mới ---

        lblName.setText(name);
        lblInfo.setText(info);

        // --- Cập nhật Avatar ---
        String cacheKey = (avatarUrl != null && !avatarUrl.isEmpty()) ? avatarUrl : name; // Dùng name làm cache key

        if (avatarCache.containsKey(cacheKey)) {
            lblAvatar.setIcon(avatarCache.get(cacheKey));
        } else {
            Icon placeholder = createLetterAvatar(name, 48);
            lblAvatar.setIcon(placeholder);
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                loadAvatarAsync(list, avatarUrl, cacheKey, 48);
            } else {
                avatarCache.put(cacheKey, placeholder);
            }
        }

        // --- Xử lý màu khi được chọn ---
        if (isSelected) {
            setBackground(backgroundSelectionColor);
            lblName.setForeground(textSelectionColor);
            lblInfo.setForeground(Color.LIGHT_GRAY);
        } else {
            setBackground(backgroundNonSelectionColor);
            lblName.setForeground(Color.BLACK);
            lblInfo.setForeground(Color.GRAY);
        }

        return this;
    }

    /**
     * Tải avatar (dùng chung cho User và Group)
     */
    private void loadAvatarAsync(JList<?> list, String url, String cacheKey, int size) {
        CompletableFuture.supplyAsync(() -> {
            try {
                URL imageUrl = new URL(url.replace(" ", "%20"));
                ImageIcon icon = new ImageIcon(imageUrl);
                while (icon.getImageLoadStatus() == MediaTracker.LOADING) {
                    Thread.sleep(20);
                }
                if (icon.getImageLoadStatus() == MediaTracker.COMPLETE && icon.getIconWidth() > 0) {
                    Image scaledImg = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
                    return new ImageIcon(scaledImg);
                }
                return null;
            } catch (Exception e) {
                System.err.println("Không thể tải avatar: " + url + " - " + e.getMessage());
                return null;
            }
        }).thenAccept(icon -> {
            if (icon != null) {
                avatarCache.put(cacheKey, icon);
                SwingUtilities.invokeLater(list::repaint);
            }
        });
    }

    /**
     * Tạo avatar bằng chữ cái đầu (dùng chung cho User và Group)
     */
    private Icon createLetterAvatar(String name, int size) {
        if (name == null || name.isEmpty()) name = "?";
        String firstLetter = name.substring(0, 1).toUpperCase();
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color color = new Color(Math.abs(name.hashCode()) % 16777216);
        color = new Color( (color.getRed() + 255) / 2, (color.getGreen() + 255) / 2, (color.getBlue() + 255) / 2 );
        g2.setColor(color);
        g2.fillOval(0, 0, size, size);
        g2.setColor(color.darker().darker());
        g2.setFont(new Font("Segoe UI", Font.BOLD, size / 2 + 2));
        FontMetrics fm = g2.getFontMetrics();
        int x = (size - fm.stringWidth(firstLetter)) / 2;
        int y = (size - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(firstLetter, x, y);
        g2.dispose();
        return new ImageIcon(image);
    }
}
package chatapp_client;

import model.Group; // Import model Group
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.*;

/**
 * Class Renderer tùy chỉnh để hiển thị Group trong JList.
 */
public class GroupListRenderer extends JPanel implements ListCellRenderer<Group> {

    private JLabel lblAvatar;
    private JLabel lblGroupName;
    private JLabel lblInfo; // Ví dụ: số thành viên (chưa làm)
    private JPanel textPanel;

    // Màu sắc
    private Color textSelectionColor = Color.WHITE;
    private Color backgroundSelectionColor = new Color(0, 120, 215); // Xanh đậm
    private Color backgroundNonSelectionColor = new Color(249, 249, 249); // Nền xám nhạt

    // Cache cho avatar nhóm
    private static final Map<String, Icon> avatarCache = new ConcurrentHashMap<>();

    public GroupListRenderer() {
        setLayout(new BorderLayout(10, 0));
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        setOpaque(true);

        lblAvatar = new JLabel();
        lblAvatar.setPreferredSize(new Dimension(48, 48));
        lblAvatar.setHorizontalAlignment(SwingConstants.CENTER);
        lblAvatar.setVerticalAlignment(SwingConstants.CENTER);

        textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        textPanel.setOpaque(false);

        lblGroupName = new JLabel();
        lblGroupName.setFont(new Font("Segoe UI", Font.BOLD, 15));

        lblInfo = new JLabel(); // Placeholder
        lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblInfo.setForeground(Color.GRAY);

        textPanel.add(lblGroupName);
        textPanel.add(lblInfo);

        add(lblAvatar, BorderLayout.WEST);
        add(textPanel, BorderLayout.CENTER);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Group> list, Group group,
                                               int index, boolean isSelected, boolean cellHasFocus) {

        if (backgroundNonSelectionColor == null) {
            backgroundNonSelectionColor = list.getBackground();
        }

        if (group == null) {
            lblGroupName.setText("N/A"); lblInfo.setText(""); lblAvatar.setIcon(null);
            return this;
        }

        // --- Cập nhật Text ---
        lblGroupName.setText(group.getName());
        lblInfo.setText("Nhóm chat"); // Thông tin tạm thời

        // --- Cập nhật Avatar ---
        String avatarUrl = group.getAvatar();
        // Dùng tên nhóm làm key nếu không có URL (để tạo avatar chữ cái)
        String cacheKey = (avatarUrl != null && !avatarUrl.isEmpty()) ? avatarUrl : group.getName();

        if (avatarCache.containsKey(cacheKey)) {
            lblAvatar.setIcon(avatarCache.get(cacheKey));
        } else {
            Icon placeholder = createLetterAvatar(group.getName(), 48);
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
            lblGroupName.setForeground(textSelectionColor);
            lblInfo.setForeground(Color.LIGHT_GRAY);
        } else {
            setBackground(backgroundNonSelectionColor);
            lblGroupName.setForeground(Color.BLACK);
            lblInfo.setForeground(Color.GRAY);
        }

        return this;
    }

    // --- Các hàm helper (tương tự UserListRenderer) ---
    private void loadAvatarAsync(JList<? extends Group> list, String url, String cacheKey, int size) {
        // ... (Giống UserListRenderer.loadAvatarAsync) ...
        CompletableFuture.supplyAsync(() -> {
            try {
                URL imageUrl = new URL(url.replace(" ", "%20")); ImageIcon icon = new ImageIcon(imageUrl);
                while (icon.getImageLoadStatus() == MediaTracker.LOADING) Thread.sleep(20);
                if (icon.getImageLoadStatus() == MediaTracker.COMPLETE && icon.getIconWidth() > 0) {
                    Image scaled = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH); return new ImageIcon(scaled);
                } return null;
            } catch (Exception e) { return null; }
        }).thenAccept(icon -> { if (icon != null) { avatarCache.put(cacheKey, icon); SwingUtilities.invokeLater(list::repaint); } });
    }
    private Icon createLetterAvatar(String groupName, int size) {
        // ... (Giống UserListRenderer.createLetterAvatar) ...
        if (groupName == null || groupName.isEmpty()) groupName = "#"; String letter = groupName.substring(0, 1).toUpperCase();
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB); Graphics2D g2 = img.createGraphics(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color c = new Color(Math.abs(groupName.hashCode()) % 16777216); c = new Color((c.getRed()+255)/2, (c.getGreen()+255)/2, (c.getBlue()+255)/2); g2.setColor(c);
        g2.fillOval(0, 0, size, size); g2.setColor(c.darker().darker()); g2.setFont(new Font("Segoe UI", Font.BOLD, size/2+2)); FontMetrics fm = g2.getFontMetrics();
        int x = (size-fm.stringWidth(letter))/2, y = (size-fm.getHeight())/2 + fm.getAscent(); g2.drawString(letter, x, y); g2.dispose(); return new ImageIcon(img);
    }
}
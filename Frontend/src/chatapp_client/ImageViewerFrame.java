package chatapp_client;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import service.NetworkService;

/**
 * Một JFrame tùy chỉnh để hiển thị ảnh từ URL với nút Download và zoom (nút +/-, cuộn chuột).
 * Ảnh tự động fit vào viewbox (JScrollPane) khi không zoom, cập nhật khi cửa sổ thay đổi kích thước.
 * Zoom tập trung vào vị trí con trỏ chuột.
 * Fallback mở trình duyệt nếu định dạng ảnh không được hỗ trợ.
 */
public class ImageViewerFrame extends JFrame {

    private JLabel imageLabel;
    private JScrollPane scrollPane;
    private JLabel loadingLabel;
    private JPanel bottomPanel;
    private JButton btnDownload;
    private JButton btnZoomIn;
    private JButton btnZoomOut;
    private JLabel zoomLabel;
    private String imageUrl;
    private Image originalImage; // Lưu ảnh gốc để resize
    private double zoomLevel = 1.0; // Tỷ lệ zoom mặc định (100%)
    private static final double ZOOM_STEP = 0.1; // Bước zoom (10%)
    private static final double MIN_ZOOM = 0.1; // Zoom tối thiểu (10%)
    private static final double MAX_ZOOM = 5.0; // Zoom tối đa (500%)

    public ImageViewerFrame(String imageUrl) {
        this.imageUrl = imageUrl;

        setTitle("Xem Ảnh");
        setSize(750, 600); // Kích thước mặc định
        setMinimumSize(new Dimension(400, 300)); // Kích thước tối thiểu
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.DARK_GRAY);

        // Hiển thị "Đang tải..."
        loadingLabel = new JLabel("Đang tải ảnh...", SwingConstants.CENTER);
        loadingLabel.setForeground(Color.WHITE);
        loadingLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        add(loadingLabel, BorderLayout.CENTER);

        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);

        scrollPane = new JScrollPane(imageLabel);
        scrollPane.getViewport().setBackground(Color.DARK_GRAY);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        // Thêm MouseWheelListener cho zoom
        scrollPane.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                Point mousePoint = e.getPoint();
                int notches = e.getWheelRotation();
                double newZoomLevel = zoomLevel + (notches < 0 ? ZOOM_STEP : -ZOOM_STEP);
                zoomImage(newZoomLevel, mousePoint);
            }
        });

        // Bottom panel với các nút và label zoom
        bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(Color.DARK_GRAY);

        // Nút Zoom In
        btnZoomIn = createStyledButton("+", new Color(0, 122, 255));
        btnZoomIn.setToolTipText("Phóng to");
        btnZoomIn.addActionListener(e -> zoomImage(zoomLevel + ZOOM_STEP, null));

        // Nút Zoom Out
        btnZoomOut = createStyledButton("-", new Color(0, 122, 255));
        btnZoomOut.setToolTipText("Thu nhỏ");
        btnZoomOut.addActionListener(e -> zoomImage(zoomLevel - ZOOM_STEP, null));

        // Label hiển thị tỷ lệ zoom
        zoomLabel = new JLabel("Zoom: 100%");
        zoomLabel.setForeground(Color.WHITE);
        zoomLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Nút Download
        btnDownload = createStyledButton("Tải về", new Color(76, 175, 80));
        btnDownload.addActionListener(e -> downloadImage());

        bottomPanel.add(zoomLabel);
        bottomPanel.add(Box.createHorizontalStrut(10));
        bottomPanel.add(btnZoomIn);
        bottomPanel.add(btnZoomOut);
        bottomPanel.add(Box.createHorizontalStrut(10));
        bottomPanel.add(btnDownload);
        add(bottomPanel, BorderLayout.SOUTH);

        // Thêm ComponentListener để resize ảnh khi cửa sổ thay đổi kích thước
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (originalImage != null) {
                    updateImageDisplay();
                }
            }
        });

        // Tải ảnh bất đồng bộ
        loadImage(imageUrl);
    }

    private void loadImage(String url) {
        CompletableFuture.supplyAsync(() -> {
            HttpClient c = HttpClient.newHttpClient();
            String eU = NetworkService.encodeUrlPath(url);
            if (eU == null) return CompletableFuture.failedFuture(new IllegalArgumentException("URL lỗi"));

            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(eU)).timeout(Duration.ofSeconds(30)).GET().build();
            try {
                HttpResponse<byte[]> r = c.send(req, HttpResponse.BodyHandlers.ofByteArray());
                return r.statusCode() == 200 ? r.body() : CompletableFuture.failedFuture(new IOException("HTTP " + r.statusCode()));
            } catch (Exception ex) {
                return CompletableFuture.failedFuture(ex);
            }
        })
        .thenCompose(r -> r instanceof CompletableFuture ? (CompletableFuture<byte[]>) r : CompletableFuture.completedFuture((byte[]) r))
        .thenAccept(bytes -> {
            if (bytes == null || bytes.length == 0) throw new CompletionException(new IOException("Ảnh rỗng"));

            try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
                originalImage = ImageIO.read(bis); // Thử đọc bằng ImageIO
            } catch (IOException ex) {
                originalImage = new ImageIcon(bytes).getImage(); // Fallback sang ImageIcon
            }

            if (originalImage == null || originalImage.getWidth(null) <= 0) {
                SwingUtilities.invokeLater(() -> {
                    System.err.println("Không thể hiển thị định dạng ảnh. Mở trình duyệt...");
                    openFileInBrowser(url);
                    dispose();
                });
                return;
            }

            SwingUtilities.invokeLater(() -> {
                remove(loadingLabel);
                add(scrollPane, BorderLayout.CENTER);
                scrollPane.revalidate();
                updateImageDisplay();
                validate();
                repaint();
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                loadingLabel.setText("<html><font color='red'>Tải ảnh thất bại:</font><br>" + ex.getMessage() + "</html>");
                btnDownload.setEnabled(false);
                btnZoomIn.setEnabled(false);
                btnZoomOut.setEnabled(false);
            });
            return null;
        });
    }

    private void updateImageDisplay() {
        if (originalImage == null) return;

        // Lấy kích thước viewport của scrollPane
        Dimension viewportSize = scrollPane.getViewport().getSize();
        if (viewportSize.width == 0 || viewportSize.height == 0) {
            viewportSize = getContentPane().getSize();
        }
        int maxWidth = viewportSize.width - 20; // Padding
        int maxHeight = viewportSize.height - 20;

        // Tính kích thước ảnh dựa trên zoomLevel
        int imgWidth = originalImage.getWidth(null);
        int imgHeight = originalImage.getHeight(null);
        int scaledWidth = (int) (imgWidth * zoomLevel);
        int scaledHeight = (int) (imgHeight * zoomLevel);

        // Giữ tỷ lệ khung hình khi fit vào viewport (chỉ khi zoomLevel = 1.0)
        if (zoomLevel == 1.0) {
            double aspectRatio = (double) imgWidth / imgHeight;
            if (scaledWidth > maxWidth) {
                scaledWidth = maxWidth;
                scaledHeight = (int) (scaledWidth / aspectRatio);
            }
            if (scaledHeight > maxHeight) {
                scaledHeight = maxHeight;
                scaledWidth = (int) (scaledHeight * aspectRatio);
            }
        }

        // Đảm bảo kích thước tối thiểu
        scaledWidth = Math.max(1, scaledWidth);
        scaledHeight = Math.max(1, scaledHeight);

        // Tạo ảnh đã scale
        Image scaledImage = originalImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
        imageLabel.setIcon(new ImageIcon(scaledImage));

        // Căn giữa ảnh
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);

        // Cập nhật label zoom
        zoomLabel.setText(String.format("Zoom: %.0f%%", zoomLevel * 100));

        // Vô hiệu hóa nút zoom nếu chạm giới hạn
        btnZoomIn.setEnabled(zoomLevel < MAX_ZOOM);
        btnZoomOut.setEnabled(zoomLevel > MIN_ZOOM);

        // Đảm bảo imageLabel có kích thước phù hợp
        imageLabel.setPreferredSize(new Dimension(scaledWidth, scaledHeight));
        scrollPane.revalidate();
        validate();
        repaint();
    }

    private void zoomImage(double newZoomLevel, Point mousePoint) {
        // Giới hạn zoom
        newZoomLevel = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoomLevel));
        if (newZoomLevel == zoomLevel) return;

        // Lưu vị trí viewport và con trỏ chuột
        Point viewPosition = scrollPane.getViewport().getViewPosition();
        Dimension viewSize = scrollPane.getViewport().getSize();

        // Tính vị trí con trỏ tương đối với ảnh
        double relativeX = mousePoint != null ? mousePoint.x + viewPosition.x : viewSize.width / 2.0;
        double relativeY = mousePoint != null ? mousePoint.y + viewPosition.y : viewSize.height / 2.0;

        // Tính tỷ lệ zoom thay đổi
        double zoomRatio = newZoomLevel / zoomLevel;

        // Cập nhật zoomLevel
        zoomLevel = newZoomLevel;

        // Tính vị trí viewport mới để giữ con trỏ ở cùng vị trí trên ảnh
        int newViewX = (int) (relativeX * zoomRatio - viewSize.width / 2.0);
        int newViewY = (int) (relativeY * zoomRatio - viewSize.height / 2.0);

        // Cập nhật ảnh
        updateImageDisplay();

        // Điều chỉnh viewport để zoom vào vị trí con trỏ
        Dimension newImageSize = new Dimension(
            (int) (originalImage.getWidth(null) * zoomLevel),
            (int) (originalImage.getHeight(null) * zoomLevel)
        );
        newViewX = Math.max(0, Math.min(newViewX, newImageSize.width - viewSize.width));
        newViewY = Math.max(0, Math.min(newViewY, newImageSize.height - viewSize.height));
        scrollPane.getViewport().setViewPosition(new Point(newViewX, newViewY));
    }

    private void openFileInBrowser(String url) {
        if (url == null || url.trim().isEmpty()) return;
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) return;
        String eU = NetworkService.encodeUrlPath(url);
        if (eU == null) return;
        try {
            Desktop.getDesktop().browse(new URI(eU));
        } catch (Exception ex) {
            System.err.println("Lỗi mở link: " + ex.getMessage());
        }
    }

    private void downloadImage() {
        JFileChooser saveChooser = new JFileChooser();
        saveChooser.setDialogTitle("Lưu Ảnh");

        // Gợi ý tên file từ URL
        String defaultName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
        saveChooser.setSelectedFile(new File(defaultName));

        if (saveChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = saveChooser.getSelectedFile();
            btnDownload.setEnabled(false);
            btnDownload.setText("Đang tải...");

            new Thread(() -> {
                try {
                    HttpClient client = HttpClient.newHttpClient();
                    String eU = NetworkService.encodeUrlPath(imageUrl);
                    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(eU)).GET().build();

                    HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(
                        fileToSave.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
                    );

                    SwingUtilities.invokeLater(() -> {
                        if (response.statusCode() == 200) {
                            JOptionPane.showMessageDialog(this, "Tải về thành công!\nLưu tại: " + fileToSave.getAbsolutePath(), "Tải Xong", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(this, "Tải về thất bại. (HTTP Status: " + response.statusCode() + ")", "Lỗi Mạng", JOptionPane.ERROR_MESSAGE);
                        }
                        btnDownload.setEnabled(true);
                        btnDownload.setText("Tải về");
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Lỗi khi tải file: " + ex.getMessage(), "Lỗi I/O", JOptionPane.ERROR_MESSAGE);
                        btnDownload.setEnabled(true);
                        btnDownload.setText("Tải về");
                    });
                }
            }).start();
        }
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            final Color o = bg;
            final Color d = bg.darker();
            @Override
            public void mouseEntered(MouseEvent e) { b.setBackground(d); }
            @Override
            public void mouseExited(MouseEvent e) { b.setBackground(o); }
        });
        return b;
    }
}
package chatapp_client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import service.NetworkService; // Cần để encode URL

/**
 * Một JFrame tùy chỉnh để hiển thị ảnh từ URL với nút Download.
 * Sẽ fallback (mở trình duyệt) nếu định dạng ảnh không được Swing hỗ trợ.
 */
public class ImageViewerFrame extends JFrame {

    private JLabel imageLabel;
    private JScrollPane scrollPane;
    private JLabel loadingLabel;
    private JPanel bottomPanel;
    private JButton btnDownload;
    private String imageUrl; // Lưu URL để download

    public ImageViewerFrame(String imageUrl) {
        this.imageUrl = imageUrl;
        
        setTitle("Xem Ảnh");
        setSize(800, 600);
        setMinimumSize(new Dimension(800, 600));
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
        
        // Nút Download
        btnDownload = new JButton("Tải về");
        btnDownload.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnDownload.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnDownload.addActionListener(e -> downloadImage());
        
        bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(Color.DARK_GRAY);
        bottomPanel.add(btnDownload);
        add(bottomPanel, BorderLayout.SOUTH);

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
            
            Image i = null;
            try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
                i = ImageIO.read(bis); // Thử đọc bằng ImageIO
            } catch (IOException ex) {}
            
            // Nếu ImageIO thất bại (ví dụ: webp không có thư viện),
            // thử tải bằng ImageIcon (hỗ trợ một số định dạng khác)
            if (i == null) {
                i = new ImageIcon(bytes).getImage();
            }

            // Nếu cả hai đều thất bại -> Mở Browser
            if (i == null || i.getWidth(null) <= 0) {
                 SwingUtilities.invokeLater(() -> {
                    System.err.println("Không thể hiển thị định dạng ảnh. Mở trình duyệt...");
                    openFileInBrowser(url);
                    dispose(); // Đóng frame này
                 });
                 return;
            }
            
            final ImageIcon fullSizeIcon = new ImageIcon(i);
            
            SwingUtilities.invokeLater(() -> {
                remove(loadingLabel);
                add(scrollPane, BorderLayout.CENTER);
                imageLabel.setIcon(fullSizeIcon);
                
                int imgWidth = fullSizeIcon.getIconWidth();
                int imgHeight = fullSizeIcon.getIconHeight();
                
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                int maxWidth = (int)(screenSize.width * 0.8);
                int maxHeight = (int)(screenSize.height * 0.8);

                if (imgWidth > maxWidth || imgHeight > maxHeight) {
                    double widthRatio = (double) maxWidth / imgWidth;
                    double heightRatio = (double) maxHeight / imgHeight;
                    double ratio = Math.min(widthRatio, heightRatio);
                    int newWidth = (int) (imgWidth * ratio);
                    int newHeight = (int) (imgHeight * ratio);
                    setSize(newWidth + 50, newHeight + 50);
                } else {
                     setSize(imgWidth + 50, imgHeight + 50);
                }
                setLocationRelativeTo(null);
                validate();
                repaint();
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                loadingLabel.setText("<html><font color='red'>Tải ảnh thất bại:</font><br>" + ex.getMessage() + "</html>");
                btnDownload.setEnabled(false);
            });
            return null;
        });
    }
    
    // Mở file trong trình duyệt (Fallback)
    private void openFileInBrowser(String url) {
        if(url==null||url.trim().isEmpty())return; 
        if(!Desktop.isDesktopSupported()||!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))return; 
        String eU=NetworkService.encodeUrlPath(url); 
        if(eU==null)return; 
        try{Desktop.getDesktop().browse(new URI(eU));}catch(Exception ex){}
    }
    
    // Xử lý nút Download
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

            // Tải file trên luồng riêng
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
}
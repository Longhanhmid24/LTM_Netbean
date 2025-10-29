package chatapp_client;

import service.NetworkService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionListener; // Import
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.BorderFactory;
import java.util.Optional;
import java.util.List;
import javax.swing.JFileChooser; // Import
import java.nio.file.Files; // Import
import java.nio.file.Path; // Import
import java.nio.file.StandardOpenOption; // Import

// ✅ THÊM CÁC IMPORT THIẾU BẮT BUỘC
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletionException;

// Imports VLCj
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;


public class MediaPlayerFrame extends JFrame {

    private EmbeddedMediaPlayerComponent mediaPlayerComponent;

    private final String mediaUrl;
    private final String mediaTitle;
    private final boolean isAudioOnly;

    private static boolean vlcFound = false;
    private static boolean isCheckingVlc = false;

    // Các nút Swing Tùy chỉnh
    private JButton btnPlayPause;
    private JButton btnStop;
    private JSlider timeSlider;
    private JLabel lblTime;
    private JLabel audioOnlyBanner; // Banner

    // ✅ Nút Download
    private JButton btnDownload;


    /**
     * Kiểm tra cài đặt VLC.
     */
    public static boolean checkVlcInstallation() {
        if (vlcFound || isCheckingVlc) return vlcFound;
        isCheckingVlc = true;
        System.out.println("Đang kiểm tra cài đặt VLC...");
        try {
            vlcFound = new NativeDiscovery().discover();
            if (vlcFound) System.out.println("OK: Đã tìm thấy thư viện VLC.");
            else System.err.println("LỖI: Không tìm thấy thư viện VLC Player!");
        } catch (Exception e) {
            System.err.println("Lỗi khi kiểm tra VLC: " + e.getMessage()); vlcFound = false;
        }
        isCheckingVlc = false;
        return vlcFound;
    }

    /**
     * Constructor mới nhận thêm cờ isAudio.
     */
    public MediaPlayerFrame(String title, String mediaUrl, boolean isAudio) {
        this.mediaUrl = mediaUrl;
        this.mediaTitle = (title != null ? title : "Media");
        this.isAudioOnly = isAudio;

        setTitle("Đang tải: " + mediaTitle);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBackground(Color.BLACK);

        if (!checkVlcInstallation()) {
            openFileExternal(mediaUrl, mediaTitle);
            mediaPlayerComponent = null;
            return;
        }

        // 2. Khởi tạo Component Cốt lõi
        try {
            mediaPlayerComponent = new EmbeddedMediaPlayerComponent();

            // Khởi tạo Audio Only Banner
            audioOnlyBanner = new JLabel("CHỈ AUDIO (AUDIO ONLY)", SwingConstants.CENTER);
            audioOnlyBanner.setFont(new Font("Segoe UI", Font.BOLD, 30));
            audioOnlyBanner.setForeground(Color.YELLOW);
            audioOnlyBanner.setBackground(new Color(0, 0, 0, 180));
            audioOnlyBanner.setOpaque(true);
            audioOnlyBanner.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        } catch (Exception | Error e) {
            System.err.println("Lỗi nghiêm trọng khi khởi tạo EmbeddedMediaPlayerComponent: " + e.getMessage());
            e.printStackTrace();
            openFileExternal(mediaUrl, mediaTitle); // Fallback khi khởi tạo lỗi
            mediaPlayerComponent = null;
            return;
        }

        // 3. Setup Controls và Listeners
        JPanel controlsPanel = createCustomControls();

        // --- CẤU TRÚC LAYOUT VÀ ẨN/HIỆN ---
        JPanel mediaPanel = new JPanel(new BorderLayout());
        mediaPanel.setBackground(Color.BLACK);
        mediaPanel.setOpaque(true);
        mediaPanel.add(mediaPlayerComponent, BorderLayout.CENTER);

        // Nếu là Audio Only, ẩn Player và hiển thị Banner thay thế
        if (isAudioOnly) {
            mediaPlayerComponent.setVisible(false); // Ẩn bề mặt video
            mediaPanel.add(audioOnlyBanner, BorderLayout.CENTER); // Đặt banner vào giữa
        } else {
            audioOnlyBanner.setVisible(false); // Đảm bảo ẩn banner nếu là video
        }

        contentPane.add(mediaPanel, BorderLayout.CENTER);
        contentPane.add(controlsPanel, BorderLayout.SOUTH);

        setContentPane(contentPane);

        // 4. Thêm WindowListener
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (mediaPlayerComponent != null) {
                    try {
                        mediaPlayerComponent.mediaPlayer().controls().stop();
                        mediaPlayerComponent.release();
                    } catch (Exception ex) {
                        System.err.println("Lỗi khi dừng media player: " + ex.getMessage());
                    }
                }
            }
        });

        // GỌI HÀM SETUP LISTENER SAU CÙNG
        setupControlListeners();
        setupMediaListeners();
    }

    /**
     * Xây dựng bảng điều khiển bằng Swing tiêu chuẩn.
     */
    private JPanel createCustomControls() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(new Color(40, 40, 40));

        // 1. Khởi tạo các Control
        btnPlayPause = new JButton("Play"); // ▶ Play
        btnStop = new JButton("Stop"); // ■ Stop
        lblTime = new JLabel("00:00 / 00:00");
        timeSlider = new JSlider(0, 1000, 0);
        // Khởi tạo nút Download
        btnDownload = new JButton("Tải về");
        btnDownload.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnDownload.setBackground(new Color(100, 149, 237));
        btnDownload.setForeground(Color.WHITE);
        btnDownload.setFocusPainted(false);

        // 2. Cấu hình Control
        btnPlayPause.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnStop.setFont(new Font("Segoe UI", Font.BOLD, 14));

        lblTime.setFont(new Font("Consolas", Font.BOLD, 14));
        lblTime.setForeground(Color.WHITE);
        lblTime.setPreferredSize(new Dimension(120, 25));
        lblTime.setHorizontalAlignment(SwingConstants.LEFT);
        lblTime.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        timeSlider.setOpaque(false);
        timeSlider.setForeground(Color.LIGHT_GRAY);
        timeSlider.setToolTipText("Thanh tua");

        // 3. Sắp xếp Layout
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        buttonPanel.setOpaque(false);
        buttonPanel.add(btnPlayPause);
        buttonPanel.add(btnStop);

        // Panel Download (phía Đông)
        JPanel downloadPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        downloadPanel.setOpaque(false);
        downloadPanel.add(btnDownload);

        JPanel timeControlPanel = new JPanel(new BorderLayout(10, 0));
        timeControlPanel.setOpaque(false);
        timeControlPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15)); // Padding phải

        timeControlPanel.add(lblTime, BorderLayout.WEST);
        timeControlPanel.add(timeSlider, BorderLayout.CENTER);

        panel.add(buttonPanel, BorderLayout.WEST);
        panel.add(timeControlPanel, BorderLayout.CENTER);
        panel.add(downloadPanel, BorderLayout.EAST); // Thêm nút download vào EAST

        return panel;
    }


    /**
     * Thiết lập các sự kiện cho nút và slider (chỉ logic Swing).
     */
    private void setupControlListeners() {
        // --- Sự kiện Nút Play/Pause ---
        ActionListener playPauseAction = e -> {
            if (mediaPlayerComponent.mediaPlayer().status().isPlaying()) {
                mediaPlayerComponent.mediaPlayer().controls().pause();
            } else {
                mediaPlayerComponent.mediaPlayer().controls().play();
            }
        };

        btnPlayPause.addActionListener(playPauseAction); // Gắn action vào nút

        // Gắn action vào bề mặt media component (Mouse Click)
        mediaPlayerComponent.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    playPauseAction.actionPerformed(null); // Kích hoạt action Play/Pause
                }
            }
        });

        btnStop.addActionListener(e -> {
            mediaPlayerComponent.mediaPlayer().controls().stop();
            btnPlayPause.setText("Play"); // ▶ Play
            timeSlider.setValue(0);
            lblTime.setText(formatTime(0) + " / " + formatTime(mediaPlayerComponent.mediaPlayer().status().length()));
        });

        // --- Sự kiện Thanh trượt ---
        timeSlider.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {}
            @Override
            public void mouseReleased(MouseEvent e) {
                mediaPlayerComponent.mediaPlayer().controls().setPosition((float) timeSlider.getValue() / 1000f);
            }
        });

        // ✅ LOGIC NÚT DOWNLOAD
        btnDownload.addActionListener(e -> {
            downloadMediaFile(mediaUrl, mediaTitle);
        });
    }


    /**
     * Thiết lập adapter nghe sự kiện media (logic VLCj).
     */
    private void setupMediaListeners() {
        mediaPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {

            @Override
            public void mediaChanged(MediaPlayer mp, uk.co.caprica.vlcj.media.MediaRef mr) {
                SwingUtilities.invokeLater(() -> {
                    // Banner đã được setVisible(isAudioOnly) trong constructor
                    // Không cần logic phức tạp ở đây
                });
            }

            @Override
            public void timeChanged(MediaPlayer mp, long newTime) {
                if (!timeSlider.getValueIsAdjusting()) {
                    float position = mp.status().position();
                    long totalTime = mp.status().length();
                    int sliderValue = (int) (position * 1000f);
                    timeSlider.setValue(sliderValue);
                    SwingUtilities.invokeLater(() -> lblTime.setText(formatTime(newTime) + " / " + formatTime(totalTime)));
                }
            }

            @Override
            public void lengthChanged(MediaPlayer mp, long newLength) {
                SwingUtilities.invokeLater(() -> {
                    timeSlider.setValue(0);
                    lblTime.setText(formatTime(0) + " / " + formatTime(newLength));
                });
            }

            @Override
            public void finished(MediaPlayer mp) {
                SwingUtilities.invokeLater(() -> {
                    btnPlayPause.setText("Play"); // ▶ Play
                    timeSlider.setValue(1000);
                });
            }

            @Override
            public void playing(MediaPlayer mp) {
                SwingUtilities.invokeLater(() -> btnPlayPause.setText("Pause")); // ⏸ Pause
            }

            @Override
            public void paused(MediaPlayer mp) {
                SwingUtilities.invokeLater(() -> btnPlayPause.setText("Play")); // ▶ Play
            }

            @Override
            public void stopped(MediaPlayer mp) {
                SwingUtilities.invokeLater(() -> btnPlayPause.setText("Play")); // ▶ Play
            }
        });
    }

    /**
     * Chuyển đổi mili giây thành chuỗi thời gian (mm:ss nếu < 1 giờ, HH:mm:ss nếu >= 1 giờ).
     */
    private String formatTime(long ms) {
        if (ms <= 0) return "00:00";

        long totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms);

        if (totalSeconds < 3600) { // Nếu dưới 1 giờ (3600 giây)
            long seconds = totalSeconds % 60;
            long minutes = totalSeconds / 60;
            return String.format("%02d:%02d", minutes, seconds);
        } else {
            long seconds = totalSeconds % 60;
            long minutes = (totalSeconds / 60) % 60;
            long hours = totalSeconds / 3600;
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
    }

    /**
     * Ghi đè setVisible để bắt đầu phát khi frame hiển thị
     */
    @Override
    public void setVisible(boolean visible) {
        if (mediaPlayerComponent == null) {
            super.setVisible(false);
            return;
        }

        super.setVisible(visible);

        if (visible) {
            playMedia();
        }
    }

    /**
     * Bắt đầu phát media.
     */
    private void playMedia() {
        if (mediaPlayerComponent == null) { return; }

        // Gọi NetworkService để mã hóa URL
        String encodedUrl = service.NetworkService.encodeUrlPath(mediaUrl);

        if (encodedUrl != null) {
            System.out.println("VLCj: Đang thử phát: " + encodedUrl);
            setTitle("Đang phát: " + mediaTitle);

            boolean success = mediaPlayerComponent.mediaPlayer().media().play(encodedUrl);

            if (!success) {
                System.err.println("VLCj: Lệnh play() trả về false (Không tìm thấy file hoặc lỗi codec).");
                JOptionPane.showMessageDialog(this, "Không thể bắt đầu phát file media này.", "Lỗi Phát", JOptionPane.ERROR_MESSAGE);
                dispose(); // Đóng cửa sổ nếu không phát được
            } else {
                btnPlayPause.setText("⏸ Pause");
            }
        } else {
            System.err.println("VLCj: URL không hợp lệ, không thể phát: " + mediaUrl);
            JOptionPane.showMessageDialog(this, "URL media không hợp lệ.", "Lỗi Phát", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    /**
     * Hàm mở ứng dụng ngoài (cần thiết cho Fallback)
     */
    private void openFileExternal(String url, String title) {
        String encodedUrl = service.NetworkService.encodeUrlPath(url);
        if (encodedUrl != null) {
            try {
                System.out.println("Mở trình phát ngoài cho: " + encodedUrl);
                Desktop.getDesktop().browse(new URI(encodedUrl));
                JOptionPane.showMessageDialog(null, "Đã mở trình phát media mặc định cho file:\n" + title, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException | URISyntaxException ex) {
                System.err.println("Lỗi khi mở trình phát ngoài: " + ex.getMessage());
                JOptionPane.showMessageDialog(null, "Không thể mở file media bên ngoài.", "Lỗi Phát", JOptionPane.ERROR_MESSAGE);
            }
        }
        // Gọi dispose frame trống
        SwingUtilities.invokeLater(this::dispose);
    }

    /**
     * ✅ TẠO HÀM MỚI: Xử lý logic tải file media về máy cục bộ.
     */
    private void downloadMediaFile(String url, String fileName) {
        // Cần lấy NetworkService.encodeUrlPath()
        String encodedUrl = service.NetworkService.encodeUrlPath(url);
        if (encodedUrl == null) {
            JOptionPane.showMessageDialog(this, "URL không hợp lệ, không thể tải.", "Lỗi Tải", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser saveChooser = new JFileChooser();
        saveChooser.setDialogTitle("Lưu Tệp Media");
        // Đặt tên file gợi ý (loại bỏ tiền tố timestamp nếu có)
        String suggestedName = fileName;
        if (suggestedName.matches("^\\d{14,}_.*")) {
            suggestedName = suggestedName.substring(suggestedName.indexOf('_') + 1);
           }
        saveChooser.setSelectedFile(new File(suggestedName));

        int userSelection = saveChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = saveChooser.getSelectedFile();

            // Chạy việc tải file trên luồng riêng để không treo giao diện
            new Thread(() -> {
                try {
                    // Dùng HttpClient để tải file
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(encodedUrl))
                            .GET()
                            .build();

                    // Tải file trực tiếp vào đường dẫn đã chọn
                    HttpResponse<Path> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofFile(fileToSave.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));

                    SwingUtilities.invokeLater(() -> {
                        if (response.statusCode() == 200) {
                            JOptionPane.showMessageDialog(this, "Tải về thành công!\nLưu tại: " + fileToSave.getAbsolutePath(), "Tải Xong", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            // Xóa file lỗi (nếu có)
                            try { Files.deleteIfExists(fileToSave.toPath()); } catch(IOException ex) {}
                            JOptionPane.showMessageDialog(this, "Tải về thất bại. (HTTP Status: " + response.statusCode() + ")", "Lỗi Mạng", JOptionPane.ERROR_MESSAGE);
                        }
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Lỗi khi tải file: " + ex.getMessage(), "Lỗi I/O", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
        }
    }
}
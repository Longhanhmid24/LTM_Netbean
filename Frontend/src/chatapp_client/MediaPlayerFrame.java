package chatapp_client;

import service.NetworkService;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

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
    private JButton btnRewind; // << Tua lại
    private JButton btnForward; // >> Tua nhanh
    private JSlider timeSlider;
    private JSlider volumeSlider; // Thanh âm lượng
    private JLabel lblTime;
    private JLabel audioOnlyBanner;
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
            System.err.println("Lỗi khi kiểm tra VLC: " + e.getMessage());
            vlcFound = false;
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

        setTitle("Đang kiểm tra VLC...");
        setSize(800, 600);
        setMinimumSize(new Dimension(400, 300));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Hiển thị loading panel trong khi kiểm tra VLC
        JLabel loadingLabel = new JLabel("Đang kiểm tra VLC...", SwingConstants.CENTER);
        loadingLabel.setForeground(Color.WHITE);
        loadingLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        loadingLabel.setBackground(Color.BLACK);
        loadingLabel.setOpaque(true);
        setContentPane(loadingLabel);

        // Kiểm tra VLC
        if (!checkVlcInstallation()) {
            openFileExternal(mediaUrl, mediaTitle, "Không tìm thấy VLC. Đã mở trình duyệt.");
            mediaPlayerComponent = null;
            return;
        }

        // Khởi tạo Component Cốt lõi
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
            openFileExternal(mediaUrl, mediaTitle, "Không thể khởi tạo trình phát. Đã mở trình duyệt.");
            mediaPlayerComponent = null;
            return;
        }

        // Setup Controls và Layout
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBackground(Color.BLACK);

        JPanel controlsPanel = createCustomControls();
        JPanel mediaPanel = new JPanel(new BorderLayout());
        mediaPanel.setBackground(Color.BLACK);
        mediaPanel.setOpaque(true);
        mediaPanel.add(mediaPlayerComponent, BorderLayout.CENTER);

        if (isAudioOnly) {
            mediaPlayerComponent.setVisible(false);
            mediaPanel.add(audioOnlyBanner, BorderLayout.CENTER);
        } else {
            audioOnlyBanner.setVisible(false);
        }

        contentPane.add(mediaPanel, BorderLayout.CENTER);
        contentPane.add(controlsPanel, BorderLayout.SOUTH);

        setContentPane(contentPane);

        // Thêm WindowListener để phát media khi cửa sổ mở
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                playMedia();
            }

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

        setupControlListeners();
        setupMediaListeners();
    }

    /**
     * Xây dựng bảng điều khiển bằng Swing tiêu chuẩn.
     */
    private JPanel createCustomControls() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(new Color(40, 40, 40));

        // Khởi tạo các Control
        btnPlayPause = new JButton("Play");
        btnStop = new JButton("Stop");
        btnRewind = new JButton("<<"); // Tua lại 5s
        btnForward = new JButton(">>"); // Tua nhanh 5s
        lblTime = new JLabel("00:00 / 00:00");
        timeSlider = new JSlider(0, 1000, 0);
        volumeSlider = new JSlider(0, 100, 50); // Âm lượng từ 0-100, mặc định 50
        btnDownload = new JButton("Tải về");

        // Cấu hình Control
        btnPlayPause.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnStop.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnRewind.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnForward.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnDownload.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnDownload.setBackground(new Color(100, 149, 237));
        btnDownload.setForeground(Color.WHITE);
        btnDownload.setFocusPainted(false);

        lblTime.setFont(new Font("Consolas", Font.BOLD, 14));
        lblTime.setForeground(Color.WHITE);
        lblTime.setPreferredSize(new Dimension(120, 25));
        lblTime.setHorizontalAlignment(SwingConstants.LEFT);
        lblTime.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        timeSlider.setOpaque(false);
        timeSlider.setForeground(Color.LIGHT_GRAY);
        timeSlider.setToolTipText("Thanh tua");

        volumeSlider.setOpaque(false);
        volumeSlider.setForeground(Color.LIGHT_GRAY);
        volumeSlider.setToolTipText("Điều chỉnh âm lượng");
        volumeSlider.setPreferredSize(new Dimension(100, 25));

        // Sắp xếp Layout
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        buttonPanel.setOpaque(false);
        buttonPanel.add(btnRewind);
        buttonPanel.add(btnPlayPause);
        buttonPanel.add(btnStop);
        buttonPanel.add(btnForward);

        JPanel downloadPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        downloadPanel.setOpaque(false);
        // ✅ SỬA: Đặt màu chữ trắng cho JLabel "Âm lượng:"
        JLabel volumeLabel = new JLabel("Volume:");
        volumeLabel.setForeground(Color.WHITE);
        downloadPanel.add(volumeLabel);
        downloadPanel.add(volumeSlider);
        downloadPanel.add(btnDownload);

        JPanel timeControlPanel = new JPanel(new BorderLayout(10, 0));
        timeControlPanel.setOpaque(false);
        timeControlPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15));
        timeControlPanel.add(lblTime, BorderLayout.WEST);
        timeControlPanel.add(timeSlider, BorderLayout.CENTER);

        panel.add(buttonPanel, BorderLayout.WEST);
        panel.add(timeControlPanel, BorderLayout.CENTER);
        panel.add(downloadPanel, BorderLayout.EAST);

        return panel;
    }

    /**
     * Thiết lập các sự kiện cho nút và slider.
     */
    private void setupControlListeners() {
        if (mediaPlayerComponent == null) return;

        // Sự kiện Play/Pause
        ActionListener playPauseAction = e -> {
            if (mediaPlayerComponent.mediaPlayer().status().isPlaying()) {
                mediaPlayerComponent.mediaPlayer().controls().pause();
            } else {
                mediaPlayerComponent.mediaPlayer().controls().play();
            }
        };

        btnPlayPause.addActionListener(playPauseAction);
        mediaPlayerComponent.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    playPauseAction.actionPerformed(null);
                }
            }
        });

        // Sự kiện Stop
        btnStop.addActionListener(e -> {
            mediaPlayerComponent.mediaPlayer().controls().stop();
            btnPlayPause.setText("Play");
            timeSlider.setValue(0);
            lblTime.setText("00:00 / " + formatTime(mediaPlayerComponent.mediaPlayer().status().length()));
        });

        // Sự kiện Tua lại 5s
        btnRewind.addActionListener(e -> {
            mediaPlayerComponent.mediaPlayer().controls().skipTime(-5000); // Tua lại 5 giây
        });

        // Sự kiện Tua nhanh 5s
        btnForward.addActionListener(e -> {
            mediaPlayerComponent.mediaPlayer().controls().skipTime(5000); // Tua nhanh 5 giây
        });

        // Sự kiện Thanh trượt thời gian
        timeSlider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                mediaPlayerComponent.mediaPlayer().controls().setPosition((float) timeSlider.getValue() / 1000f);
            }
        });

        // Sự kiện Thanh trượt âm lượng
        volumeSlider.addChangeListener(e -> {
            mediaPlayerComponent.mediaPlayer().audio().setVolume(volumeSlider.getValue());
        });

        // Sự kiện Download
        btnDownload.addActionListener(e -> downloadMediaFile(mediaUrl, mediaTitle));
    }

    /**
     * Thiết lập adapter nghe sự kiện media.
     */
    private void setupMediaListeners() {
        if (mediaPlayerComponent == null) return;
        mediaPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void mediaChanged(MediaPlayer mp, uk.co.caprica.vlcj.media.MediaRef mr) {
                SwingUtilities.invokeLater(() -> {
                    // Không cần logic phức tạp
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
                    lblTime.setText("00:00 / " + formatTime(newLength));
                });
            }

            @Override
            public void finished(MediaPlayer mp) {
                SwingUtilities.invokeLater(() -> {
                    btnPlayPause.setText("Play");
                    timeSlider.setValue(1000);
                });
            }

            @Override
            public void playing(MediaPlayer mp) {
                SwingUtilities.invokeLater(() -> btnPlayPause.setText("Pause"));
            }

            @Override
            public void paused(MediaPlayer mp) {
                SwingUtilities.invokeLater(() -> btnPlayPause.setText("Play"));
            }

            @Override
            public void stopped(MediaPlayer mp) {
                SwingUtilities.invokeLater(() -> btnPlayPause.setText("Play"));
            }

            @Override
            public void error(MediaPlayer mp) {
                SwingUtilities.invokeLater(() -> {
                    System.err.println("VLCj: Lỗi phát media!");
                    openFileExternal(mediaUrl, mediaTitle, "Không thể phát file media: Lỗi codec hoặc file không hợp lệ.");
                });
            }
        });
    }

    /**
     * Chuyển đổi mili giây thành chuỗi thời gian.
     */
    private String formatTime(long ms) {
        if (ms <= 0) return "00:00";
        long totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms);
        if (totalSeconds < 3600) {
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
     * Bắt đầu phát media.
     */
    private void playMedia() {
        if (mediaPlayerComponent == null) {
            openFileExternal(mediaUrl, mediaTitle, "Không thể khởi tạo trình phát media.");
            return;
        }

        String encodedUrl = NetworkService.encodeUrlPath(mediaUrl);
        if (encodedUrl != null) {
            System.out.println("VLCj: Đang thử phát: " + encodedUrl);
            setTitle("Đang phát: " + mediaTitle);
            try {
                boolean success = mediaPlayerComponent.mediaPlayer().media().play(encodedUrl);
                if (!success) {
                    System.err.println("VLCj: Lệnh play() trả về false (Không tìm thấy file hoặc lỗi codec).");
                    openFileExternal(mediaUrl, mediaTitle, "Không thể phát file media: Lỗi codec hoặc file không hợp lệ.");
                } else {
                    btnPlayPause.setText("Pause");
                    mediaPlayerComponent.mediaPlayer().audio().setVolume(volumeSlider.getValue());
                }
            } catch (Exception e) {
                System.err.println("VLCj: Lỗi khi gọi play(): " + e.getMessage());
                openFileExternal(mediaUrl, mediaTitle, "Không thể phát file media: " + e.getMessage());
            }
        } else {
            System.err.println("VLCj: URL không hợp lệ, không thể phát: " + mediaUrl);
            openFileExternal(mediaUrl, mediaTitle, "URL media không hợp lệ.");
        }
    }

    /**
     * Hàm mở ứng dụng ngoài (cần thiết cho Fallback).
     */
    private void openFileExternal(String url, String title, String errorMessage) {
        String encodedUrl = NetworkService.encodeUrlPath(url);
        if (encodedUrl != null) {
            try {
                System.out.println("Mở trình phát ngoài cho: " + encodedUrl);
                Desktop.getDesktop().browse(new URI(encodedUrl));
                JOptionPane.showMessageDialog(null, errorMessage + "\nĐã mở trình phát media mặc định.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            } catch (IOException | URISyntaxException ex) {
                System.err.println("Lỗi khi mở trình phát ngoài: " + ex.getMessage());
                JOptionPane.showMessageDialog(null, "Không thể mở file media bên ngoài.", "Lỗi Phát", JOptionPane.ERROR_MESSAGE);
            }
        }
        SwingUtilities.invokeLater(this::dispose);
    }

    /**
     * Xử lý logic tải file media về máy cục bộ.
     */
    private void downloadMediaFile(String url, String fileName) {
        String encodedUrl = NetworkService.encodeUrlPath(url);
        if (encodedUrl == null) {
            JOptionPane.showMessageDialog(this, "URL không hợp lệ, không thể tải.", "Lỗi Tải", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser saveChooser = new JFileChooser();
        saveChooser.setDialogTitle("Lưu Tệp Media");
        String suggestedName = fileName.matches("^\\d{14,}_.*") ? fileName.substring(fileName.indexOf('_') + 1) : fileName;
        saveChooser.setSelectedFile(new File(suggestedName));

        if (saveChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = saveChooser.getSelectedFile();
            btnDownload.setEnabled(false);
            btnDownload.setText("Đang tải...");

            new Thread(() -> {
                try {
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(encodedUrl)).GET().build();
                    HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(fileToSave.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
                    SwingUtilities.invokeLater(() -> {
                        if (response.statusCode() == 200) {
                            JOptionPane.showMessageDialog(this, "Tải về thành công!\nLưu tại: " + fileToSave.getAbsolutePath(), "Tải Xong", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            try {
                                Files.deleteIfExists(fileToSave.toPath());
                            } catch (IOException ex) {}
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
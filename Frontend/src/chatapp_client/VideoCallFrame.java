package chatapp_client;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*; // Import Timer từ swing
import javax.swing.border.EmptyBorder;

class VideoCallFrame extends JFrame {
    private JLabel lblTimer;
    private Timer timer; // Đây là javax.swing.Timer
    private int seconds = 0;

    public VideoCallFrame(String username) {
        setTitle("Cuộc gọi Video với " + username);
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Color.BLACK);
        setLayout(new BorderLayout(10, 10));

        JLabel lblAvatar = new JLabel(" " + username, SwingConstants.CENTER);
        lblAvatar.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblAvatar.setForeground(Color.WHITE);
        lblAvatar.setBorder(new EmptyBorder(30, 0, 10, 0));

        lblTimer = new JLabel("00:00", SwingConstants.CENTER);
        lblTimer.setFont(new Font("Consolas", Font.BOLD, 26));
        lblTimer.setForeground(Color.GREEN);

        JButton btnEnd = new JButton("Kết thúc");
        btnEnd.setFont(new Font("Segoe UI", Font.BOLD, 22));
        btnEnd.setBackground(Color.RED);
        btnEnd.setForeground(Color.WHITE);
        btnEnd.setFocusPainted(false);
        btnEnd.addActionListener(e -> {
            if (timer != null) timer.stop();
            dispose(); // Đóng frame này
        });

        add(lblAvatar, BorderLayout.NORTH);
        add(lblTimer, BorderLayout.CENTER);
        add(btnEnd, BorderLayout.SOUTH);

        startTimer();
        playRingtone();
    }

    private void startTimer() {
        // Dùng javax.swing.Timer, nó chạy trên EDT
        timer = new Timer(1000, e -> { // 1000ms delay
            seconds++;
            int m = seconds / 60;
            int s = seconds % 60;
            lblTimer.setText(String.format("%02d:%02d", m, s));
        });
        timer.start();
    }

    // Chuông giả lập (chạy trên luồng riêng)
    private void playRingtone() {
        new Thread(() -> {
            try {
                for (int i = 0; i < 3; i++) {
                    Toolkit.getDefaultToolkit().beep();
                    Thread.sleep(800);
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt(); // Khôi phục trạng thái interrupt
            }
        }).start();
    }
}
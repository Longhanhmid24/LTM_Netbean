package chatapp_client;

import model.ChatMessage;
import model.MessageSendDTO;
import service.NetworkService;
import service.KeyService;
import service.CryptoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.*;
import javax.crypto.SecretKey;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.*;
import org.java_websocket.client.WebSocketClient;
import java.net.http.*;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.security.PublicKey;
import java.nio.file.StandardOpenOption;

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
    
// ✅ Constructor dành cho NGƯỜI NHẬN CUỘC GỌI (receiver)
public VideoCallFrame(int peerId, String callType, Long callId, WebSocketClient ws, int currentUserId, boolean isReceiver) {
    this.peerId = peerId;
    this.callType = callType;
    this.callId = callId;
    this.ws = ws;
    this.currentUserId = currentUserId;

    setTitle("Cuộc gọi đến từ " + peerId + " (" + callType + ")");
    setSize(500, 400);
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setLocationRelativeTo(null);
    getContentPane().setBackground(Color.BLACK);
    setLayout(new BorderLayout(10, 10));

    JLabel lblInfo = new JLabel("Đang kết nối...", SwingConstants.CENTER);
    lblInfo.setFont(new Font("Segoe UI", Font.BOLD, 26));
    lblInfo.setForeground(Color.WHITE);

    lblTimer = new JLabel("00:00", SwingConstants.CENTER);
    lblTimer.setFont(new Font("Consolas", Font.BOLD, 26));
    lblTimer.setForeground(Color.GREEN);

    JButton btnEnd = new JButton("Từ chối");
    btnEnd.setFont(new Font("Segoe UI", Font.BOLD, 22));
    btnEnd.setBackground(Color.RED);
    btnEnd.setForeground(Color.WHITE);
    btnEnd.addActionListener(e -> endCall());

    add(lblInfo, BorderLayout.NORTH);
    add(lblTimer, BorderLayout.CENTER);
    add(btnEnd, BorderLayout.SOUTH);

    startTimer();
    // ❌ người nhận không tự beep lại — caller đã beep rồi
}


    // ✅ thêm các biến
        private int peerId;
        private String callType;
        private long callId;
        private WebSocketClient ws;
        private int currentUserId;
        
    private void sendCallSignal(String type) {
    String frame = """
    SEND
    destination:/app/call.send
    content-type:application/json

    {"type":"%s","callerId":%d,"receiverId":%d,"callType":"%s"}\0
    """.formatted(type, currentUserId, peerId, callType);

    ws.send(frame);
}
    
    private void endCall() {
    String frame = """
    SEND
    destination:/app/call.send
    content-type:application/json

    {"type":"hangup","callerId":%d,"receiverId":%d}\0
    """.formatted(currentUserId, peerId);

    ws.send(frame);

    NetworkService.endCall(callId);
    if (timer != null) timer.stop();
    dispose();
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
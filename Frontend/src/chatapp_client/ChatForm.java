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

/**
 * Panel chat 1:1.
 * ✅ ĐÃ NÂNG CẤP: Sử dụng E2EE Lớp 2 (Mã hóa từng tin nhắn).
 * ✅ ĐÃ CẬP NHẬT: Mở ảnh bằng ImageViewerFrame và thêm nút Download.
 */
public class ChatForm extends JPanel {

    private JPanel chatPanel; private JScrollPane scrollPane; private JTextField txtMsg; private JButton btnSend; private JButton btnFile; private JProgressBar progressBar;
    private int currentUserId;
    private int friendId;
    private WebSocketClient ws;
    private JButton btnCallAudio;
    private JButton btnCallVideo;
    
    public static final String DECRYPT_ERROR_PLACEHOLDER = "[Decryption Error]";

    public ChatForm(int userId, int friendId, String username, WebSocketClient sharedWsClient) {
        this.currentUserId = userId;
        this.friendId = friendId;
        this.ws = sharedWsClient;
        initializeChatComponents();
        setupChatLayout();
        setupChatActions();
        loadHistory(); // Tải lịch sử E2EE Lớp 2
    }

    // --- UI Init, Layout, Actions ---
    private void initializeChatComponents() {
        setBackground(Color.WHITE);
        chatPanel = new JPanel() {
            public boolean getScrollableTracksViewportWidth() { return true; }
            public boolean getScrollableTracksViewportHeight() { return false; }
        };
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(Color.WHITE);
        chatPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        scrollPane = new JScrollPane(chatPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setTransferHandler(createFileDropHandler());
        txtMsg = new JTextField();
        txtMsg.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        txtMsg.setBorder(BorderFactory.createCompoundBorder( BorderFactory.createLineBorder(new Color(210, 210, 210), 1, true), BorderFactory.createEmptyBorder(8, 10, 8, 10) ));
        txtMsg.setToolTipText("Enter message and press Enter");
        btnSend = createStyledButton("Send", new Color(0, 122, 255));
        btnFile = createStyledButton("File", new Color(76, 175, 80));
        btnCallAudio = createStyledButton("Call", new Color(52, 152, 219)); // Blue
        btnCallVideo = createStyledButton("Video", new Color(231, 76, 60)); // Red
        progressBar = new JProgressBar(); progressBar.setVisible(false); progressBar.setStringPainted(true); progressBar.setForeground(new Color(0, 122, 255)); progressBar.setBackground(new Color(230, 230, 230));
    }
    
    private void setupChatLayout() { 
        setLayout(new BorderLayout(5, 5)); 
        JPanel bottomPanel = new JPanel(new BorderLayout(8, 5)); 
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10)); 
        bottomPanel.setBackground(new Color(245, 248, 250)); 
        JPanel inputControls = new JPanel(new BorderLayout(5, 0)); 
        inputControls.setOpaque(false); 
        inputControls.add(txtMsg, BorderLayout.CENTER); 
        inputControls.add(btnSend, BorderLayout.EAST); 
        bottomPanel.add(btnFile, BorderLayout.WEST); 
        bottomPanel.add(inputControls, BorderLayout.CENTER); 
        // ✅ thêm panel chứa 2 nút gọi
        JPanel callPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        callPanel.setOpaque(false);
        callPanel.add(btnCallAudio);
        callPanel.add(btnCallVideo);

        bottomPanel.add(callPanel, BorderLayout.EAST);
        JPanel southPanel = new JPanel(); 
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS)); 
        southPanel.add(progressBar); 
        southPanel.add(bottomPanel); 
        add(scrollPane, BorderLayout.CENTER); 
        add(southPanel, BorderLayout.SOUTH); 
    }
    
    private void setupChatActions() { 
        btnSend.addActionListener(e -> sendMessage()); 
        txtMsg.addActionListener(e -> sendMessage()); 
        btnFile.addActionListener(e -> chooseFile()); 
        btnCallAudio.addActionListener(e -> startCall("audio"));
        btnCallVideo.addActionListener(e -> startCall("video"));

    }
    
    private TransferHandler createFileDropHandler() { 
        return new TransferHandler() { 
            @Override public boolean canImport(TransferHandler.TransferSupport support) { return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor); } 
            @Override public boolean importData(TransferHandler.TransferSupport support) { 
                if (!canImport(support)) return false; 
                Transferable t = support.getTransferable(); 
                try { 
                    java.util.List<File> files = (java.util.List<File>) t.getTransferData(DataFlavor.javaFileListFlavor); 
                    if (files.isEmpty()) return false; 
                    new Thread(() -> { 
                        files.stream().filter(File::isFile).forEach(file -> { 
                            uploadAndSendFile(file); 
                            try { Thread.sleep(100); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); } 
                        }); 
                    }).start(); 
                    return true; 
                } catch (Exception ex) { return false; } 
            } 
        }; 
    }
    
    private JButton createStyledButton(String text, Color bg){ 
        JButton b = new JButton(text); 
        b.setFont(new Font("Segoe UI", Font.BOLD, 14)); 
        b.setBackground(bg); b.setForeground(Color.WHITE); 
        b.setFocusPainted(false); b.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15)); 
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); 
        b.addMouseListener(new MouseAdapter() { 
            final Color o = bg; final Color d = bg.darker(); 
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(d); } 
            @Override public void mouseExited(MouseEvent e) { b.setBackground(o); } 
        }); 
        return b; 
    }

    public void disposeWebSocket() { 
        System.out.println("ChatForm disposeWebSocket called (no action needed)."); 
    }
    
    private void chooseFile() { 
        JFileChooser c = new JFileChooser(); 
        c.setDialogTitle("Select File"); 
        c.setFileFilter(new FileNameExtensionFilter("All Supported","jpg","png","gif","jpeg","webp","bmp","mp4","mov","avi","wmv","mkv","mp3","wav","ogg","aac","m4a","txt","zip","pdf","docx","xlsx","pptx")); 
        c.setAcceptAllFileFilterUsed(true); 
        if (c.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) { 
            File f = c.getSelectedFile(); 
            if (f != null && f.exists() && f.isFile()) uploadAndSendFile(f); 
            else JOptionPane.showMessageDialog(this, "Invalid File.", "Error", 2); 
        } 
    }
    
    private String detectFileType(File file) { 
        String n=file.getName().toLowerCase(); 
        if(n.matches(".*\\.(jpg|jpeg|png|gif|webp|bmp)$"))return "image"; 
        if(n.matches(".*\\.(mp4|mov|avi|wmv|mkv|flv|webm)$"))return "video"; 
        if(n.matches(".*\\.(mp3|wav|ogg|aac|m4a)$"))return "audio"; 
        return "file"; 
    }
    
    private void scrollToBottom() { 
        SwingUtilities.invokeLater(() -> { 
            JScrollBar v = scrollPane.getVerticalScrollBar(); 
            if (v != null) SwingUtilities.invokeLater(() -> v.setValue(v.getMaximum())); 
        }); 
    }

    
    // --- Sending Logic (E2EE Lớp 2) ---

    private void sendMessage() {
        String messageText = txtMsg.getText().trim();
        if (!messageText.isEmpty()) {
            encryptAndSendMessage("text", messageText, null, null);
            txtMsg.setText("");
            txtMsg.requestFocusInWindow();
        }
    }
    
    private void uploadAndSendFile(File file) {
        long maxSize = 100 * 1024 * 1024; if (file.length() > maxSize) { JOptionPane.showMessageDialog(this, "File too large.", "Error", 2); return; }
        SwingUtilities.invokeLater(() -> { progressBar.setValue(0); progressBar.setVisible(true); btnSend.setEnabled(false); btnFile.setEnabled(false); });
        
        final String originalFileType = detectFileType(file);
        final String originalFileName = file.getName();
        final Component parent = this;

        NetworkService.uploadFile(file)
            .thenAccept(finalUrl -> {
                SwingUtilities.invokeLater(() -> { progressBar.setVisible(false); btnSend.setEnabled(true); btnFile.setEnabled(true); });
                if (finalUrl != null && !finalUrl.isEmpty()) {
                    encryptAndSendMessage(originalFileType, null, finalUrl, originalFileName);
                } else {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, "Upload failed: No URL.", "Error", 0));
                }
            })
            .exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> { progressBar.setVisible(false); btnSend.setEnabled(true); btnFile.setEnabled(true); Throwable c = ex.getCause() != null ? ex.getCause() : ex; JOptionPane.showMessageDialog(parent, "Error sending file: " + c.getMessage(), "Error", 0); }); 
                return null;
            });
    }

    private void encryptAndSendMessage(String messageType, String textContent, String mediaUrl, String fileName) {
        final Component parent = this;
        final String finalMediaType = messageType;
        final String finalMediaUrl = mediaUrl;
        final String finalFileName = fileName;

        CompletableFuture<PublicKey> friendKeyFuture = NetworkService.getPublicKey(friendId);
        CompletableFuture<PublicKey> selfKeyFuture = CompletableFuture.completedFuture(CryptoService.getPublicKey());

        CompletableFuture.allOf(friendKeyFuture, selfKeyFuture).thenAccept(voidResult -> {
            PublicKey friendKey = friendKeyFuture.join();
            PublicKey selfKey = selfKeyFuture.join();

            if (friendKey == null || selfKey == null) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, "Lỗi E2EE: Không thể lấy khóa (của bạn hoặc người nhận).", "Lỗi", 0));
                return;
            }

            SecretKey sessionKey = CryptoService.generateAESKey_Session();
            if (sessionKey == null) {
                 SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, "Lỗi E2EE: Không thể tạo khóa phiên.", "Lỗi", 0));
                 return;
            }
            String sessionKeyString = CryptoService.aesKeyToString(sessionKey);
            
            String plainTextContent;
            final String localDisplayContent; 

            if ("text".equals(finalMediaType)) {
                plainTextContent = textContent;
                localDisplayContent = textContent;
            } else {
                plainTextContent = String.format("{\"url\":\"%s\",\"fileName\":\"%s\"}", finalMediaUrl, finalFileName);
                localDisplayContent = null;
            }

            Map<String, String> encryptedData = CryptoService.aesEncrypt(plainTextContent, sessionKey);
            if (encryptedData == null) {
                 SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, "Lỗi E2EE: Mã hóa AES thất bại.", "Lỗi", 0));
                 return;
            }
            String contentCipher = encryptedData.get("cipherText");
            String contentIv = encryptedData.get("iv");
            
            String encKeyForSelf = CryptoService.encryptRSA(sessionKeyString, selfKey);
            String encKeyForFriend = CryptoService.encryptRSA(sessionKeyString, friendKey);
            
            if(encKeyForSelf == null || encKeyForFriend == null) {
                 SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, "Lỗi E2EE: Mã hóa RSA thất bại.", "Lỗi", 0));
                 return;
            }
            
            Map<String, String> keyMap = new HashMap<>();
            keyMap.put(String.valueOf(currentUserId), encKeyForSelf);
            keyMap.put(String.valueOf(friendId), encKeyForFriend);

            sendStompMessage(finalMediaType, contentCipher, contentIv, keyMap);
            
            SwingUtilities.invokeLater(() -> {
                ChatMessage msg = new ChatMessage();
                msg.setSenderId(currentUserId);
                msg.setMessageType(finalMediaType);
                msg.setTimestamp(ZonedDateTime.now());
                if ("text".equals(finalMediaType)) {
                    msg.setDecryptedContent(localDisplayContent);
                } else {
                    msg.setDecryptedMediaUrl(finalMediaUrl);
                    msg.setDecryptedFileName(finalFileName);
                }
                addMessageBubble(msg, true);
                chatPanel.revalidate();
                scrollToBottom();
            });

        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, "Lỗi mạng lấy key: " + ex.getMessage(), "Lỗi", 0));
            return null;
        });
    }

    private void sendStompMessage(String messageType, String contentCipher, String contentIv, Map<String, String> keys) {
        
        MessageSendDTO dto = new MessageSendDTO();
        dto.setSenderId(currentUserId);
        dto.setReceiverId(friendId);
        dto.setGroupId(null);
        dto.setMessageType(messageType);
        dto.setContentCipher(contentCipher);
        dto.setContentIv(contentIv);
        dto.setKeys(keys);

        String jsonToSend = dto.toJson();
        byte[] jsonBytes = jsonToSend.getBytes(StandardCharsets.UTF_8);
        int contentLength = jsonBytes.length;
        
        String sendFrame = """
                        SEND
                        destination:/app/chat.send
                        content-type:application/json
                        content-length:%d

                        %s\0""".formatted(contentLength, jsonToSend);
                        
        if (ws != null && ws.isOpen()) {
            System.out.println(">>> Sending STOMP 1:1 (E2EE Lớp 2)");
            ws.send(sendFrame);
        } else {
            JOptionPane.showMessageDialog(this, "Connection lost.", "Error", 0);
        }
    }

    public void handleIncomingMessage(final ChatMessage msg) {
        if (msg != null && msg.getSenderId() == friendId) {
             
             java.security.PrivateKey myPrivateKey = CryptoService.getPrivateKey();
             if (myPrivateKey == null) {
                 System.err.println("ChatForm: Không thể giải mã tin nhắn đến, Private Key là null!");
                 return;
             }
             
             String encSessionKey = msg.getEncSessionKey();
             if (encSessionKey == null) {
                  System.err.println("ChatForm: Tin nhắn đến không có 'encSessionKey'!");
                  setErrorBubble(msg);
             } else {
                 String sessionKeyString = CryptoService.decryptRSA(encSessionKey, myPrivateKey);
                 if (sessionKeyString == null) {
                      System.err.println("ChatForm: Giải mã RSA thất bại! (Không thể lấy khóa AES)");
                      setErrorBubble(msg);
                 } else {
                     SecretKey sessionKey = CryptoService.stringToAESKey(sessionKeyString);
                     if (sessionKey == null) {
                         System.err.println("ChatForm: Chuyển string thành AES key thất bại!");
                         setErrorBubble(msg);
                     } else {
                         String decryptedPayload = CryptoService.aesDecrypt(msg.getContent(), msg.getContentIv(), sessionKey);
                         if (decryptedPayload == null) {
                             System.err.println("ChatForm: Giải mã AES thất bại! (Nội dung hỏng hoặc sai khóa)");
                             setErrorBubble(msg);
                         } else {
                             // Bóc tách payload
                             if ("text".equals(msg.getMessageType())) {
                                 msg.setDecryptedContent(decryptedPayload);
                             } else {
                                 try {
                                     JsonNode root = new ObjectMapper().readTree(decryptedPayload);
                                     msg.setDecryptedMediaUrl(root.get("url").asText(null));
                                     msg.setDecryptedFileName(root.get("fileName").asText(null));
                                 } catch (Exception e) {
                                      System.err.println("ChatForm: Lỗi parse JSON payload của file: " + e.getMessage());
                                      setErrorBubble(msg);
                                 }
                             }
                         }
                     }
                 }
             }

             SwingUtilities.invokeLater(() -> {
                 addMessageBubble(msg, false);
                 chatPanel.revalidate(); 
                 scrollToBottom();
                 if (!this.isFocusOwner() && !txtMsg.isFocusOwner()) Toolkit.getDefaultToolkit().beep();
             });
        }
    }
    
    private void setErrorBubble(ChatMessage msg) {
         msg.setMessageType("text");
         msg.setDecryptedContent(DECRYPT_ERROR_PLACEHOLDER);
    }
    
    private void startCall(String type) {
    System.out.println("Starting call to " + friendId + " type=" + type);

    NetworkService.startCall(currentUserId, friendId, type)
        .thenAccept(callId -> {
            // Gửi tín hiệu gọi qua WebSocket
            String frame = """
                SEND
                destination:/app/call.send
                content-type:application/json

                {"type":"call_request","callerId":%d,"receiverId":%d,"callType":"%s"}\0
                """.formatted(currentUserId, friendId, type);

            ws.send(frame);

            // MỞ UI CUỘC GỌI DÙNG IP ĐỘNG
            SwingUtilities.invokeLater(() -> {
                String baseUrl = NetworkService.API_BASE_URL; // DÙNG API_BASE_URL
                String url = baseUrl + "/call.html"
                        + "?callId=" + callId
                        + "&userId=" + currentUserId
                        + "&peerId=" + friendId
                        + "&type=" + type;

                try {
                    Desktop.getDesktop().browse(new URI(url));
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Không thể mở trình duyệt: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            });
        })
        .exceptionally(ex -> {
            JOptionPane.showMessageDialog(this, "Lỗi khởi tạo cuộc gọi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            return null;
        });
}


    private void loadHistory() {
        System.out.println("Loading 1:1 history (E2EE Lớp 2): " + currentUserId + " <-> " + friendId);
        
        java.security.PrivateKey myPrivateKey = CryptoService.getPrivateKey();
        if (myPrivateKey == null) {
            System.err.println("ChatForm: Không thể tải lịch sử, Private Key là null!");
            return;
        }

        NetworkService.getMessages(currentUserId, friendId).thenAccept(messages -> {
            SwingUtilities.invokeLater(() -> {
                chatPanel.removeAll();
                if (messages != null && !messages.isEmpty()) {
                    System.out.println("Processing " + messages.size() + " historical messages (E2EE Lớp 2).");
                    
                    for (final ChatMessage msg : messages) {
                        if (msg != null) {
                            boolean sentByMe = msg.getSenderId() == currentUserId;
                            
                            String sessionKeyString = CryptoService.decryptRSA(msg.getEncSessionKey(), myPrivateKey);
                            if (sessionKeyString == null) {
                                setErrorBubble(msg);
                            } else {
                                SecretKey sessionKey = CryptoService.stringToAESKey(sessionKeyString);
                                if (sessionKey == null) {
                                     setErrorBubble(msg);
                                } else {
                                    String decryptedPayload = CryptoService.aesDecrypt(msg.getContent(), msg.getContentIv(), sessionKey);
                                    if (decryptedPayload == null) {
                                         setErrorBubble(msg);
                                    } else {
                                        if ("text".equals(msg.getMessageType())) {
                                            msg.setDecryptedContent(decryptedPayload);
                                        } else {
                                            try {
                                                JsonNode root = new ObjectMapper().readTree(decryptedPayload);
                                                msg.setDecryptedMediaUrl(root.get("url").asText(null));
                                                msg.setDecryptedFileName(root.get("fileName").asText(null));
                                            } catch (Exception e) {
                                                 setErrorBubble(msg);
                                            }
                                        }
                                    }
                                }
                            }
                            addMessageBubble(msg, sentByMe);
                        }
                    }
                } else { 
                    System.out.println("No 1:1 history found."); 
                    JLabel l = new JLabel("--- Start of encrypted conversation ---", 0); 
                    l.setForeground(Color.GRAY); 
                    JPanel p = new JPanel(new FlowLayout(1)); 
                    p.setBackground(Color.WHITE); p.add(l); 
                    p.setMaximumSize(new Dimension(Integer.MAX_VALUE, l.getPreferredSize().height + 10)); 
                    chatPanel.add(p);
                }
                chatPanel.revalidate(); 
                scrollToBottom();
            });
        }).exceptionally(ex -> { System.err.println("Failed history load: " + ex.getMessage()); SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Could not load history.\nError: " + ex.getMessage(), "Error", 0)); return null; });
    }

    // --- UI Helper Methods ---
    
    // Trong ChatForm.java
private void addMessageBubble(ChatMessage msg, boolean alignRight) {
    if (msg == null) return;
    RoundedBubblePanel bubble = new RoundedBubblePanel(20, 20);
    bubble.setLayout(new BorderLayout(5, 3)); 
    bubble.setBackground(alignRight ? new Color(225, 245, 255) : new Color(240, 240, 240)); 
    bubble.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10)); 
    bubble.setOpaque(false);
    String bubbleStyle = "width: 350px; max-width: 100%;"; 
    Component contentComponent;

    String text = msg.getDecryptedContent();
    String url = msg.getDecryptedMediaUrl();
    String name = msg.getDecryptedFileName();

    try {
        boolean isContentError = DECRYPT_ERROR_PLACEHOLDER.equals(text);
        
        if (isContentError) {
            JLabel errorLabel = new JLabel(DECRYPT_ERROR_PLACEHOLDER); 
            errorLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14)); 
            errorLabel.setForeground(Color.RED); 
            errorLabel.setOpaque(false); 
            contentComponent = errorLabel;
        } else if ("text".equals(msg.getMessageType()) && text != null && !text.trim().isEmpty()) {
            String escapedHtml = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
            JLabel textLabel = new JLabel("<html><body style='" + bubbleStyle + "'>" + escapedHtml + "</body></html>"); 
            textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16)); 
            textLabel.setOpaque(false); 
            contentComponent = textLabel;
        } else if (url != null && "image".equals(msg.getMessageType())) {
            contentComponent = createImageLabel(url);
        } else if (url != null && ("video".equals(msg.getMessageType()) || "audio".equals(msg.getMessageType()) || "file".equals(msg.getMessageType()))) {
            String fileNameToShow = (name == null) ? "[File]" : name;
            contentComponent = createLinkLabel(fileNameToShow, url, "video".equals(msg.getMessageType()), "audio".equals(msg.getMessageType()));
        } else { 
            JLabel fallbackLabel = new JLabel("[Unsupported message]");
            fallbackLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14)); 
            fallbackLabel.setForeground(Color.GRAY); 
            fallbackLabel.setOpaque(false);
            contentComponent = fallbackLabel;
        }
    } catch (Exception e) { 
        JLabel errorLabel = new JLabel("[Display Error]"); 
        errorLabel.setForeground(Color.RED); 
        errorLabel.setOpaque(false); 
        contentComponent = errorLabel; 
    }

    bubble.add(contentComponent, BorderLayout.CENTER);
    ZonedDateTime timestamp = msg.getTimestamp();
    if (timestamp != null) { 
        // Chuẩn hóa múi giờ
        ZonedDateTime localTimestamp = timestamp.withZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh"));
        JLabel timeLabel = new JLabel(localTimestamp.format(DateTimeFormatter.ofPattern("HH:mm"))); 
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10)); 
        timeLabel.setForeground(Color.DARK_GRAY); 
        timeLabel.setHorizontalAlignment(alignRight ? 4 : 2); 
        timeLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 0, 2)); 
        timeLabel.setOpaque(false); 
        bubble.add(timeLabel, BorderLayout.SOUTH); 
    } else { 
        bubble.add(Box.createVerticalStrut(12), BorderLayout.SOUTH); 
    }
    JPanel wrapper = new JPanel(new FlowLayout(alignRight ? 4 : 0, 0, 2)); 
    wrapper.setBackground(Color.WHITE); 
    wrapper.add(bubble); 
    wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, wrapper.getPreferredSize().height)); 
    chatPanel.add(wrapper);
}
    
    
    private JLabel createImageLabel(String url) {
        if (url == null) { JLabel el = new JLabel(DECRYPT_ERROR_PLACEHOLDER); el.setFont(new Font("Segoe UI", Font.ITALIC, 14)); el.setForeground(Color.RED); el.setOpaque(false); return el; }
        JLabel iL = new JLabel("..."); iL.setHorizontalAlignment(0); iL.setVerticalAlignment(0); iL.setPreferredSize(new Dimension(150,150)); iL.setBorder(BorderFactory.createEtchedBorder()); iL.setOpaque(true); iL.setBackground(Color.LIGHT_GRAY);
        CompletableFuture.supplyAsync(()->{ HttpClient c=HttpClient.newHttpClient(); String eU=NetworkService.encodeUrlPath(url); if(eU==null)return CompletableFuture.failedFuture(new IllegalArgumentException("URL lỗi")); HttpRequest req=HttpRequest.newBuilder().uri(URI.create(eU)).timeout(Duration.ofSeconds(20)).GET().build(); try{HttpResponse<byte[]> r=c.send(req,HttpResponse.BodyHandlers.ofByteArray()); return r.statusCode()==200?r.body():CompletableFuture.failedFuture(new IOException("HTTP "+r.statusCode()));}catch(Exception ex){return CompletableFuture.failedFuture(ex);} }).thenCompose(r->r instanceof CompletableFuture?(CompletableFuture<byte[]>)r:CompletableFuture.completedFuture((byte[])r)).thenAccept(b->{ if(b==null||b.length==0)throw new CompletionException(new IOException("Ảnh rỗng")); Image i=null; try(ByteArrayInputStream bis=new ByteArrayInputStream(b)){i=ImageIO.read(bis);}catch(IOException ex){} if(i==null)i=new ImageIcon(b).getImage(); if(i==null||i.getWidth(null)<=0)throw new CompletionException(new IOException("Ảnh lỗi")); int ow=i.getWidth(null),oh=i.getHeight(null),mw=250,mh=250,nw=ow,nh=oh; double r=(double)ow/oh; if(ow>mw){nw=mw;nh=(int)(nw/r);} if(nh>mh){nh=mh;nw=(int)(nh*r);} nw=Math.max(1,nw); nh=Math.max(1,nh); try{Image s=i.getScaledInstance(nw,nh,Image.SCALE_SMOOTH); if(s==null)throw new Exception("Scale lỗi"); var icon=new ImageIcon(s); final String fU=url; SwingUtilities.invokeLater(()->{iL.setIcon(icon);iL.setText(null);iL.setPreferredSize(null);iL.setBorder(null);iL.setOpaque(false);iL.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); for(MouseListener ml:iL.getMouseListeners())iL.removeMouseListener(ml); iL.addMouseListener(new MouseAdapter(){@Override public void mouseClicked(MouseEvent e){showImagePreview(fU);}}); Container p=iL.getParent(); if(p!=null){p.validate();p.repaint();}}); }catch(Exception ex){throw new CompletionException("Scale lỗi",ex);} }).exceptionally(ex->{SwingUtilities.invokeLater(()->{iL.setText("Tải lỗi");iL.setForeground(Color.RED);});return null;}); return iL;
    }
    
    /**
     * ✅ SỬA ĐỔI: Thêm nút Download cho 'file', giữ nguyên Media/Audio
     */
    private Component createLinkLabel(String name, String url, boolean isVideo, boolean isAudio) {
         if (url == null) { JLabel el = new JLabel(DECRYPT_ERROR_PLACEHOLDER); el.setFont(new Font("Segoe UI", Font.ITALIC, 14)); el.setForeground(Color.RED); el.setOpaque(false); return el; }
         String displayName = (name == null || DECRYPT_ERROR_PLACEHOLDER.equals(name)) ? "[Encrypted File]" : name;
         displayName = (displayName != null && displayName.length() > 35) ? displayName.substring(0, 32) + "..." : displayName;
         if (displayName == null || displayName.trim().isEmpty()) displayName = "Attachment";
         
         // Nếu là Video hoặc Audio, giữ nguyên logic cũ (chỉ link)
         if (isVideo || isAudio) {
             String iText = isVideo ? "Video: " : "Audio: ";
             Icon i = UIManager.getIcon(isVideo ? "FileView.videoIcon" : "FileView.audioIcon");
             var l = new JLabel("<html><a href=''>" + displayName + "</a></html>");
             l.setFont(new Font("Segoe UI", Font.PLAIN, 16)); l.setForeground(new Color(0, 102, 204));
             l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); l.setToolTipText("Open: " + (name != null ? name : "link"));
             if (i != null) l.setIcon(i);
             else l.setText("<html><a href=''>" + iText + displayName + "</a></html>");
             for (MouseListener ml : l.getMouseListeners()) l.removeMouseListener(ml);
             l.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { 
                 SwingUtilities.invokeLater(() -> { if (MediaPlayerFrame.checkVlcInstallation()) new MediaPlayerFrame(name, url, isAudio).setVisible(true); else { JOptionPane.showMessageDialog(ChatForm.this, "VLC not found.\nOpening browser...", "Error", 2); openFile(url); } });
             }});
             return l;
         }

         // --- ✅ LOGIC MỚI: Nếu là 'file' (PDF, ZIP, v.v.) ---
         JPanel filePanel = new JPanel(new BorderLayout(10, 0));
         filePanel.setOpaque(false);

         Icon i = UIManager.getIcon("FileView.fileIcon");
         var l = new JLabel("<html><a href=''>" + displayName + "</a></html>");
         l.setFont(new Font("Segoe UI", Font.PLAIN, 16)); l.setForeground(new Color(0, 102, 204));
         l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); l.setToolTipText("Open in browser: " + (name != null ? name : "link"));
         if (i != null) l.setIcon(i);
         else l.setText("<html><a href=''>File: " + displayName + "</a></html>");
         for (MouseListener ml : l.getMouseListeners()) l.removeMouseListener(ml);
         l.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { 
             openFile(url);
         }});
         
         JButton btnDownload = new JButton("Tải về");
         btnDownload.setFont(new Font("Segoe UI", Font.PLAIN, 12));
         btnDownload.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
         final String finalName = (name == null || DECRYPT_ERROR_PLACEHOLDER.equals(name)) ? "downloaded_file" : name;
         btnDownload.addActionListener(e -> downloadFile(url, finalName, btnDownload));
         
         filePanel.add(l, BorderLayout.CENTER);
         filePanel.add(btnDownload, BorderLayout.EAST);
         return filePanel;
    }

    /**
     * Mở ImageViewerFrame thay vì trình duyệt.
     */
    private void showImagePreview(String url) {
        SwingUtilities.invokeLater(() -> {
            new ImageViewerFrame(url).setVisible(true);
        });
    }
    
    private void openFile(String url) {
        if(url==null||url.trim().isEmpty())return; 
        if(!Desktop.isDesktopSupported()||!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))return; 
        String eU=NetworkService.encodeUrlPath(url); 
        if(eU==null)return; 
        try{Desktop.getDesktop().browse(new URI(eU));}catch(Exception ex){} 
    }
    
    /**
     * HÀM MỚI: Tải file
     */
    private void downloadFile(String url, String fileName, JButton btnDownload) {
        JFileChooser saveChooser = new JFileChooser();
        saveChooser.setDialogTitle("Lưu File");
        saveChooser.setSelectedFile(new File(fileName));

        if (saveChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = saveChooser.getSelectedFile();
            btnDownload.setEnabled(false);
            btnDownload.setText("..."); 

            new Thread(() -> {
                try {
                    HttpClient client = HttpClient.newHttpClient();
                    String eU = NetworkService.encodeUrlPath(url);
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
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Lỗi khi tải file: " + e.getMessage(), "Lỗi I/O", JOptionPane.ERROR_MESSAGE);
                        btnDownload.setEnabled(true);
                        btnDownload.setText("Tải về");
                    });
                }
            }).start();
        }
    }
    
    private void openVideoCall(String username) { SwingUtilities.invokeLater(() -> new VideoCallFrame(username).setVisible(true)); }
    @Override protected void finalize() throws Throwable { try {} finally { super.finalize(); } }

}
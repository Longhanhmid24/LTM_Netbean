package chatapp_client;

import model.GroupMessage;
import service.NetworkService;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.java_websocket.client.WebSocketClient;
import java.util.Timer;
import java.util.TimerTask;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Panel chat dành riêng cho Group.
 * ✅ ĐÃ CẬP NHẬT: Mở ảnh trong ImageViewerFrame và thêm nút Download.
 */
public class GroupChatForm extends JPanel {

    // --- (Components và Constructor giữ nguyên) ---
    private JPanel chatPanel; private JScrollPane scrollPane; private JTextField txtMsg; private JButton btnSend; private JButton btnFile; private JProgressBar progressBar;
    private int currentUserId; private int groupId; private WebSocketClient ws;
    private Map<Integer, String> memberUsernames;
    public GroupChatForm(int userId, int groupId, String groupName, WebSocketClient sharedWsClient) {
        this.currentUserId = userId; this.groupId = groupId; this.ws = sharedWsClient;
        this.memberUsernames = new java.util.concurrent.ConcurrentHashMap<>();
        initializeChatComponents(); setupChatLayout(); setupChatActions();
        loadMemberUsernamesAndHistory();
    }
    private void initializeChatComponents() { /* (Giữ nguyên) */ setBackground(Color.WHITE); chatPanel = new JPanel() { public boolean getScrollableTracksViewportWidth() { return true; } public boolean getScrollableTracksViewportHeight() { return false; } }; chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS)); chatPanel.setBackground(Color.WHITE); chatPanel.setBorder(new EmptyBorder(10, 10, 10, 10)); scrollPane = new JScrollPane(chatPanel); scrollPane.setBorder(BorderFactory.createEmptyBorder()); scrollPane.getVerticalScrollBar().setUnitIncrement(16); scrollPane.setTransferHandler(createFileDropHandler()); txtMsg = new JTextField(); txtMsg.setFont(new Font("Segoe UI", Font.PLAIN, 16)); txtMsg.setBorder(BorderFactory.createCompoundBorder( BorderFactory.createLineBorder(new Color(210, 210, 210), 1, true), BorderFactory.createEmptyBorder(8, 10, 8, 10) )); txtMsg.setToolTipText("Nhập tin nhắn và nhấn Enter"); btnSend = createStyledButton("Gửi", new Color(0, 122, 255)); btnFile = createStyledButton("Tệp", new Color(76, 175, 80)); progressBar = new JProgressBar(); progressBar.setVisible(false); progressBar.setStringPainted(true); progressBar.setForeground(new Color(0, 122, 255)); progressBar.setBackground(new Color(230, 230, 230)); }
    private void setupChatLayout() { /* (Giữ nguyên) */ setLayout(new BorderLayout(5, 5)); JPanel bottomPanel = new JPanel(new BorderLayout(8, 5)); bottomPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10)); bottomPanel.setBackground(new Color(245, 248, 250)); JPanel inputControls = new JPanel(new BorderLayout(5, 0)); inputControls.setOpaque(false); inputControls.add(txtMsg, BorderLayout.CENTER); inputControls.add(btnSend, BorderLayout.EAST); bottomPanel.add(btnFile, BorderLayout.WEST); bottomPanel.add(inputControls, BorderLayout.CENTER); JPanel southPanel = new JPanel(); southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS)); southPanel.add(progressBar); southPanel.add(bottomPanel); add(scrollPane, BorderLayout.CENTER); add(southPanel, BorderLayout.SOUTH); }
    private void setupChatActions() { /* (Giữ nguyên) */ btnSend.addActionListener(e -> sendMessage()); txtMsg.addActionListener(e -> sendMessage()); btnFile.addActionListener(e -> chooseFile()); }
    private TransferHandler createFileDropHandler() { /* (Giữ nguyên) */ return new TransferHandler() { @Override public boolean canImport(TransferHandler.TransferSupport support) { return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor); } @Override public boolean importData(TransferHandler.TransferSupport support) { if (!canImport(support)) return false; Transferable t = support.getTransferable(); try { java.util.List<File> files = (java.util.List<File>) t.getTransferData(DataFlavor.javaFileListFlavor); if (files.isEmpty()) return false; new Thread(() -> { files.stream().filter(File::isFile).forEach(file -> { System.out.println("Group Drop: " + file.getAbsolutePath()); uploadAndSendFile(file); try { Thread.sleep(100); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); } }); }).start(); return true; } catch (Exception ex) { ex.printStackTrace(); return false; } } }; }
    private JButton createStyledButton(String text, Color bg) { /* (Giữ nguyên) */ JButton b = new JButton(text); b.setFont(new Font("Segoe UI", Font.BOLD, 14)); b.setBackground(bg); b.setForeground(Color.WHITE); b.setFocusPainted(false); b.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15)); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); b.addMouseListener(new MouseAdapter() { final Color o = bg; final Color d = bg.darker(); @Override public void mouseEntered(MouseEvent e) { b.setBackground(d); } @Override public void mouseExited(MouseEvent e) { b.setBackground(o); } }); return b; }

    // --- (Giữ nguyên: sendMessage, chooseFile, uploadAndSendFile, detectFileType, sendGroupStompMessage) ---
    private void sendMessage() { String messageText = txtMsg.getText().trim(); if (!messageText.isEmpty()) { sendGroupStompMessage(messageText, "text", null, null); txtMsg.setText(""); txtMsg.requestFocusInWindow(); } }
    private void chooseFile() { JFileChooser c = new JFileChooser(); c.setDialogTitle("Chọn Tệp Gửi Nhóm"); c.setFileFilter(new FileNameExtensionFilter("Tất cả file hỗ trợ","jpg","png","gif","jpeg","webp","bmp","mp4","mov","avi","wmv","mkv","mp3","wav","ogg","aac","m4a","txt","zip","pdf","docx","xlsx","pptx")); c.setAcceptAllFileFilterUsed(true); if (c.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) { File f = c.getSelectedFile(); if (f != null && f.exists() && f.isFile()) uploadAndSendFile(f); else JOptionPane.showMessageDialog(this, "File không hợp lệ.", "Lỗi", JOptionPane.ERROR_MESSAGE); } }
    private void uploadAndSendFile(File file) { long maxSize = 100 * 1024 * 1024; if (file.length() > maxSize) { JOptionPane.showMessageDialog(this, "File quá lớn (Tối đa 100MB).", "Lỗi", JOptionPane.ERROR_MESSAGE); return; } SwingUtilities.invokeLater(() -> { progressBar.setValue(0); progressBar.setVisible(true); btnSend.setEnabled(false); btnFile.setEnabled(false); }); CompletableFuture.supplyAsync(() -> NetworkService.uploadFile(file)) .thenCompose(future -> future) .thenAccept(resultUrlObject -> { String url = (resultUrlObject instanceof String) ? (String) resultUrlObject : null; final String finalUrl = url; SwingUtilities.invokeLater(() -> { progressBar.setVisible(false); btnSend.setEnabled(true); btnFile.setEnabled(true); if (finalUrl != null && !finalUrl.isEmpty()) { String fileType = detectFileType(file); sendGroupStompMessage(null, fileType, finalUrl, file.getName()); System.out.println("Upload nhóm thành công, đã gửi: " + file.getName()); } else { JOptionPane.showMessageDialog(this, "Upload thất bại: Không có URL.", "Lỗi", JOptionPane.ERROR_MESSAGE); } }); }).exceptionally(ex -> { SwingUtilities.invokeLater(() -> { progressBar.setVisible(false); btnSend.setEnabled(true); btnFile.setEnabled(true); JOptionPane.showMessageDialog(this, "Lỗi upload: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE); }); return null; }); }
    private String detectFileType(File file) { String n = file.getName().toLowerCase(); if (n.matches(".*\\.(jpg|jpeg|png|gif|webp|bmp)$")) return "image"; if (n.matches(".*\\.(mp4|mov|avi|wmv|mkv|flv|webm)$")) return "video"; if (n.matches(".*\\.(mp3|wav|ogg|aac|m4a)$")) return "audio"; return "file"; }
    private void sendGroupStompMessage(String content, String messageType, String mediaUrl, String fileName) { if (ws == null || !ws.isOpen()) { JOptionPane.showMessageDialog(this, "Mất kết nối WebSocket. Không thể gửi tin nhắn nhóm.", "Lỗi Kết Nối", JOptionPane.ERROR_MESSAGE); return; } GroupMessage msgToSend = new GroupMessage(); msgToSend.setGroupId(groupId); msgToSend.setSenderId(currentUserId); msgToSend.setMessageType(messageType); msgToSend.setContent(content); msgToSend.setMediaUrl(mediaUrl); msgToSend.setFileName(fileName); String jsonToSend = NetworkService.createGroupSendableJson(msgToSend); byte[] jsonBytes = jsonToSend.getBytes(StandardCharsets.UTF_8); int contentLength = jsonBytes.length; String sendFrame = """
                        SEND
                        destination:/app/group.send
                        content-type:application/json
                        content-length:%d

                        %s\0""".formatted(contentLength, jsonToSend); System.out.println(">>> Gửi STOMP NHÓM (Dài: " + contentLength + "): " + sendFrame.replace("\0", "[NUL]")); ws.send(sendFrame); msgToSend.setSenderUsername(memberUsernames.getOrDefault(currentUserId, "Bạn")); msgToSend.setTimestamp(ZonedDateTime.now()); addMessageBubble(msgToSend, true); chatPanel.revalidate(); scrollToBottom(); }

    // --- (Giữ nguyên: loadMemberUsernamesAndHistory, loadGroupHistory, handleIncomingGroupMessage, addMessageBubble) ---
    private void loadMemberUsernamesAndHistory() { System.out.println("Đang tải thành viên nhóm: " + groupId); NetworkService.getGroupMembers(groupId).thenAccept(members -> { if (members != null) { memberUsernames = members.stream() .collect(Collectors.toMap( model.GroupMember::getMemberId, model.GroupMember::getUsername, (existing, replacement) -> existing )); System.out.println("Đã tải " + memberUsernames.size() + " username thành viên."); loadGroupHistory(); } else { SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Không thể tải danh sách thành viên.", "Lỗi", JOptionPane.ERROR_MESSAGE)); loadGroupHistory(); } }).exceptionally(ex -> { SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi tải thành viên: " + ex.getMessage(), "Lỗi Mạng", JOptionPane.ERROR_MESSAGE)); loadGroupHistory(); return null; }); }
    private void loadGroupHistory() { System.out.println("Đang tải lịch sử nhóm: " + groupId); NetworkService.getGroupMessages(groupId).thenAccept(messages -> { SwingUtilities.invokeLater(() -> { chatPanel.removeAll(); if (messages != null && !messages.isEmpty()) { System.out.println("Hiển thị " + messages.size() + " tin nhắn lịch sử nhóm."); for (GroupMessage msg : messages) { if (msg != null) { msg.setSenderUsername(memberUsernames.getOrDefault(msg.getSenderId(), "User " + msg.getSenderId())); addMessageBubble(msg, msg.getSenderId() == currentUserId); } } } else { System.out.println("Không tìm thấy lịch sử nhóm."); JLabel startLabel = new JLabel("--- Bắt đầu cuộc trò chuyện nhóm ---", SwingConstants.CENTER); startLabel.setForeground(Color.GRAY); JPanel startPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); startPanel.setBackground(Color.WHITE); startPanel.add(startLabel); startPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, startLabel.getPreferredSize().height + 10)); chatPanel.add(startPanel); } chatPanel.revalidate(); System.out.println("Hoàn tất thêm bong bóng lịch sử nhóm."); scrollToBottom(); }); }).exceptionally(ex -> { System.err.println("Tải lịch sử nhóm thất bại: " + ex.getMessage()); ex.printStackTrace(); SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Không thể tải lịch sử nhóm.\nLỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE)); return null; }); }
    public void handleIncomingGroupMessage(GroupMessage msg) { if (msg != null && msg.getGroupId() == this.groupId) { msg.setSenderUsername(memberUsernames.getOrDefault(msg.getSenderId(), "User " + msg.getSenderId())); SwingUtilities.invokeLater(() -> { addMessageBubble(msg, msg.getSenderId() == currentUserId); chatPanel.revalidate(); scrollToBottom(); if (!this.isShowing()) { Toolkit.getDefaultToolkit().beep(); } }); } }
    private void addMessageBubble(GroupMessage msg, boolean alignRight) { if (msg == null) return; var bubble = new RoundedBubblePanel(20, 20); bubble.setLayout(new BorderLayout(5, 3)); bubble.setBackground(alignRight ? new Color(225, 245, 255) : new Color(240, 240, 240)); bubble.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10)); bubble.setOpaque(false); String bubbleStyle = "width: 350px; max-width: 100%;"; JPanel contentWrapper = new JPanel(new BorderLayout(0, 2)); contentWrapper.setOpaque(false); if (!alignRight) { JLabel senderLabel = new JLabel(msg.getSenderUsername() != null ? msg.getSenderUsername() : ("User " + msg.getSenderId())); senderLabel.setFont(new Font("Segoe UI", Font.BOLD, 11)); senderLabel.setForeground(new Color(0, 100, 0)); senderLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 1, 0)); contentWrapper.add(senderLabel, BorderLayout.NORTH); } Component contentComponent; try { if ("text".equals(msg.getMessageType()) && msg.getContent() != null && !msg.getContent().trim().isEmpty()) { String escaped = msg.getContent().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>"); var lbl = new JLabel("<html><body style='" + bubbleStyle + "'>" + escaped + "</body></html>"); lbl.setFont(new Font("Segoe UI", Font.PLAIN, 16)); lbl.setOpaque(false); contentComponent = lbl; } else if ("image".equals(msg.getMessageType()) && msg.getMediaUrl() != null) { contentComponent = createImageLabel(msg.getMediaUrl()); } else if (("video".equals(msg.getMessageType()) || "audio".equals(msg.getMessageType()) || "file".equals(msg.getMessageType())) && msg.getMediaUrl() != null && msg.getFileName() != null) { contentComponent = createLinkLabel(msg.getFileName(), msg.getMediaUrl(), "video".equals(msg.getMessageType()), "audio".equals(msg.getMessageType())); } else { String fallback = "[Tin nhắn nhóm không hỗ trợ/rỗng]"; var lbl = new JLabel(fallback); lbl.setFont(new Font("Segoe UI", Font.ITALIC, 14)); lbl.setForeground(Color.GRAY); lbl.setOpaque(false); contentComponent = lbl; } } catch (Exception e) { System.err.println("Lỗi tạo nội dung bong bóng nhóm: " + e.getMessage()); var errLbl = new JLabel("[Lỗi hiển thị]"); errLbl.setForeground(Color.RED); errLbl.setOpaque(false); contentComponent = errLbl; e.printStackTrace(); } contentWrapper.add(contentComponent, BorderLayout.CENTER); bubble.add(contentWrapper, BorderLayout.CENTER); ZonedDateTime ts = msg.getTimestamp(); if (ts != null) { var timeLbl = new JLabel(ts.format(DateTimeFormatter.ofPattern("HH:mm"))); timeLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10)); timeLbl.setForeground(Color.DARK_GRAY); timeLbl.setHorizontalAlignment(alignRight ? SwingConstants.RIGHT : SwingConstants.LEFT); timeLbl.setBorder(BorderFactory.createEmptyBorder(2, 2, 0, 2)); timeLbl.setOpaque(false); bubble.add(timeLbl, BorderLayout.SOUTH); } else { bubble.add(Box.createVerticalStrut(12), BorderLayout.SOUTH); } var wrapper = new JPanel(new FlowLayout(alignRight ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 2)); wrapper.setBackground(Color.WHITE); wrapper.add(bubble); wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, wrapper.getPreferredSize().height)); chatPanel.add(wrapper); }
    
    
    /**
     * ✅ SỬA ĐỔI: createImageLabel (Giống ChatForm)
     */
    private JLabel createImageLabel(String url) {
        var imgLabel = new JLabel("Đang tải..."); imgLabel.setHorizontalAlignment(SwingConstants.CENTER); imgLabel.setVerticalAlignment(SwingConstants.CENTER);
        imgLabel.setPreferredSize(new Dimension(150, 150)); imgLabel.setBorder(BorderFactory.createEtchedBorder()); imgLabel.setOpaque(true); imgLabel.setBackground(Color.LIGHT_GRAY);
        if (url == null || url.trim().isEmpty()) { imgLabel.setText("URL Ảnh lỗi"); imgLabel.setForeground(Color.RED); return imgLabel; }
        
        CompletableFuture.supplyAsync(() -> {
            HttpClient cl = HttpClient.newHttpClient(); String encUrl = NetworkService.encodeUrlPath(url); if (encUrl == null) return CompletableFuture.<byte[]>failedFuture(new IllegalArgumentException("URL lỗi"));
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(encUrl)).timeout(java.time.Duration.ofSeconds(20)).GET().build();
            try { HttpResponse<byte[]> resp = cl.send(req, HttpResponse.BodyHandlers.ofByteArray()); return resp.statusCode() == 200 ? resp.body() : CompletableFuture.<byte[]>failedFuture(new IOException("HTTP " + resp.statusCode()));
            } catch (Exception ex) { return CompletableFuture.<byte[]>failedFuture(ex); }
        })
        .thenCompose(r -> r instanceof CompletableFuture ? (CompletableFuture<byte[]>) r : CompletableFuture.completedFuture((byte[]) r))
        .thenAccept(bytes -> {
            if (bytes == null || bytes.length == 0) throw new CompletionException(new IOException("Ảnh rỗng")); Image img = null;
            try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) { img = ImageIO.read(bis); } catch (IOException ex) {}
            if (img == null) img = new ImageIcon(bytes).getImage();
            if (img == null || img.getWidth(null) <= 0) throw new CompletionException(new IOException("Ảnh lỗi"));
            int ow=img.getWidth(null), oh=img.getHeight(null), mw=250, mh=250, nw=ow, nh=oh; double r=(double)ow/oh; if(ow>mw){nw=mw; nh=(int)(nw/r);} if(nh>mh){nh=mh; nw=(int)(nh*r);} nw=Math.max(1,nw); nh=Math.max(1,nh);
            try { Image scaled = img.getScaledInstance(nw, nh, Image.SCALE_SMOOTH); if (scaled == null) throw new Exception("Scale lỗi"); var icon = new ImageIcon(scaled); final String fUrl = url;
                SwingUtilities.invokeLater(() -> { imgLabel.setIcon(icon); imgLabel.setText(null); imgLabel.setPreferredSize(null); imgLabel.setBorder(null); imgLabel.setOpaque(false); imgLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    for (MouseListener ml : imgLabel.getMouseListeners()) imgLabel.removeMouseListener(ml);
                    imgLabel.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { showImagePreview(fUrl); } });
                    Container p = imgLabel.getParent(); if (p != null) { p.validate(); p.repaint(); }
                });
            } catch (Exception ex) { throw new CompletionException("Scale lỗi", ex); }
        })
        .exceptionally(ex -> { SwingUtilities.invokeLater(() -> { imgLabel.setText("Tải lỗi"); imgLabel.setForeground(Color.RED); }); return null; });
        return imgLabel;
    }
    
    /**
     * ✅ SỬA ĐỔI: Thêm nút Download cho 'file'
     */
    private Component createLinkLabel(String name, String url, boolean isVideo, boolean isAudio) {
         if (url == null) { JLabel el = new JLabel("Lỗi URL"); el.setFont(new Font("Segoe UI", Font.ITALIC, 14)); el.setForeground(Color.RED); el.setOpaque(false); return el; }
         String displayName = (name != null && !name.trim().isEmpty()) ? name : "Attachment";
         displayName = (displayName.length() > 35) ? displayName.substring(0, 32) + "..." : displayName;
         
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
                 SwingUtilities.invokeLater(() -> { if (MediaPlayerFrame.checkVlcInstallation()) new MediaPlayerFrame(name, url, isAudio).setVisible(true); else { JOptionPane.showMessageDialog(GroupChatForm.this, "Không tìm thấy VLC.\nMở bằng trình duyệt...", "Lỗi", JOptionPane.WARNING_MESSAGE); openFile(url); } });
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
         final String finalName = (name != null && !name.trim().isEmpty()) ? name : "downloaded_file";
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
        if (url == null || url.trim().isEmpty()) return; if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) return;
        String encUrl = NetworkService.encodeUrlPath(url); if (encUrl == null) return;
        try { Desktop.getDesktop().browse(new URI(encUrl)); } catch (Exception ex) { System.err.println("Lỗi mở link: " + ex.getMessage()); }
    }
    
    /**
     * ✅ HÀM MỚI: Tải file (Tương tự ChatForm)
     */
    private void downloadFile(String url, String fileName, JButton btnDownload) {
        JFileChooser saveChooser = new JFileChooser();
        saveChooser.setDialogTitle("Lưu File");
        saveChooser.setSelectedFile(new File(fileName));

        if (saveChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = saveChooser.getSelectedFile();
            btnDownload.setEnabled(false);
            btnDownload.setText("..."); // Đang tải...

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
    
    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> { JScrollBar v = scrollPane.getVerticalScrollBar(); if (v != null) SwingUtilities.invokeLater(() -> v.setValue(v.getMaximum())); });
    }
}
package service;

// ... (Imports cũ: Group, GroupMember, ChatMessage, LoginResponse, RegisterRequest, MessageSendDTO, v.v.)
import model.Group;
import model.GroupMember;
import model.GroupMessage;
import model.User;
import model.ChatMessage;
import model.LoginResponse;
import model.RegisterRequest;
import model.MessageSendDTO;
import model.Friendship; // ✅ IMPORT MỚI
import model.FriendRequest; // ✅ IMPORT MỚI
// ... (Các import Java, Jackson, WebSocket, HTTP còn lại) ...
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.net.MalformedURLException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.sql.Timestamp;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import util.LenientZonedDateTimeDeserializer;
import java.security.PublicKey;
import java.util.Base64;
import javax.crypto.SecretKey;

public class NetworkService {

    // ... (Constants, Mappers, và các hàm helper JSON giữ nguyên) ...
    private static final String SERVER_IP = "192.168.1.230";
    public static final String BASE_URL = "http://" + SERVER_IP + ":8080/api/";
    public static final String WS_URL = "ws://" + SERVER_IP + ":8080/ws/raw";
    private static final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).build();
    private static final ObjectMapper MAPPER; // DB (Snake Case)
    private static final ObjectMapper STOMP_MAPPER; // DTO Nhận (CamelCase)
    private static final ObjectMapper SEND_MAPPER; // DTO Gửi (CamelCase)
    static { /* ... (Giữ nguyên) ... */
        ObjectMapper m1 = new ObjectMapper(); m1.registerModule(new JavaTimeModule()); m1.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); m1.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE); SimpleModule ldm1 = new SimpleModule(); ldm1.addDeserializer(ZonedDateTime.class, new LenientZonedDateTimeDeserializer()); m1.registerModule(ldm1); m1.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); MAPPER = m1;
        ObjectMapper m2 = new ObjectMapper(); m2.registerModule(new JavaTimeModule()); m2.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); SimpleModule ldm2 = new SimpleModule(); ldm2.addDeserializer(ZonedDateTime.class, new LenientZonedDateTimeDeserializer()); m2.registerModule(ldm2); m2.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); STOMP_MAPPER = m2;
        ObjectMapper m3 = new ObjectMapper(); m3.registerModule(new JavaTimeModule()); m3.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); SEND_MAPPER = m3;
    }
    public static String createE2EESendableJson(MessageSendDTO dto) { try { return SEND_MAPPER.writeValueAsString(dto); } catch (Exception e) { return "{}"; } }
    public static String createGroupSendableJson(GroupMessage gm) { try { return SEND_MAPPER.writeValueAsString(gm); } catch(Exception e){ return "{}"; } }
    public static ChatMessage parseStompMessage(String json) { if(json==null || json.isEmpty()) return null; try { return STOMP_MAPPER.readValue(json, ChatMessage.class); } catch(Exception e){ System.err.println("Lỗi parseSTOMP: " + e.getMessage() + " | JSON: " + json); return null; } }
    public static GroupMessage parseGroupStompMessage(String json) { if(json==null || json.isEmpty()) return null; try { return STOMP_MAPPER.readValue(json, GroupMessage.class); } catch(Exception e){ return null; } }
    public static void init() { System.out.println("NetworkService initialized."); }
    public static String encodeUrlPath(String urlString) { try { URL u=new URL(urlString); URI uri=new URI(u.getProtocol(),u.getUserInfo(),u.getHost(),u.getPort(),u.getPath(),u.getQuery(),u.getRef()); return uri.toASCIIString(); } catch (Exception e) { return null;} }

    
    // --- API Methods (Auth & User) ---
    public static CompletableFuture<LoginResponse> login(String usernameOrSdt, String password) { /* (Giữ nguyên) */
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonBody = SEND_MAPPER.writeValueAsString(Map.of("username", usernameOrSdt, "password", password));
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE_URL + "auth/login")).header("Content-Type", "application/json").timeout(java.time.Duration.ofSeconds(15)).POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return STOMP_MAPPER.readValue(response.body(), LoginResponse.class);
                } else {
                    String errorMsg = "Lỗi không xác định";
                    try { JsonNode errorNode = STOMP_MAPPER.readTree(response.body()); if (errorNode.has("error")) errorMsg = errorNode.get("error").asText(); } catch (Exception e) {}
                    throw new IOException(errorMsg);
                }
            } catch (Exception e) {
                 if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                throw new CompletionException(e);
            }
        });
    }
    public static CompletableFuture<User> createUser(RegisterRequest requestDto) { /* (Giữ nguyên) */
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonBody = SEND_MAPPER.writeValueAsString(requestDto);
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE_URL + "users")).header("Content-Type", "application/json").timeout(java.time.Duration.ofSeconds(15)).POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonNode root = STOMP_MAPPER.readTree(response.body());
                    if (root.get("success").asBoolean()) {
                         User createdUser = new User();
                         createdUser.setId(root.get("userId").asInt());
                         createdUser.setUsername(root.get("username").asText());
                         return createdUser;
                    } else {
                         throw new IOException(root.get("error").asText("Đăng ký thất bại"));
                    }
                } else {
                    String errorMsg = "Lỗi không xác định";
                    try { JsonNode errorNode = STOMP_MAPPER.readTree(response.body()); if (errorNode.has("error")) errorMsg = errorNode.get("error").asText(); } catch (Exception e) {}
                    throw new IOException(errorMsg);
                }
            } catch (Exception e) {
                 if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                throw new CompletionException(e);
            }
        });
    }
    public static CompletableFuture<List<User>> getAllUsers() { /* (Giữ nguyên) */ return CompletableFuture.supplyAsync(() -> { try { HttpRequest r=HttpRequest.newBuilder().uri(URI.create(BASE_URL+"users")).timeout(java.time.Duration.ofSeconds(15)).GET().build(); HttpResponse<String> rp=httpClient.send(r,HttpResponse.BodyHandlers.ofString()); if(rp.statusCode()==200) return MAPPER.readValue(rp.body(), new TypeReference<List<User>>(){}); } catch (Exception e) {} return new ArrayList<>(); }); }
    public static CompletableFuture<List<ChatMessage>> getMessages(int uId, int fId) { /* (Giữ nguyên - Lớp 2) */
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest r = HttpRequest.newBuilder().uri(URI.create(BASE_URL + "messages/" + uId + "/" + fId)).timeout(java.time.Duration.ofSeconds(20)).GET().build();
                HttpResponse<String> rp = httpClient.send(r, HttpResponse.BodyHandlers.ofString());
                if (rp.statusCode() == 200) {
                    List<Map<String, Object>> dbResult = MAPPER.readValue(rp.body(), new TypeReference<List<Map<String, Object>>>(){});
                    List<ChatMessage> chatMessages = new ArrayList<>();
                    for (Map<String, Object> row : dbResult) {
                        ChatMessage msg = new ChatMessage();
                        msg.setId(((Number)row.get("id")).longValue());
                        msg.setSenderId((Integer)row.get("sender_id"));
                        msg.setMessageType((String)row.get("message_type"));
                        Object cipherObj = row.get("content_cipher");
                        if (cipherObj instanceof String) msg.setContent((String) cipherObj);
                        else if (cipherObj instanceof byte[]) msg.setContent(Base64.getEncoder().encodeToString((byte[]) cipherObj));
                        msg.setContentIv((String)row.get("content_iv"));
                        msg.setEncSessionKey((String)row.get("enc_session_key"));
                        Object tsObj = row.get("timestamp");
                        if (tsObj instanceof String) {
                             try { msg.setTimestamp(LocalDateTime.parse((String)tsObj, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S")).atZone(ZoneId.systemDefault())); }
                             catch (Exception e) { try { msg.setTimestamp(LocalDateTime.parse((String)tsObj, DateTimeFormatter.ISO_LOCAL_DATE_TIME).atZone(ZoneId.systemDefault())); } catch (Exception e2) { try { msg.setTimestamp(ZonedDateTime.parse((String)tsObj)); } catch (Exception e3) {} } }
                        }
                        chatMessages.add(msg);
                    }
                    return chatMessages;
                }
            } catch (Exception e) { e.printStackTrace(); }
            return new ArrayList<>();
        });
    }
    public static CompletableFuture<String> uploadFile(File file) { /* (Giữ nguyên) */ return CompletableFuture.supplyAsync(() -> { try { String b="----WebKitFormBoundary"+System.nanoTime(); var sb=new StringBuilder(); sb.append("--").append(b).append("\r\n").append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(file.getName()).append("\"\r\n"); String ct=Files.probeContentType(file.toPath()); if(ct==null) ct="application/octet-stream"; sb.append("Content-Type: ").append(ct).append("\r\n\r\n"); byte[] fb=Files.readAllBytes(file.toPath()); byte[] sp=sb.toString().getBytes(StandardCharsets.UTF_8); byte[] eb=("\r\n--"+b+"--\r\n").getBytes(StandardCharsets.UTF_8); byte[] body=new byte[sp.length+fb.length+eb.length]; System.arraycopy(sp,0,body,0,sp.length); System.arraycopy(fb,0,body,sp.length,fb.length); System.arraycopy(eb,0,body,sp.length+fb.length,eb.length); HttpRequest r=HttpRequest.newBuilder().uri(URI.create(BASE_URL+"upload")).header("Content-Type","multipart/form-data; boundary="+b).timeout(java.time.Duration.ofMinutes(2)).POST(HttpRequest.BodyPublishers.ofByteArray(body)).build(); HttpResponse<String> rp=httpClient.send(r,HttpResponse.BodyHandlers.ofString()); if(rp.statusCode()==200){JsonNode root=MAPPER.readTree(rp.body()); if(root.has("url")) return root.get("url").asText();} } catch (Exception e) {} return null; }); }
    
    // --- API Bạn bè (MỚI) ---
    public static CompletableFuture<List<User>> getFriends(int userId) { /* (Giữ nguyên) */ return CompletableFuture.supplyAsync(() -> { try { HttpRequest r = HttpRequest.newBuilder().uri(URI.create(BASE_URL + "friendships/" + userId + "/list")).timeout(java.time.Duration.ofSeconds(15)).GET().build(); HttpResponse<String> rp = httpClient.send(r, HttpResponse.BodyHandlers.ofString()); if (rp.statusCode() == 200) { List<Map<String, Object>> dbResult = MAPPER.readValue(rp.body(), new TypeReference<List<Map<String, Object>>>(){}); return dbResult.stream().map(row -> { User friend = new User(); friend.setId((Integer) row.get("friend_id")); friend.setUsername((String) row.get("username")); friend.setAvatar((String) row.get("avatar")); return friend; }).collect(Collectors.toList()); } } catch (Exception e) { e.printStackTrace(); } return new ArrayList<>(); }); }
    public static CompletableFuture<List<FriendRequest>> getFriendRequests(int userId) { /* (Giữ nguyên) */ return CompletableFuture.supplyAsync(() -> { try { HttpRequest r = HttpRequest.newBuilder().uri(URI.create(BASE_URL + "friendships/" + userId + "/requests")).timeout(java.time.Duration.ofSeconds(15)).GET().build(); HttpResponse<String> rp = httpClient.send(r, HttpResponse.BodyHandlers.ofString()); if (rp.statusCode() == 200) { return MAPPER.readValue(rp.body(), new TypeReference<List<FriendRequest>>(){}); } } catch (Exception e) { e.printStackTrace(); } return new ArrayList<>(); }); }
    public static CompletableFuture<Boolean> sendFriendRequest(int senderId, int receiverId) { /* (Giữ nguyên) */ return CompletableFuture.supplyAsync(() -> { try { Friendship req = new Friendship(senderId, receiverId, "pending", senderId); String jsonBody = req.toJson(); HttpRequest r = HttpRequest.newBuilder().uri(URI.create(BASE_URL + "friendships/request")).header("Content-Type", "application/json").timeout(java.time.Duration.ofSeconds(10)).POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build(); HttpResponse<String> rp = httpClient.send(r, HttpResponse.BodyHandlers.ofString()); return rp.statusCode() == 200; } catch (Exception e) { e.printStackTrace(); return false; } }); }
    public static CompletableFuture<Boolean> acceptFriendRequest(int senderId, int receiverId, int actionUserId) { /* (Giữ nguyên) */ return CompletableFuture.supplyAsync(() -> { try { Friendship req = new Friendship(senderId, receiverId, "accepted", actionUserId); String jsonBody = req.toJson(); HttpRequest r = HttpRequest.newBuilder().uri(URI.create(BASE_URL + "friendships/accept")).header("Content-Type", "application/json").timeout(java.time.Duration.ofSeconds(10)).PUT(HttpRequest.BodyPublishers.ofString(jsonBody)).build(); HttpResponse<String> rp = httpClient.send(r, HttpResponse.BodyHandlers.ofString()); return rp.statusCode() == 200; } catch (Exception e) { e.printStackTrace(); return false; } }); }
    public static CompletableFuture<Boolean> removeFriend(int userId1, int userId2) { /* (Giữ nguyên) */ return CompletableFuture.supplyAsync(() -> { try { Friendship req = new Friendship(userId1, userId2, null, 0); String jsonBody = req.toJson(); HttpRequest r = HttpRequest.newBuilder().uri(URI.create(BASE_URL + "friendships/remove")).header("Content-Type", "application/json").timeout(java.time.Duration.ofSeconds(10)).method("DELETE", HttpRequest.BodyPublishers.ofString(jsonBody)).build(); HttpResponse<String> rp = httpClient.send(r, HttpResponse.BodyHandlers.ofString()); return rp.statusCode() == 200; } catch (Exception e) { e.printStackTrace(); return false; } }); }

    // --- API Nhóm ---
    
    /** ✅ SỬA ĐỔI: API Tạo Nhóm (khớp với Batch 5) */
    public static CompletableFuture<Group> createGroup(String groupName, int creatorId, String avatarUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Backend (GroupController) nhận Map (snake_case)
                String jsonBody = MAPPER.writeValueAsString(Map.of(
                    "name", groupName, 
                    "creator_id", creatorId,
                    "avatar", (avatarUrl != null ? avatarUrl : "")
                ));
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "groups"))
                        .header("Content-Type", "application/json")
                        .timeout(java.time.Duration.ofSeconds(15))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 201) { // 201 Created
                    return MAPPER.readValue(response.body(), Group.class);
                } else {
                    System.err.println("Tạo nhóm thất bại: " + response.statusCode() + " | " + response.body());
                    return null;
                }
            } catch (Exception e) { 
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                e.printStackTrace(); 
                return null;
            }
        });
    }

    /** ✅ HÀM MỚI: API Thêm thành viên */
    public static CompletableFuture<Boolean> addMemberToGroup(int groupId, int memberId) {
         return CompletableFuture.supplyAsync(() -> {
            try {
                // Backend (GroupMemberController) nhận Map (camelCase)
                String jsonBody = SEND_MAPPER.writeValueAsString(Map.of(
                    "groupId", groupId, 
                    "memberId", memberId,
                    "role", "member"
                ));
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "group-members/add"))
                        .header("Content-Type", "application/json")
                        .timeout(java.time.Duration.ofSeconds(15))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                return response.statusCode() == 200;
            } catch (Exception e) { 
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                e.printStackTrace(); 
                return false;
            }
        });
    }

    /** ✅ HÀM MỚI: API Xóa nhóm */
    public static CompletableFuture<Boolean> deleteGroup(int groupId, int requesterId) {
         return CompletableFuture.supplyAsync(() -> {
            try {
                // Backend (GroupController) nhận Map (snake_case)
                String jsonBody = MAPPER.writeValueAsString(Map.of(
                    "user_id", requesterId
                ));
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "groups/" + groupId))
                        .header("Content-Type", "application/json")
                        .timeout(java.time.Duration.ofSeconds(15))
                        .method("DELETE", HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                return response.statusCode() == 200;
            } catch (Exception e) { 
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                e.printStackTrace(); 
                return false;
            }
        });
    }

    // (Các API Nhóm giả định cũ)
    public static CompletableFuture<List<Group>> getGroupsForUser(int userId) { /* (Giữ nguyên) */ String ep=BASE_URL+"groups/user/"+userId; System.out.println("WARN: API nhóm: GET "+ep); return CompletableFuture.supplyAsync(() -> { try { HttpRequest r=HttpRequest.newBuilder().uri(URI.create(ep)).timeout(java.time.Duration.ofSeconds(15)).GET().build(); HttpResponse<String> rp=httpClient.send(r,HttpResponse.BodyHandlers.ofString()); if(rp.statusCode()==200) return MAPPER.readValue(rp.body(), new TypeReference<List<Group>>(){}); } catch (Exception e) { e.printStackTrace(); } return new ArrayList<>(); }); }
    public static CompletableFuture<List<GroupMessage>> getGroupMessages(int groupId) { /* (Giữ nguyên) */ String ep=BASE_URL+"group-messages/"+groupId; System.out.println("WARN: API lịch sử nhóm: GET "+ep); return CompletableFuture.supplyAsync(() -> { try { HttpRequest r=HttpRequest.newBuilder().uri(URI.create(ep)).timeout(java.time.Duration.ofSeconds(20)).GET().build(); HttpResponse<String> rp=httpClient.send(r,HttpResponse.BodyHandlers.ofString()); if(rp.statusCode()==200) return MAPPER.readValue(rp.body(), new TypeReference<List<GroupMessage>>(){}); } catch (Exception e) { e.printStackTrace(); } return new ArrayList<>(); }); }
    public static CompletableFuture<List<GroupMember>> getGroupMembers(int groupId) { /* (Giữ nguyên) */ return CompletableFuture.supplyAsync(() -> { try { HttpRequest r=HttpRequest.newBuilder().uri(URI.create(BASE_URL+"group-members/"+groupId)).timeout(java.time.Duration.ofSeconds(15)).GET().build(); HttpResponse<String> rp=httpClient.send(r,HttpResponse.BodyHandlers.ofString()); if(rp.statusCode()==200) return MAPPER.readValue(rp.body(), new TypeReference<List<GroupMember>>(){}); } catch (Exception e) { e.printStackTrace(); } return new ArrayList<>(); }); }


    // --- WebSocket (Giữ nguyên) ---
    public static WebSocketClient connectWS(int userId, WSMessageListener listener) { /* (Giữ nguyên) */ URI u; try{u=new URI(WS_URL);}catch(Exception e){listener.onError(e); return null;} WebSocketClient wc=new WebSocketClient(u){ @Override public void onOpen(ServerHandshake h){ send("CONNECT\naccept-version:1.1,1.0\nheart-beat:10000,10000\n\n\0"); listener.setWebSocketClient(this); } @Override public void onMessage(String m){ if(m.startsWith("CONNECTED\n")){ String s="SUBSCRIBE\nid:sub-user-"+userId+"\ndestination:/queue/messages/"+userId+"\nack:auto\n\n\0"; send(s); listener.onStompConnected(); } else if(m.startsWith("MESSAGE\n")){ int hEnd=m.indexOf("\n\n"); if(hEnd!=-1){ String h=m.substring(0,hEnd); int bStart=hEnd+2; int bEnd=m.indexOf('\0',bStart); if(bEnd>bStart){ String b=m.substring(bStart,bEnd); listener.onMessageReceived(h,b); } } } else if(m.startsWith("ERROR\n")){System.err.println("STOMP ERROR:"+m);} else if(!m.trim().isEmpty()&&!m.equals("\n")){/* Ignore keep-alive */}} @Override public void onClose(int c, String r, boolean re){ listener.onClose(); if(c!=1000){ var t=new Timer(); t.schedule(new TimerTask(){public void run(){NetworkService.connectWS(userId,listener);}}, 5000); }} @Override public void onError(Exception ex){ listener.onError(ex); } }; try{wc.connect(); return wc;}catch(Exception e){listener.onError(e); return null;} }
    public interface WSMessageListener { /* (Giữ nguyên) */ void onStompConnected(); void onMessageReceived(String h, String b); void onClose(); void onError(Exception ex); void setWebSocketClient(WebSocketClient c); }
    
    // --- E2EE (RSA Key) API Methods ---
    public static CompletableFuture<PublicKey> getPublicKey(int fId) { /* (Giữ nguyên) */ if(KeyService.isPublicRSAKeyCached(fId))return CompletableFuture.completedFuture(KeyService.getPublicRSAKey(fId)); return CompletableFuture.supplyAsync(()->{ try{ HttpRequest r=HttpRequest.newBuilder().uri(URI.create(BASE_URL+"keys/"+fId)).timeout(java.time.Duration.ofSeconds(10)).GET().build(); HttpResponse<String> rp=httpClient.send(r,HttpResponse.BodyHandlers.ofString()); if(rp.statusCode()==200){ JsonNode root=MAPPER.readTree(rp.body()); if(root.has("public_key")){ String k=root.get("public_key").asText(); if(k!=null&&!k.isEmpty()){ PublicKey pk=CryptoService.stringToPublicKey(k); if(pk!=null){ KeyService.cachePublicRSAKey(fId,pk); return pk; } } } } }catch(Exception e){} return null; }); }
    public static CompletableFuture<Boolean> uploadPublicKey(int uId, String pkStr) { /* (Giữ nguyên) */ return CompletableFuture.supplyAsync(()->{ try{ String j=SEND_MAPPER.writeValueAsString(Map.of("publicKey",pkStr)); HttpRequest r=HttpRequest.newBuilder().uri(URI.create(BASE_URL+"keys/"+uId)).header("Content-Type","application/json").timeout(java.time.Duration.ofSeconds(15)).POST(HttpRequest.BodyPublishers.ofString(j)).build(); HttpResponse<String> rp=httpClient.send(r,HttpResponse.BodyHandlers.ofString()); return rp.statusCode()==200||rp.statusCode()==201; }catch(Exception e){ return false;} }); }
}
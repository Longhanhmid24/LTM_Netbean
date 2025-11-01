package model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * DTO (Data Transfer Object) để gửi các yêu cầu liên quan đến bạn bè
 * (Khớp với model Friendship.java của Backend).
 */
public class Friendship {

    // Gửi bằng CamelCase (khớp với DTO của Spring)
    private int userId1;
    private int userId2;
    private String status; // "pending", "accepted", "blocked"
    private int actionUserId;

    public Friendship(int userId1, int userId2, String status, int actionUserId) {
        this.userId1 = userId1;
        this.userId2 = userId2;
        this.status = status;
        this.actionUserId = actionUserId;
    }

    // Getters & Setters (Cần cho Jackson)
    public int getUserId1() { return userId1; }
    public void setUserId1(int userId1) { this.userId1 = userId1; }
    public int getUserId2() { return userId2; }
    public void setUserId2(int userId2) { this.userId2 = userId2; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getActionUserId() { return actionUserId; }
    public void setActionUserId(int actionUserId) { this.actionUserId = actionUserId; }
    
    // Mapper để gửi JSON (CamelCase)
    private static final ObjectMapper TO_JSON_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            // Mặc định là CamelCase

    public String toJson() {
        try {
            return TO_JSON_MAPPER.writeValueAsString(this);
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }
}
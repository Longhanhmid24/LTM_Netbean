package model;

public class CallSignal {
    private String type;        
    private int callerId;
    private int receiverId;
    private String sdp;         
    private String candidate;   
    private boolean encrypted;  
    private String payload;     
    private Long callId;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getCallerId() { return callerId; }
    public void setCallerId(int callerId) { this.callerId = callerId; }

    public int getReceiverId() { return receiverId; }
    public void setReceiverId(int receiverId) { this.receiverId = receiverId; }

    public String getSdp() { return sdp; }
    public void setSdp(String sdp) { this.sdp = sdp; }

    public String getCandidate() { return candidate; }
    public void setCandidate(String candidate) { this.candidate = candidate; }

    public boolean isEncrypted() { return encrypted; }
    public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public Long getCallId() { return callId; }
    public void setCallId(Long callId) { this.callId = callId; }

    public String getCallType() { return type; } // THÊM
    
    @Override
    public String toString() {
        return "CallSignal{" +
                "type='" + type + '\'' +
                ", callerId=" + callerId +
                ", receiverId=" + receiverId +
                ", sdp='" + sdp + '\'' +
                ", candidate='" + candidate + '\'' +
                ", encrypted=" + encrypted +
                ", callId=" + callId +
                '}';
    }
}

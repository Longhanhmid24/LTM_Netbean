package com.app.chatserver.call_video;

import com.app.chatserver.model.CallSignal;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class CallController {

    private final SimpMessagingTemplate messagingTemplate;

    public CallController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/call.send")
    public void sendSignal(CallSignal signal) {
        // Log để kiểm tra có mã hóa hay không
        if (signal.isEncrypted()) {
            System.out.println("[CallController] 🔐 Encrypted signal từ " + signal.getCallerId() +
                    " → " + signal.getReceiverId() +
                    " | Type: " + signal.getType());
        } else {
            System.out.println("[CallController] 🔁 Plain signal từ " + signal.getCallerId() +
                    " → " + signal.getReceiverId() +
                    " | Type: " + signal.getType());
        }

        // Server chỉ chuyển tiếp nguyên vẹn, KHÔNG giải mã
        messagingTemplate.convertAndSend("/queue/call/" + signal.getReceiverId(), signal);
    }
}

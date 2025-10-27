package com.app.chatserver.call_video;

import com.app.chatserver.model.CallSignal; // 🟢 Thêm dòng này
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
        System.out.println("[CallController] 🔁 Forward signal: " + signal);
        messagingTemplate.convertAndSend("/queue/call/" + signal.getReceiverId(), signal);
    }
}

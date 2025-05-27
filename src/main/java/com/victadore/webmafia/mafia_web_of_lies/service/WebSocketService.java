package com.victadore.webmafia.mafia_web_of_lies.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.victadore.webmafia.mafia_web_of_lies.dto.GameEvent;

@Service
public class WebSocketService {
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastGameUpdate(String gameCode, GameEvent event) {
        messagingTemplate.convertAndSend("/topic/game/" + gameCode, event);
    }

    public void sendPrivateMessage(String username, GameEvent event) {
        messagingTemplate.convertAndSendToUser(
            username,
            "/queue/private",
            event
        );
    }
}
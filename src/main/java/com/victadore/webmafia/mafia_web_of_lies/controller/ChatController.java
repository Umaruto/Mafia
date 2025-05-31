package com.victadore.webmafia.mafia_web_of_lies.controller;

import com.victadore.webmafia.mafia_web_of_lies.dto.ChatMessageRequest;
import com.victadore.webmafia.mafia_web_of_lies.dto.ChatMessageResponse;
import com.victadore.webmafia.mafia_web_of_lies.model.ChatMessage;
import com.victadore.webmafia.mafia_web_of_lies.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    @Autowired
    private ChatService chatService;
    
    /**
     * WebSocket endpoint for sending chat messages
     */
    @MessageMapping("/game/{gameCode}/chat")
    public void sendMessage(@DestinationVariable String gameCode, 
                           @Payload Map<String, Object> payload) {
        try {
            String username = (String) payload.get("username");
            String message = (String) payload.get("message");
            String chatType = (String) payload.get("chatType");
            String targetRole = (String) payload.get("targetRole");
            
            ChatMessageRequest request = new ChatMessageRequest();
            request.setMessage(message);
            request.setChatType(chatType != null ? 
                ChatMessage.ChatType.valueOf(chatType) : 
                ChatMessage.ChatType.PUBLIC);
            request.setTargetRole(targetRole);
            
            chatService.sendMessage(gameCode, username, request);
        } catch (Exception e) {
            System.err.println("Error processing chat message: " + e.getMessage());
            // TODO: Send error response back to client
        }
    }
    
    /**
     * REST endpoint to get chat messages for a player
     */
    @GetMapping("/{gameCode}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(
            @PathVariable String gameCode,
            @RequestParam String username) {
        try {
            List<ChatMessageResponse> messages = chatService.getMessagesForPlayer(gameCode, username);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * REST endpoint to get all public messages for a game
     */
    @GetMapping("/{gameCode}/public")
    public ResponseEntity<List<ChatMessageResponse>> getPublicMessages(
            @PathVariable String gameCode,
            @RequestParam String username) {
        try {
            List<ChatMessageResponse> messages = chatService.getPublicMessages(gameCode, username);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * REST endpoint to send a chat message (alternative to WebSocket)
     */
    @PostMapping("/{gameCode}/send")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable String gameCode,
            @RequestParam String username,
            @RequestBody ChatMessageRequest request) {
        try {
            ChatMessageResponse response = chatService.sendMessage(gameCode, username, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 
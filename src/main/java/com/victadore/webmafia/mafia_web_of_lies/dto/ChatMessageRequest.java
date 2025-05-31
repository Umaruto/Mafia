package com.victadore.webmafia.mafia_web_of_lies.dto;

import com.victadore.webmafia.mafia_web_of_lies.model.ChatMessage;

public class ChatMessageRequest {
    
    private String message;
    private ChatMessage.ChatType chatType = ChatMessage.ChatType.PUBLIC;
    private String targetRole; // For private messages
    
    // Constructors
    public ChatMessageRequest() {}
    
    public ChatMessageRequest(String message, ChatMessage.ChatType chatType) {
        this.message = message;
        this.chatType = chatType;
    }
    
    public ChatMessageRequest(String message, ChatMessage.ChatType chatType, String targetRole) {
        this.message = message;
        this.chatType = chatType;
        this.targetRole = targetRole;
    }
    
    // Getters and Setters
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public ChatMessage.ChatType getChatType() {
        return chatType;
    }
    
    public void setChatType(ChatMessage.ChatType chatType) {
        this.chatType = chatType;
    }
    
    public String getTargetRole() {
        return targetRole;
    }
    
    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }
} 
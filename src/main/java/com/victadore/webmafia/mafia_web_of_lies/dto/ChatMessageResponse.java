package com.victadore.webmafia.mafia_web_of_lies.dto;

import com.victadore.webmafia.mafia_web_of_lies.model.ChatMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatMessageResponse {
    
    private Long id;
    private String senderUsername;
    private String message;
    private String timestamp;
    private Integer day;
    private Integer phase;
    private ChatMessage.ChatType chatType;
    private String targetRole;
    private boolean isCurrentUser;
    
    // Constructor from ChatMessage entity
    public ChatMessageResponse(ChatMessage chatMessage, String currentUsername) {
        this.id = chatMessage.getId();
        this.senderUsername = chatMessage.getSenderUsername();
        this.message = chatMessage.getMessage();
        this.timestamp = chatMessage.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        this.day = chatMessage.getDay();
        this.phase = chatMessage.getPhase();
        this.chatType = chatMessage.getChatType();
        this.targetRole = chatMessage.getTargetRole();
        this.isCurrentUser = chatMessage.getSenderUsername().equals(currentUsername);
    }
    
    // Default constructor
    public ChatMessageResponse() {}
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getSenderUsername() {
        return senderUsername;
    }
    
    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public Integer getDay() {
        return day;
    }
    
    public void setDay(Integer day) {
        this.day = day;
    }
    
    public Integer getPhase() {
        return phase;
    }
    
    public void setPhase(Integer phase) {
        this.phase = phase;
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
    
    public boolean isCurrentUser() {
        return isCurrentUser;
    }
    
    public void setCurrentUser(boolean currentUser) {
        isCurrentUser = currentUser;
    }
} 
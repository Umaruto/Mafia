package com.victadore.webmafia.mafia_web_of_lies.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String gameCode;
    
    @Column(nullable = false)
    private String senderUsername;
    
    @Column(nullable = false, length = 1000)
    private String message;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "game_day", nullable = false)
    private Integer day;
    
    @Column(name = "game_phase", nullable = false)
    private Integer phase; // 0 = DAY, 1 = NIGHT
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatType chatType = ChatType.PUBLIC;
    
    @Column
    private String targetRole; // For private messages (e.g., "MAFIA")
    
    public enum ChatType {
        PUBLIC,    // Everyone can see (day phase)
        PRIVATE,   // Role-specific (e.g., Mafia night chat)
        SYSTEM     // System messages
    }
    
    // Constructors
    public ChatMessage() {}
    
    public ChatMessage(String gameCode, String senderUsername, String message, 
                      Integer day, Integer phase, ChatType chatType) {
        this.gameCode = gameCode;
        this.senderUsername = senderUsername;
        this.message = message;
        this.day = day;
        this.phase = phase;
        this.chatType = chatType;
        this.timestamp = LocalDateTime.now();
    }
    
    public ChatMessage(String gameCode, String senderUsername, String message, 
                      Integer day, Integer phase, ChatType chatType, String targetRole) {
        this(gameCode, senderUsername, message, day, phase, chatType);
        this.targetRole = targetRole;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getGameCode() {
        return gameCode;
    }
    
    public void setGameCode(String gameCode) {
        this.gameCode = gameCode;
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
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
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
    
    public ChatType getChatType() {
        return chatType;
    }
    
    public void setChatType(ChatType chatType) {
        this.chatType = chatType;
    }
    
    public String getTargetRole() {
        return targetRole;
    }
    
    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }
} 
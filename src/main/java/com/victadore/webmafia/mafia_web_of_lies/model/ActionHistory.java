package com.victadore.webmafia.mafia_web_of_lies.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(indexes = {
    @Index(name = "idx_action_history_game", columnList = "game_id"),
    @Index(name = "idx_action_history_timestamp", columnList = "timestamp"),
    @Index(name = "idx_action_history_actor", columnList = "actor_username"),
    @Index(name = "idx_action_history_phase", columnList = "game_day, game_phase")
})
public class ActionHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Game association is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;
    
    @NotBlank(message = "Action type is required")
    @Size(max = 50, message = "Action type cannot exceed 50 characters")
    @Column(nullable = false, length = 50)
    private String actionType;
    
    @NotBlank(message = "Actor username is required")
    @Size(max = 20, message = "Actor username cannot exceed 20 characters")
    @Column(nullable = false, length = 20)
    private String actorUsername;
    
    @Size(max = 20, message = "Target username cannot exceed 20 characters")
    @Column(length = 20)
    private String targetUsername;
    
    @NotNull(message = "Game day is required")
    @Column(nullable = false)
    private Integer gameDay;
    
    @NotNull(message = "Game phase is required")
    @Column(nullable = false)
    private Integer gamePhase; // 0 = Day, 1 = Night
    
    @NotNull(message = "Timestamp is required")
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Size(max = 1000, message = "Action details cannot exceed 1000 characters")
    @Column(length = 1000)
    private String actionDetails; // JSON or structured text with additional details
    
    @Size(max = 500, message = "Result cannot exceed 500 characters")
    @Column(length = 500)
    private String result; // Result of the action (success, failure, etc.)
    
    @NotNull(message = "Success status is required")
    @Column(nullable = false)
    private Boolean successful = true;
    
    // Additional fields for specific action types
    @Column
    private Boolean isSkipVote; // For vote actions
    
    @Column
    private Integer voteCount; // For elimination actions
    
    @Size(max = 100, message = "Additional data cannot exceed 100 characters")
    @Column(length = 100)
    private String additionalData; // For any extra information
} 
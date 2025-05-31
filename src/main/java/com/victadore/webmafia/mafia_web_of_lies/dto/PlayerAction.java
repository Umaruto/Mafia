package com.victadore.webmafia.mafia_web_of_lies.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerAction {
    @NotBlank(message = "Username is required")
    @Size(min = 2, max = 20, message = "Username must be between 2 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_\\-]+$", message = "Username can only contain letters, numbers, underscores, and hyphens")
    private String username;
    
    @NotBlank(message = "Action is required")
    @Pattern(regexp = "^(KILL|SAVE|INVESTIGATE|VOTE|SKIP_VOTE|CHAT)$", 
             message = "Action must be one of: KILL, SAVE, INVESTIGATE, VOTE, SKIP_VOTE, CHAT")
    private String action;
    
    // Target can be null for some actions like SKIP_VOTE
    @Size(max = 500, message = "Target/message cannot exceed 500 characters")
    private String target;
}
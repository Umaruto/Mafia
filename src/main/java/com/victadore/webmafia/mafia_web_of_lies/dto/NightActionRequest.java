package com.victadore.webmafia.mafia_web_of_lies.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NightActionRequest {
    @NotBlank(message = "Actor username is required")
    @Size(min = 2, max = 20, message = "Actor username must be between 2 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_\\-]+$", message = "Actor username can only contain letters, numbers, underscores, and hyphens")
    private String actorUsername;
    
    @NotBlank(message = "Target username is required")
    @Size(min = 2, max = 20, message = "Target username must be between 2 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_\\-]+$", message = "Target username can only contain letters, numbers, underscores, and hyphens")
    private String targetUsername;
    
    @NotBlank(message = "Action type is required")
    @Pattern(regexp = "^(KILL|SAVE|INVESTIGATE)$", message = "Action type must be KILL, SAVE, or INVESTIGATE")
    private String actionType;
}
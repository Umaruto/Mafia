package com.victadore.webmafia.mafia_web_of_lies.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GameCreateRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 2, max = 20, message = "Username must be between 2 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username can only contain letters, numbers, underscores, and hyphens")
    private String createdBy;
    
    @Size(min = 4, max = 15, message = "Minimum players must be between 4 and 15")
    private Integer minPlayers = 4;
    
    @Size(min = 4, max = 15, message = "Maximum players must be between 4 and 15")
    private Integer maxPlayers = 15;
} 
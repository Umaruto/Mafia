package com.victadore.webmafia.mafia_web_of_lies.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GameJoinRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 2, max = 20, message = "Username must be between 2 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username can only contain letters, numbers, underscores, and hyphens")
    private String username;
    
    @NotBlank(message = "Game code is required")
    @Size(min = 6, max = 6, message = "Game code must be exactly 6 characters")
    @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Game code must contain only uppercase letters and numbers")
    private String gameCode;
} 
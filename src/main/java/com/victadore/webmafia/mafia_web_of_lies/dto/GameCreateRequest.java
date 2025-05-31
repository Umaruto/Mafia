package com.victadore.webmafia.mafia_web_of_lies.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

@Data
public class GameCreateRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 2, max = 20, message = "Username must be between 2 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_\\-]+$", message = "Username can only contain letters, numbers, underscores, and hyphens")
    private String createdBy;
    
    @Min(value = 4, message = "Minimum players must be at least 4")
    @Max(value = 15, message = "Minimum players cannot exceed 15")
    private Integer minPlayers = 4;
    
    @Min(value = 4, message = "Maximum players must be at least 4")
    @Max(value = 15, message = "Maximum players cannot exceed 15")
    private Integer maxPlayers = 15;
} 
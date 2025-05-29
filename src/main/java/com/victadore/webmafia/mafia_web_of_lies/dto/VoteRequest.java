package com.victadore.webmafia.mafia_web_of_lies.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VoteRequest {
    @NotBlank(message = "Voter username is required")
    @Size(min = 2, max = 20, message = "Voter username must be between 2 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Voter username can only contain letters, numbers, underscores, and hyphens")
    private String voterUsername;
    
    // Can be null when skip is true
    @Size(min = 2, max = 20, message = "Target username must be between 2 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Target username can only contain letters, numbers, underscores, and hyphens")
    private String targetUsername;
    
    @NotNull(message = "Skip status must be specified")
    private Boolean skip;
    
    // Custom validation to ensure either skip is true OR targetUsername is provided
    public boolean isValid() {
        if (skip != null && skip) {
            return true; // Skip vote is valid regardless of target
        }
        return targetUsername != null && !targetUsername.trim().isEmpty();
    }
}
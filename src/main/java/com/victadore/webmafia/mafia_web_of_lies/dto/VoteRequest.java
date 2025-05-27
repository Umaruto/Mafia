package com.victadore.webmafia.mafia_web_of_lies.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VoteRequest {
    @NotBlank(message = "Voter username is required")
    private String voterUsername;
    
    private String targetUsername;  // Can be null or "SKIP"
    
    @NotNull(message = "Skip status must be specified")
    private boolean skip;          // New field to explicitly indicate skip
}
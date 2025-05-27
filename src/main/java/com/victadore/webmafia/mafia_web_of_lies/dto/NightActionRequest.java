package com.victadore.webmafia.mafia_web_of_lies.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NightActionRequest {
    @NotBlank(message = "Actor username is required")
    private String actorUsername;
    
    @NotBlank(message = "Target username is required")
    private String targetUsername;
    
    @NotBlank(message = "Action type is required")
    private String actionType;
}
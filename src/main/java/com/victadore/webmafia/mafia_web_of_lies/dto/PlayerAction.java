package com.victadore.webmafia.mafia_web_of_lies.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerAction {
    private String username;
    private String action;
    private String target;
}
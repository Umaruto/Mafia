package com.victadore.webmafia.mafia_web_of_lies.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GameEvent {
    private String type;
    private String gameCode;
    private Object payload;
}
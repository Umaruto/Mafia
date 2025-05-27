package com.victadore.webmafia.mafia_web_of_lies.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import com.victadore.webmafia.mafia_web_of_lies.service.WebSocketService;
import com.victadore.webmafia.mafia_web_of_lies.service.GameLogicService;
import com.victadore.webmafia.mafia_web_of_lies.dto.GameEvent;
import com.victadore.webmafia.mafia_web_of_lies.dto.PlayerAction;

@Controller
public class GameWebSocketController {
    private final WebSocketService webSocketService;
    private final GameLogicService gameLogicService;

    public GameWebSocketController(WebSocketService webSocketService, 
                                 GameLogicService gameLogicService) {
        this.webSocketService = webSocketService;
        this.gameLogicService = gameLogicService;
    }

    @MessageMapping("/game/{gameCode}/action")
    @SendTo("/topic/game/{gameCode}")
    public GameEvent handlePlayerAction(@DestinationVariable String gameCode,
                                      @Payload PlayerAction action) {
        // Process the action based on its type
        switch (action.getAction()) {
            case "KILL":
                gameLogicService.handleNightAction(gameCode, action.getUsername(), action.getTarget(), "KILL");
                break;
            case "SAVE":
                gameLogicService.handleNightAction(gameCode, action.getUsername(), action.getTarget(), "SAVE");
                break;
            case "INVESTIGATE":
                gameLogicService.handleNightAction(gameCode, action.getUsername(), action.getTarget(), "INVESTIGATE");
                break;
            case "VOTE":
                // Handle voting
                break;
            case "CHAT":
                // Handle chat messages
                break;
        }
        
        return new GameEvent("ACTION", gameCode, action);
    }
}
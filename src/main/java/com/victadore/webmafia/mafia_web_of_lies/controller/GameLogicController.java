package com.victadore.webmafia.mafia_web_of_lies.controller;

import com.victadore.webmafia.mafia_web_of_lies.model.Game;
import com.victadore.webmafia.mafia_web_of_lies.service.GameLogicService;

import jakarta.validation.Valid;


import com.victadore.webmafia.mafia_web_of_lies.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/game-logic")
public class GameLogicController {
    private final GameLogicService gameLogicService;

    public GameLogicController(GameLogicService gameLogicService) {
        this.gameLogicService = gameLogicService;
    }

    @PostMapping("/{gameCode}/day")
    public ResponseEntity<Game> startDayPhase(@PathVariable String gameCode) {
        Game game = gameLogicService.startDayPhase(gameCode);
        return ResponseEntity.ok(game);
    }

    @PostMapping("/{gameCode}/night")
    public ResponseEntity<Game> startNightPhase(@PathVariable String gameCode) {
        Game game = gameLogicService.startNightPhase(gameCode);
        return ResponseEntity.ok(game);
    }

    @PostMapping("/{gameCode}/advance-phase")
    public ResponseEntity<Game> advancePhase(@PathVariable String gameCode) {
        try {
            Game game = gameLogicService.advancePhase(gameCode);
            return ResponseEntity.ok(game);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{gameCode}/night-action")
    public ResponseEntity<Game> handleNightAction(
            @PathVariable String gameCode,
            @Valid @RequestBody NightActionRequest request) {
        Game game = gameLogicService.handleNightAction(
            gameCode, 
            request.getActorUsername(), 
            request.getTargetUsername(), 
            request.getActionType()
        );
        return ResponseEntity.ok(game);
    }

    @PostMapping("/{gameCode}/vote")
    public ResponseEntity<Game> handleVote(
            @PathVariable String gameCode,
            @Valid @RequestBody VoteRequest voteRequest) {
        Game game = gameLogicService.handleVote(gameCode, voteRequest);
        return ResponseEntity.ok(game);
    }

    @GetMapping("/{gameCode}/vote-status")
    public ResponseEntity<Map<String, String>> getVotingStatus(@PathVariable String gameCode) {
        Map<String, String> status = gameLogicService.getVotingStatus(gameCode);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/{gameCode}/has-voted/{username}")
    public ResponseEntity<Boolean> hasPlayerVoted(
            @PathVariable String gameCode,
            @PathVariable String username) {
        boolean hasVoted = gameLogicService.hasPlayerVoted(gameCode, username);
        return ResponseEntity.ok(hasVoted);
    }
}
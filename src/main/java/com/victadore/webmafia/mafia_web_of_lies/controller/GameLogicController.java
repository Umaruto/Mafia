package com.victadore.webmafia.mafia_web_of_lies.controller;

import com.victadore.webmafia.mafia_web_of_lies.model.Game;
import com.victadore.webmafia.mafia_web_of_lies.service.GameLogicService;
import com.victadore.webmafia.mafia_web_of_lies.service.GameService;
import com.victadore.webmafia.mafia_web_of_lies.service.ValidationService;
import com.victadore.webmafia.mafia_web_of_lies.exception.ValidationException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.victadore.webmafia.mafia_web_of_lies.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/game-logic")
public class GameLogicController {
    private final GameLogicService gameLogicService;
    private final GameService gameService;
    private final ValidationService validationService;

    public GameLogicController(GameLogicService gameLogicService, GameService gameService, ValidationService validationService) {
        this.gameLogicService = gameLogicService;
        this.gameService = gameService;
        this.validationService = validationService;
    }

    @PostMapping("/{gameCode}/day")
    public ResponseEntity<Game> startDayPhase(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode) {
        validationService.validateGameCode(gameCode);
        Game game = gameLogicService.startDayPhase(gameCode);
        return ResponseEntity.ok(game);
    }

    @PostMapping("/{gameCode}/night")
    public ResponseEntity<Game> startNightPhase(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode) {
        validationService.validateGameCode(gameCode);
        Game game = gameLogicService.startNightPhase(gameCode);
        return ResponseEntity.ok(game);
    }

    @PostMapping("/{gameCode}/advance-phase")
    public ResponseEntity<Game> advancePhase(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode) {
        try {
            validationService.validateGameCode(gameCode);
            Game game = gameLogicService.advancePhase(gameCode);
            return ResponseEntity.ok(game);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{gameCode}/night-action")
    public ResponseEntity<Game> handleNightAction(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode,
            @Valid @RequestBody NightActionRequest request) {
        validationService.validateGameCode(gameCode);
        
        // Get game to perform business validation
        Game game = gameService.getGameByCode(gameCode);
        validationService.validateNightAction(game, request.getActorUsername(), request.getActionType());
        
        Game updatedGame = gameLogicService.handleNightAction(
            gameCode, 
            request.getActorUsername(), 
            request.getTargetUsername(), 
            request.getActionType()
        );
        return ResponseEntity.ok(updatedGame);
    }

    @PostMapping("/{gameCode}/vote")
    public ResponseEntity<Game> handleVote(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode,
            @Valid @RequestBody VoteRequest voteRequest) {
        validationService.validateGameCode(gameCode);
        
        // Get game to perform business validation
        Game game = gameService.getGameByCode(gameCode);
        validationService.validateVoteRequest(voteRequest, game);
        
        Game updatedGame = gameLogicService.handleVote(gameCode, voteRequest);
        return ResponseEntity.ok(updatedGame);
    }

    @GetMapping("/{gameCode}/vote-status")
    public ResponseEntity<Map<String, String>> getVotingStatus(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode) {
        validationService.validateGameCode(gameCode);
        Map<String, String> status = gameLogicService.getVotingStatus(gameCode);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/{gameCode}/has-voted/{username}")
    public ResponseEntity<Boolean> hasPlayerVoted(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode,
            @PathVariable @Size(min = 2, max = 20, message = "Invalid username length") String username) {
        validationService.validateGameCode(gameCode);
        validationService.validateUsername(username);
        
        boolean hasVoted = gameLogicService.hasPlayerVoted(gameCode, username);
        return ResponseEntity.ok(hasVoted);
    }
}
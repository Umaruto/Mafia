package com.victadore.webmafia.mafia_web_of_lies.controller;

import com.victadore.webmafia.mafia_web_of_lies.model.Game;
import com.victadore.webmafia.mafia_web_of_lies.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/games")
public class GameController {
    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    public ResponseEntity<Game> createGame(@RequestParam String createdBy) {
        Game game = gameService.createGame(createdBy);
        return ResponseEntity.ok(game);
    }

    @PostMapping("/{gameCode}/join")
    public ResponseEntity<Game> joinGame(
            @PathVariable String gameCode,
            @RequestParam String username) {
        Game game = gameService.joinGame(gameCode, username);
        return ResponseEntity.ok(game);
    }

    @PostMapping("/{gameCode}/start")
    public ResponseEntity<Game> startGame(@PathVariable String gameCode) {
        Game game = gameService.startGame(gameCode);
        return ResponseEntity.ok(game);
    }
}
package com.victadore.webmafia.mafia_web_of_lies.controller;

import com.victadore.webmafia.mafia_web_of_lies.model.Player;
import com.victadore.webmafia.mafia_web_of_lies.model.Role;
import com.victadore.webmafia.mafia_web_of_lies.service.PlayerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
public class PlayerController {
    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/game/{gameId}/alive")
    public ResponseEntity<List<Player>> getAlivePlayers(@PathVariable Long gameId) {
        List<Player> players = playerService.getAlivePlayers(gameId);
        return ResponseEntity.ok(players);
    }

    @GetMapping("/game/{gameId}/role/{role}")
    public ResponseEntity<List<Player>> getPlayersByRole(
            @PathVariable Long gameId,
            @PathVariable Role role) {
        List<Player> players = playerService.getPlayersByRole(gameId, role);
        return ResponseEntity.ok(players);
    }

    @PostMapping("/{playerId}/kill")
    public ResponseEntity<Void> killPlayer(@PathVariable Long playerId) {
        playerService.killPlayer(playerId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{playerId}/save")
    public ResponseEntity<Void> savePlayer(@PathVariable Long playerId) {
        playerService.savePlayer(playerId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{playerId}/investigate")
    public ResponseEntity<Boolean> investigatePlayer(@PathVariable Long playerId) {
        boolean isDetective = playerService.investigatePlayer(playerId);
        return ResponseEntity.ok(isDetective);
    }

    @GetMapping("/{playerId}/alive")
    public ResponseEntity<Boolean> isPlayerAlive(@PathVariable Long playerId) {
        boolean isAlive = playerService.isPlayerAlive(playerId);
        return ResponseEntity.ok(isAlive);
    }
}
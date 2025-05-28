package com.victadore.webmafia.mafia_web_of_lies.controller;

import com.victadore.webmafia.mafia_web_of_lies.exception.*;
import com.victadore.webmafia.mafia_web_of_lies.model.*;
import com.victadore.webmafia.mafia_web_of_lies.service.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/api/games")
public class GameController {
    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<?> createGame(@RequestParam String createdBy) {
        try {
            Game game = gameService.createGame(createdBy);
            Map<String, Object> response = new HashMap<>();
            response.put("gameCode", game.getGameCode());
            response.put("message", "Game created successfully");
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
        } catch (GameException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
        }
    }

    @GetMapping("/game/{gameCode}")
    public String gamePage(@PathVariable String gameCode, 
                        @RequestParam String username,
                        Model model) {
        Game game = gameService.getGameByCode(gameCode);
        if (game == null) {
            throw new GameException("Game not found");
        }
        
        model.addAttribute("gameCode", gameCode);
        model.addAttribute("username", username);
        return "game";
    }

    @PostMapping("/{gameCode}/join")
    @ResponseBody
    public ResponseEntity<?> joinGame(@PathVariable String gameCode, @RequestParam String username) {
        try {
            Game game = gameService.joinGame(gameCode, username);
            Map<String, Object> response = new HashMap<>();
            response.put("gameCode", game.getGameCode());
            response.put("message", "Successfully joined the game");
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
        } catch (GameException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
        }
    }

    @GetMapping("/{gameCode}/waiting-room")
    public String waitingRoom(@PathVariable String gameCode, 
                            @RequestParam String username,
                            Model model) {
        Game game = gameService.getGameByCode(gameCode);
        if (game == null) {
            throw new GameException("Game not found");
        }
        
        model.addAttribute("gameCode", gameCode);
        model.addAttribute("username", username);
        model.addAttribute("isCreator", game.getCreatedBy().equals(username));
        model.addAttribute("playerCount", game.getPlayers().size());
        model.addAttribute("minPlayers", game.getMinPlayers());
        model.addAttribute("maxPlayers", game.getMaxPlayers());
        
        return "waiting-room";
    }

    @GetMapping("/{gameCode}/players")
    @ResponseBody
    public ResponseEntity<List<Map<String, String>>> getPlayers(@PathVariable String gameCode) {
        Game game = gameService.getGameByCode(gameCode);
        if (game == null) {
            throw new GameException("Game not found");
        }
        
        List<Map<String, String>> players = game.getPlayers().stream()
            .map(player -> {
                Map<String, String> playerInfo = new HashMap<>();
                playerInfo.put("username", player.getUsername());
                return playerInfo;
            })
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(players);
    }

    @PostMapping("/{gameCode}/start")
    @ResponseBody
    public ResponseEntity<?> startGame(@PathVariable String gameCode, @RequestParam String username) {
        try {
            Game game = gameService.getGameByCode(gameCode);
            if (!game.getCreatedBy().equals(username)) {
                throw new GameException("Only the game creator can start the game");
            }
            
            gameService.startGame(gameCode);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Game started successfully");
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
        } catch (GameException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
        }
    }

    @GetMapping("/{gameCode}/player/{username}/role")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getPlayerRole(@PathVariable String gameCode, @PathVariable String username) {
        try {
            Game game = gameService.getGameByCode(gameCode);
            Player player = game.getPlayers().stream()
                .filter(p -> p.getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new GameException("Player not found in game"));
            
            Map<String, String> roleInfo = new HashMap<>();
            roleInfo.put("role", player.getRole().toString());
            roleInfo.put("description", getRoleDescription(player.getRole()));
            
            return ResponseEntity.ok(roleInfo);
        } catch (GameException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    private String getRoleDescription(Role role) {
        switch (role) {
            case MAFIA:
                return "You are part of the Mafia. Your goal is to eliminate all citizens. You can kill one player each night.";
            case DOCTOR:
                return "You are the Doctor. You can save one player from death each night, including yourself.";
            case DETECTIVE:
                return "You are the Detective. You can investigate one player each night to learn their role.";
            case CITIZEN:
                return "You are a Citizen. Your goal is to identify and eliminate all Mafia members through voting.";
            default:
                return "Unknown role";
        }
    }

    @GetMapping("/{gameCode}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getGameInfo(@PathVariable String gameCode) {
        try {
            Game game = gameService.getGameByCode(gameCode);
            Map<String, Object> gameInfo = new HashMap<>();
            gameInfo.put("gameCode", game.getGameCode());
            gameInfo.put("gameState", game.getGameState().toString());
            gameInfo.put("currentDay", game.getCurrentDay());
            gameInfo.put("currentPhase", game.getCurrentPhase());
            gameInfo.put("playerCount", game.getPlayers().size());
            gameInfo.put("minPlayers", game.getMinPlayers());
            gameInfo.put("maxPlayers", game.getMaxPlayers());
            
            return ResponseEntity.ok(gameInfo);
        } catch (GameException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
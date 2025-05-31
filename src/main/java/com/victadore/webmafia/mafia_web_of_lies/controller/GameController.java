package com.victadore.webmafia.mafia_web_of_lies.controller;

import com.victadore.webmafia.mafia_web_of_lies.exception.*;
import com.victadore.webmafia.mafia_web_of_lies.model.*;
import com.victadore.webmafia.mafia_web_of_lies.service.*;
import com.victadore.webmafia.mafia_web_of_lies.dto.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/api/games")
public class GameController {
    private final GameService gameService;
    private final ValidationService validationService;

    public GameController(GameService gameService, ValidationService validationService) {
        this.gameService = gameService;
        this.validationService = validationService;
    }

    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createGame(@Valid @RequestBody GameCreateRequest request) {
        try {
            // Additional business validation
            validationService.validateGameCreation(request.getCreatedBy(), request.getMinPlayers(), request.getMaxPlayers());
            
            Game game = gameService.createGame(request.getCreatedBy());
            Map<String, Object> response = new HashMap<>();
            response.put("gameCode", game.getGameCode());
            response.put("message", "Game created successfully");
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
        } catch (ValidationException e) {
            throw e; // Let global exception handler deal with it
        } catch (GameException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
        }
    }

    // Backup endpoint for form-based creation (for existing frontend compatibility)
    @PostMapping("/legacy")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createGameLegacy(@RequestParam String createdBy) {
        try {
            validationService.validateUsername(createdBy);
            validationService.validateGameCreation(createdBy, 4, 15);
            
            Game game = gameService.createGame(createdBy);
            Map<String, Object> response = new HashMap<>();
            response.put("gameCode", game.getGameCode());
            response.put("message", "Game created successfully");
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
        } catch (ValidationException e) {
            throw e;
        } catch (GameException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
        }
    }

    @GetMapping("/game/{gameCode}")
    public String gamePage(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode, 
            @RequestParam @Size(min = 2, max = 20, message = "Invalid username length") String username,
            Model model) {
        try {
            validationService.validateGameCode(gameCode);
            validationService.validateUsername(username);
            
            Game game = gameService.getGameByCode(gameCode);
            model.addAttribute("gameCode", gameCode);
            model.addAttribute("username", username);
            return "game";
        } catch (ValidationException e) {
            throw new GameException("Invalid parameters: " + e.getMessage());
        }
    }

    @PostMapping("/join")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> joinGameGeneral(@Valid @RequestBody GameJoinRequest request) {
        try {
            validationService.validateGameCode(request.getGameCode());
            validationService.validateUsername(request.getUsername());
            
            Game game = gameService.joinGame(request.getGameCode(), request.getUsername());
            Map<String, Object> response = new HashMap<>();
            response.put("gameCode", game.getGameCode());
            response.put("message", "Successfully joined the game");
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
        } catch (ValidationException e) {
            throw e;
        } catch (GameException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
        }
    }

    @GetMapping("/{gameCode}/waiting-room")
    public String waitingRoom(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode, 
            @RequestParam @Size(min = 2, max = 20, message = "Invalid username length") String username,
            Model model) {
        try {
            validationService.validateGameCode(gameCode);
            validationService.validateUsername(username);
            
            Game game = gameService.getGameByCode(gameCode);
            
            model.addAttribute("gameCode", gameCode);
            model.addAttribute("username", username);
            model.addAttribute("isCreator", game.getCreatedBy().equals(username));
            model.addAttribute("playerCount", game.getPlayers().size());
            model.addAttribute("minPlayers", game.getMinPlayers());
            model.addAttribute("maxPlayers", game.getMaxPlayers());
            
            return "waiting-room";
        } catch (ValidationException e) {
            throw new GameException("Invalid parameters: " + e.getMessage());
        }
    }

    @GetMapping("/{gameCode}/players")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getPlayers(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode) {
        try {
            validationService.validateGameCode(gameCode);
            
            Game game = gameService.getGameByCode(gameCode);
            
            List<Map<String, Object>> players = game.getPlayers().stream()
                .map(player -> {
                    Map<String, Object> playerInfo = new HashMap<>();
                    playerInfo.put("username", player.getUsername());
                    playerInfo.put("alive", player.isAlive());
                    return playerInfo;
                })
                .collect(Collectors.toList());
                
            return ResponseEntity.ok(players);
        } catch (ValidationException e) {
            throw e;
        }
    }

    @PostMapping("/{gameCode}/start")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> startGame(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode, 
            @RequestParam @Size(min = 2, max = 20, message = "Invalid username length") String username) {
        try {
            validationService.validateGameCode(gameCode);
            validationService.validateUsername(username);
            
            Game game = gameService.getGameByCode(gameCode);
            if (!game.getCreatedBy().equals(username)) {
                throw new GameException("Only the game creator can start the game");
            }
            
            gameService.startGame(gameCode);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Game started successfully");
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
        } catch (ValidationException e) {
            throw e;
        } catch (GameException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
        }
    }

    @GetMapping("/{gameCode}/player/{username}/role")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getPlayerRole(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode, 
            @PathVariable @Size(min = 2, max = 20, message = "Invalid username length") String username) {
        try {
            validationService.validateGameCode(gameCode);
            validationService.validateUsername(username);
            
            Game game = gameService.getGameByCode(gameCode);
            Player player = game.getPlayers().stream()
                .filter(p -> p.getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new GameException("Player not found in game"));
            
            Map<String, String> roleInfo = new HashMap<>();
            roleInfo.put("role", player.getRole().toString());
            roleInfo.put("description", getRoleDescription(player.getRole()));
            
            return ResponseEntity.ok(roleInfo);
        } catch (ValidationException e) {
            throw e;
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
    public ResponseEntity<Map<String, Object>> getGameInfo(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode) {
        try {
            validationService.validateGameCode(gameCode);
            
            Game game = gameService.getGameByCode(gameCode);
            Map<String, Object> gameInfo = new HashMap<>();
            gameInfo.put("gameCode", game.getGameCode());
            gameInfo.put("gameState", game.getGameState().toString());
            gameInfo.put("currentDay", game.getCurrentDay());
            gameInfo.put("currentPhase", game.getCurrentPhase());
            gameInfo.put("playerCount", game.getPlayers().size());
            gameInfo.put("minPlayers", game.getMinPlayers());
            gameInfo.put("maxPlayers", game.getMaxPlayers());
            gameInfo.put("winner", game.getWinner());
            
            return ResponseEntity.ok(gameInfo);
        } catch (ValidationException e) {
            throw e;
        } catch (GameException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{gameCode}/players-with-roles")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getPlayersWithRoles(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode) {
        try {
            validationService.validateGameCode(gameCode);
            
            Game game = gameService.getGameByCode(gameCode);
            
            // Only show roles if game is finished
            if (game.getGameState() != GameState.FINISHED) {
                throw new GameException("Game must be finished to view all roles");
            }
            
            List<Map<String, Object>> players = game.getPlayers().stream()
                .map(player -> {
                    Map<String, Object> playerInfo = new HashMap<>();
                    playerInfo.put("username", player.getUsername());
                    playerInfo.put("role", player.getRole().toString());
                    playerInfo.put("alive", player.isAlive());
                    return playerInfo;
                })
                .collect(Collectors.toList());
                
            return ResponseEntity.ok(players);
        } catch (ValidationException e) {
            throw e;
        } catch (GameException e) {
            List<Map<String, Object>> error = new ArrayList<>();
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("message", e.getMessage());
            error.add(errorMap);
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{gameCode}/dead-players-roles")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getDeadPlayersWithRoles(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode) {
        try {
            validationService.validateGameCode(gameCode);
            
            Game game = gameService.getGameByCode(gameCode);
            
            List<Map<String, Object>> deadPlayers = game.getPlayers().stream()
                .filter(player -> !player.isAlive())
                .map(player -> {
                    Map<String, Object> playerInfo = new HashMap<>();
                    playerInfo.put("username", player.getUsername());
                    playerInfo.put("role", player.getRole().toString());
                    playerInfo.put("alive", player.isAlive());
                    return playerInfo;
                })
                .collect(Collectors.toList());
                
            return ResponseEntity.ok(deadPlayers);
        } catch (ValidationException e) {
            throw e;
        } catch (GameException e) {
            List<Map<String, Object>> error = new ArrayList<>();
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("message", e.getMessage());
            error.add(errorMap);
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{gameCode}/mafia-team/{username}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getMafiaTeam(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode, 
            @PathVariable @Size(min = 2, max = 20, message = "Invalid username length") String username) {
        try {
            validationService.validateGameCode(gameCode);
            validationService.validateUsername(username);
            
            Game game = gameService.getGameByCode(gameCode);
            
            // First verify that the requesting player is Mafia
            Player requestingPlayer = game.getPlayers().stream()
                .filter(p -> p.getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new GameException("Player not found in game"));
            
            if (requestingPlayer.getRole() != Role.MAFIA) {
                throw new GameException("Only Mafia members can view the Mafia team");
            }
            
            // Get all Mafia members
            List<Map<String, Object>> mafiaMembers = game.getPlayers().stream()
                .filter(player -> player.getRole() == Role.MAFIA)
                .map(player -> {
                    Map<String, Object> memberInfo = new HashMap<>();
                    memberInfo.put("username", player.getUsername());
                    memberInfo.put("alive", player.isAlive());
                    memberInfo.put("isYou", player.getUsername().equals(username));
                    return memberInfo;
                })
                .collect(Collectors.toList());
                
            return ResponseEntity.ok(mafiaMembers);
        } catch (ValidationException e) {
            throw e;
        } catch (GameException e) {
            List<Map<String, Object>> error = new ArrayList<>();
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("message", e.getMessage());
            error.add(errorMap);
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{gameCode}/mafia-votes/{username}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getMafiaVotes(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode, 
            @PathVariable @Size(min = 2, max = 20, message = "Invalid username length") String username) {
        try {
            validationService.validateGameCode(gameCode);
            validationService.validateUsername(username);
            
            Game game = gameService.getGameByCode(gameCode);
            
            // Verify that the requesting player is Mafia
            Player requestingPlayer = game.getPlayers().stream()
                .filter(p -> p.getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new GameException("Player not found in game"));
            
            if (requestingPlayer.getRole() != Role.MAFIA) {
                throw new GameException("Only Mafia members can view Mafia votes");
            }
            
            List<Map<String, Object>> votingStatus = new ArrayList<>();
            
            game.getPlayers().stream()
                .filter(player -> player.getRole() == Role.MAFIA && player.isAlive())
                .forEach(mafiaPlayer -> {
                    Map<String, Object> memberStatus = new HashMap<>();
                    memberStatus.put("username", mafiaPlayer.getUsername());
                    memberStatus.put("isYou", mafiaPlayer.getUsername().equals(username));
                    
                    Long targetId = game.getMafiaVotes().get(mafiaPlayer.getId());
                    if (targetId != null) {
                        String targetName = game.getPlayers().stream()
                            .filter(p -> p.getId().equals(targetId))
                            .map(Player::getUsername)
                            .findFirst()
                            .orElse("Unknown");
                        memberStatus.put("hasVoted", true);
                        memberStatus.put("target", targetName);
                        memberStatus.put("status", "Voted for " + targetName);
                    } else {
                        memberStatus.put("hasVoted", false);
                        memberStatus.put("target", null);
                        memberStatus.put("status", "Not voted yet");
                    }
                    
                    votingStatus.add(memberStatus);
                });
                
            return ResponseEntity.ok(votingStatus);
        } catch (ValidationException e) {
            throw e;
        } catch (GameException e) {
            List<Map<String, Object>> error = new ArrayList<>();
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("message", e.getMessage());
            error.add(errorMap);
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{gameCode}/investigation-result/{investigatorUsername}/{targetUsername}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getInvestigationResult(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode,
            @PathVariable @Size(min = 2, max = 20, message = "Invalid username length") String investigatorUsername,
            @PathVariable @Size(min = 2, max = 20, message = "Invalid username length") String targetUsername) {
        try {
            validationService.validateGameCode(gameCode);
            validationService.validateUsername(investigatorUsername);
            validationService.validateUsername(targetUsername);
            
            Game game = gameService.getGameByCode(gameCode);
            
            // Verify investigator is detective
            Player investigator = game.getPlayers().stream()
                .filter(p -> p.getUsername().equals(investigatorUsername))
                .findFirst()
                .orElseThrow(() -> new GameException("Investigator not found"));
            
            if (investigator.getRole() != Role.DETECTIVE) {
                throw new GameException("Only detectives can view investigation results");
            }
            
            Player target = game.getPlayers().stream()
                .filter(p -> p.getUsername().equals(targetUsername))
                .findFirst()
                .orElseThrow(() -> new GameException("Target not found"));
            
            boolean isMafia = target.getRole() == Role.MAFIA;
            String resultMessage = isMafia ? 
                targetUsername + " is MAFIA! They are your enemy." :
                targetUsername + " is INNOCENT. They are not Mafia.";
            
            Map<String, Object> result = new HashMap<>();
            result.put("targetUsername", targetUsername);
            result.put("isMafia", isMafia);
            result.put("message", resultMessage);
            
            return ResponseEntity.ok(result);
        } catch (ValidationException e) {
            throw e;
        } catch (GameException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
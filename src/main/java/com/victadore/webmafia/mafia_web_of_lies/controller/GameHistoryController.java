package com.victadore.webmafia.mafia_web_of_lies.controller;

import com.victadore.webmafia.mafia_web_of_lies.service.GameHistoryService;
import com.victadore.webmafia.mafia_web_of_lies.service.ValidationService;
import com.victadore.webmafia.mafia_web_of_lies.exception.ValidationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@Controller
@RequestMapping("/history")
public class GameHistoryController {
    
    private final GameHistoryService gameHistoryService;
    private final ValidationService validationService;
    
    public GameHistoryController(GameHistoryService gameHistoryService,
                               ValidationService validationService) {
        this.gameHistoryService = gameHistoryService;
        this.validationService = validationService;
    }
    
    /**
     * Game history browser page
     */
    @GetMapping
    public String gameHistoryBrowser(@RequestParam(defaultValue = "0") @Min(0) int page,
                                   @RequestParam(defaultValue = "20") @Min(5) @Max(50) int size,
                                   Model model) {
        
        int offset = page * size;
        List<Map<String, Object>> games = gameHistoryService.getFinishedGames(size, offset);
        
        model.addAttribute("games", games);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("hasNextPage", games.size() == size);
        
        return "history/browser";
    }
    
    /**
     * Game replay page
     */
    @GetMapping("/game/{gameCode}")
    public String gameReplay(@PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$") String gameCode,
                           Model model) {
        try {
            validationService.validateGameCode(gameCode);
            
            Map<String, Object> replay = gameHistoryService.getGameReplay(gameCode);
            
            // Ensure all required fields are present to prevent template errors
            if (replay == null) {
                replay = new HashMap<>();
            }
            
            // Provide safe defaults for all template variables
            replay.putIfAbsent("gameCode", gameCode);
            replay.putIfAbsent("gameState", "UNKNOWN");
            replay.putIfAbsent("winner", "Unknown");
            replay.putIfAbsent("totalDays", 0);
            replay.putIfAbsent("createdBy", "Unknown");
            replay.putIfAbsent("players", new ArrayList<>());
            replay.putIfAbsent("timeline", new ArrayList<>());
            replay.putIfAbsent("statistics", new HashMap<>());
            
            model.addAttribute("replay", replay);
            model.addAttribute("gameCode", gameCode);
            return "history/replay";
            
        } catch (ValidationException e) {
            model.addAttribute("error", "Invalid game code format: " + e.getMessage());
            model.addAttribute("gameCode", gameCode);
            return "history/game-not-found";
        } catch (Exception e) {
            // Log the actual error for debugging
            System.err.println("Error in gameReplay for code " + gameCode + ": " + e.getMessage());
            e.printStackTrace();
            
            // Provide minimal safe replay data to prevent template errors
            Map<String, Object> safeReplay = new HashMap<>();
            safeReplay.put("gameCode", gameCode);
            safeReplay.put("gameState", "ERROR");
            safeReplay.put("winner", "Unknown");
            safeReplay.put("totalDays", 0);
            safeReplay.put("createdBy", "Unknown");
            safeReplay.put("players", new ArrayList<>());
            safeReplay.put("timeline", new ArrayList<>());
            safeReplay.put("statistics", new HashMap<>());
            
            model.addAttribute("replay", safeReplay);
            model.addAttribute("gameCode", gameCode);
            model.addAttribute("error", "Unable to load complete game data. Showing basic information.");
            
            return "history/replay";
        }
    }
    
    /**
     * Player game history page
     */
    @GetMapping("/player/{username}")
    public String playerGameHistory(@PathVariable @Size(min = 2, max = 20) String username,
                                  @RequestParam(defaultValue = "20") @Min(5) @Max(100) int limit,
                                  Model model) {
        try {
            validationService.validateUsername(username);
            
            List<Map<String, Object>> playerGames = gameHistoryService.getPlayerGameHistory(username, limit);
            
            model.addAttribute("playerGames", playerGames);
            model.addAttribute("username", username);
            model.addAttribute("limit", limit);
            
            return "history/player-history";
        } catch (ValidationException e) {
            // Log the validation error for debugging
            System.err.println("Validation error in playerGameHistory for username " + username + ": " + e.getMessage());
            
            model.addAttribute("playerGames", List.of());
            model.addAttribute("username", username);
            model.addAttribute("limit", limit);
            model.addAttribute("error", "Invalid username format: " + e.getMessage());
            
            return "history/player-history";
        } catch (Exception e) {
            // Log the actual error for debugging
            System.err.println("Error in playerGameHistory for username " + username + ": " + e.getMessage());
            e.printStackTrace();
            
            model.addAttribute("playerGames", List.of());
            model.addAttribute("username", username);
            model.addAttribute("limit", limit);
            model.addAttribute("error", "Unable to load player history");
            
            return "history/player-history";
        }
    }
    
    /**
     * Game search page
     */
    @GetMapping("/search")
    public String gameSearch(@RequestParam(required = false) String createdBy,
                           @RequestParam(required = false) String winner,
                           @RequestParam(required = false) Integer minDays,
                           @RequestParam(required = false) Integer maxDays,
                           @RequestParam(defaultValue = "20") @Min(5) @Max(100) int limit,
                           Model model) {
        
        // Normalize empty strings to null for proper filtering
        createdBy = (createdBy != null && createdBy.trim().isEmpty()) ? null : createdBy;
        winner = (winner != null && winner.trim().isEmpty()) ? null : winner;
        
        List<Map<String, Object>> searchResults = null;
        
        // Only search if at least one parameter is provided
        if (createdBy != null || winner != null || minDays != null || maxDays != null) {
            searchResults = gameHistoryService.searchGames(createdBy, winner, minDays, maxDays, limit);
        }
        
        model.addAttribute("searchResults", searchResults);
        model.addAttribute("createdBy", createdBy);
        model.addAttribute("winner", winner);
        model.addAttribute("minDays", minDays);
        model.addAttribute("maxDays", maxDays);
        model.addAttribute("limit", limit);
        
        return "history/search";
    }
    
    // REST API Endpoints
    
    /**
     * Get finished games list via API
     */
    @GetMapping("/api/games")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getFinishedGames(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        
        int offset = page * size;
        List<Map<String, Object>> games = gameHistoryService.getFinishedGames(size, offset);
        return ResponseEntity.ok(games);
    }
    
    /**
     * Get game replay data via API
     */
    @GetMapping("/api/game/{gameCode}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getGameReplay(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$") String gameCode) {
        validationService.validateGameCode(gameCode);
        
        try {
            Map<String, Object> replay = gameHistoryService.getGameReplay(gameCode);
            return ResponseEntity.ok(replay);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get player game history via API
     */
    @GetMapping("/api/player/{username}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getPlayerGameHistory(
            @PathVariable @Size(min = 2, max = 20) String username,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        validationService.validateUsername(username);
        
        List<Map<String, Object>> playerGames = gameHistoryService.getPlayerGameHistory(username, limit);
        return ResponseEntity.ok(playerGames);
    }
    
    /**
     * Search games via API
     */
    @GetMapping("/api/search")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> searchGames(
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String winner,
            @RequestParam(required = false) Integer minDays,
            @RequestParam(required = false) Integer maxDays,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
        
        // Normalize empty strings to null for proper filtering
        createdBy = (createdBy != null && createdBy.trim().isEmpty()) ? null : createdBy;
        winner = (winner != null && winner.trim().isEmpty()) ? null : winner;
        
        List<Map<String, Object>> results = gameHistoryService.searchGames(createdBy, winner, minDays, maxDays, limit);
        return ResponseEntity.ok(results);
    }
} 
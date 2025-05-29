package com.victadore.webmafia.mafia_web_of_lies.controller;

import com.victadore.webmafia.mafia_web_of_lies.model.ActionHistory;
import com.victadore.webmafia.mafia_web_of_lies.model.Game;
import com.victadore.webmafia.mafia_web_of_lies.repository.GameRepository;
import com.victadore.webmafia.mafia_web_of_lies.service.ActionHistoryService;
import com.victadore.webmafia.mafia_web_of_lies.service.ValidationService;
import com.victadore.webmafia.mafia_web_of_lies.exception.GameException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/history")
public class ActionHistoryController {
    
    private final ActionHistoryService actionHistoryService;
    private final ValidationService validationService;
    private final GameRepository gameRepository;
    
    public ActionHistoryController(ActionHistoryService actionHistoryService, 
                                  ValidationService validationService,
                                  GameRepository gameRepository) {
        this.actionHistoryService = actionHistoryService;
        this.validationService = validationService;
        this.gameRepository = gameRepository;
    }
    
    /**
     * Get complete game history
     */
    @GetMapping("/game/{gameCode}")
    public ResponseEntity<List<Map<String, Object>>> getGameHistory(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode) {
        validationService.validateGameCode(gameCode);
        List<Map<String, Object>> history = actionHistoryService.getFormattedHistory(
            actionHistoryService.getGameHistoryByCode(gameCode).get(0).getGame().getId()
        );
        return ResponseEntity.ok(history);
    }
    
    /**
     * Get full detailed game history (available only for finished games)
     */
    @GetMapping("/game/{gameCode}/full")
    public ResponseEntity<List<Map<String, Object>>> getFullGameHistory(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode) {
        validationService.validateGameCode(gameCode);
        List<ActionHistory> allHistory = actionHistoryService.getGameHistoryByCode(gameCode);
        if (allHistory.isEmpty()) {
            throw new GameException("No history found for this game");
        }
        
        // Check if game is finished
        Long gameId = allHistory.get(0).getGame().getId();
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new GameException("Game not found"));
            
        if (!"FINISHED".equals(game.getGameState())) {
            throw new GameException("Full history is only available for finished games");
        }
        
        List<Map<String, Object>> fullHistory = allHistory.stream()
            .map(actionHistoryService::formatActionForFullDisplay)
            .collect(Collectors.toList());
        return ResponseEntity.ok(fullHistory);
    }
    
    /**
     * Get game history in raw format
     */
    @GetMapping("/game/{gameCode}/raw")
    public ResponseEntity<List<ActionHistory>> getGameHistoryRaw(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode) {
        validationService.validateGameCode(gameCode);
        List<ActionHistory> history = actionHistoryService.getGameHistoryByCode(gameCode);
        return ResponseEntity.ok(history);
    }
    
    /**
     * Get player's action history
     */
    @GetMapping("/game/{gameCode}/player/{username}")
    public ResponseEntity<List<ActionHistory>> getPlayerHistory(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode,
            @PathVariable @Size(min = 2, max = 20, message = "Invalid username length") String username) {
        validationService.validateGameCode(gameCode);
        validationService.validateUsername(username);
        
        List<ActionHistory> allHistory = actionHistoryService.getGameHistoryByCode(gameCode);
        if (allHistory.isEmpty()) {
            throw new GameException("No history found for this game");
        }
        
        Long gameId = allHistory.get(0).getGame().getId();
        List<ActionHistory> playerHistory = actionHistoryService.getPlayerHistory(gameId, username);
        return ResponseEntity.ok(playerHistory);
    }
    
    /**
     * Get actions by type
     */
    @GetMapping("/game/{gameCode}/type/{actionType}")
    public ResponseEntity<List<ActionHistory>> getActionsByType(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode,
            @PathVariable @Pattern(regexp = "^[A-Z_]+$", message = "Invalid action type format") String actionType) {
        validationService.validateGameCode(gameCode);
        
        List<ActionHistory> allHistory = actionHistoryService.getGameHistoryByCode(gameCode);
        if (allHistory.isEmpty()) {
            throw new GameException("No history found for this game");
        }
        
        Long gameId = allHistory.get(0).getGame().getId();
        List<ActionHistory> typeHistory = actionHistoryService.getActionsByType(gameId, actionType);
        return ResponseEntity.ok(typeHistory);
    }
    
    /**
     * Get voting history for a specific day
     */
    @GetMapping("/game/{gameCode}/votes/day/{day}")
    public ResponseEntity<List<ActionHistory>> getVotingHistory(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode,
            @PathVariable Integer day) {
        validationService.validateGameCode(gameCode);
        
        if (day < 1 || day > 100) {
            throw new GameException("Day must be between 1 and 100");
        }
        
        List<ActionHistory> allHistory = actionHistoryService.getGameHistoryByCode(gameCode);
        if (allHistory.isEmpty()) {
            throw new GameException("No history found for this game");
        }
        
        Long gameId = allHistory.get(0).getGame().getId();
        List<ActionHistory> votingHistory = actionHistoryService.getVotingHistory(gameId, day);
        return ResponseEntity.ok(votingHistory);
    }
    
    /**
     * Get night actions for a specific night
     */
    @GetMapping("/game/{gameCode}/night-actions/day/{day}")
    public ResponseEntity<List<ActionHistory>> getNightActions(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode,
            @PathVariable Integer day) {
        validationService.validateGameCode(gameCode);
        
        if (day < 1 || day > 100) {
            throw new GameException("Day must be between 1 and 100");
        }
        
        List<ActionHistory> allHistory = actionHistoryService.getGameHistoryByCode(gameCode);
        if (allHistory.isEmpty()) {
            throw new GameException("No history found for this game");
        }
        
        Long gameId = allHistory.get(0).getGame().getId();
        List<ActionHistory> nightActions = actionHistoryService.getNightActions(gameId, day);
        return ResponseEntity.ok(nightActions);
    }
    
    /**
     * Get elimination history
     */
    @GetMapping("/game/{gameCode}/eliminations")
    public ResponseEntity<List<ActionHistory>> getEliminationHistory(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode) {
        validationService.validateGameCode(gameCode);
        
        List<ActionHistory> allHistory = actionHistoryService.getGameHistoryByCode(gameCode);
        if (allHistory.isEmpty()) {
            throw new GameException("No history found for this game");
        }
        
        Long gameId = allHistory.get(0).getGame().getId();
        List<ActionHistory> eliminations = actionHistoryService.getEliminationHistory(gameId);
        return ResponseEntity.ok(eliminations);
    }
    
    /**
     * Get actions in a specific phase
     */
    @GetMapping("/game/{gameCode}/phase")
    public ResponseEntity<List<ActionHistory>> getActionsInPhase(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode,
            @RequestParam Integer day,
            @RequestParam Integer phase) {
        validationService.validateGameCode(gameCode);
        
        if (day < 1 || day > 100) {
            throw new GameException("Day must be between 1 and 100");
        }
        
        if (phase < 0 || phase > 1) {
            throw new GameException("Phase must be 0 (Day) or 1 (Night)");
        }
        
        List<ActionHistory> allHistory = actionHistoryService.getGameHistoryByCode(gameCode);
        if (allHistory.isEmpty()) {
            throw new GameException("No history found for this game");
        }
        
        Long gameId = allHistory.get(0).getGame().getId();
        List<ActionHistory> phaseActions = actionHistoryService.getActionsInPhase(gameId, day, phase);
        return ResponseEntity.ok(phaseActions);
    }
    
    /**
     * Get game statistics
     */
    @GetMapping("/game/{gameCode}/statistics")
    public ResponseEntity<Map<String, Object>> getGameStatistics(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode) {
        validationService.validateGameCode(gameCode);
        
        List<ActionHistory> allHistory = actionHistoryService.getGameHistoryByCode(gameCode);
        if (allHistory.isEmpty()) {
            throw new GameException("No history found for this game");
        }
        
        Long gameId = allHistory.get(0).getGame().getId();
        Map<String, Object> statistics = actionHistoryService.getGameStatistics(gameId);
        return ResponseEntity.ok(statistics);
    }
    
    /**
     * Get player statistics
     */
    @GetMapping("/game/{gameCode}/player/{username}/statistics")
    public ResponseEntity<Map<String, Object>> getPlayerStatistics(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode,
            @PathVariable @Size(min = 2, max = 20, message = "Invalid username length") String username) {
        validationService.validateGameCode(gameCode);
        validationService.validateUsername(username);
        
        List<ActionHistory> allHistory = actionHistoryService.getGameHistoryByCode(gameCode);
        if (allHistory.isEmpty()) {
            throw new GameException("No history found for this game");
        }
        
        Long gameId = allHistory.get(0).getGame().getId();
        Map<String, Object> playerStats = actionHistoryService.getPlayerStatistics(gameId, username);
        return ResponseEntity.ok(playerStats);
    }
    
    /**
     * Get action summary by day
     */
    @GetMapping("/game/{gameCode}/summary")
    public ResponseEntity<Map<String, Object>> getActionSummary(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode) {
        validationService.validateGameCode(gameCode);
        
        List<ActionHistory> allHistory = actionHistoryService.getGameHistoryByCode(gameCode);
        if (allHistory.isEmpty()) {
            throw new GameException("No history found for this game");
        }
        
        // Group actions by day and phase
        Map<String, Object> summary = new java.util.HashMap<>();
        Map<Integer, Map<String, List<ActionHistory>>> dayPhaseActions = new java.util.HashMap<>();
        
        for (ActionHistory action : allHistory) {
            int day = action.getGameDay();
            String phase = action.getGamePhase() == 0 ? "day" : "night";
            
            dayPhaseActions.computeIfAbsent(day, k -> new java.util.HashMap<>())
                          .computeIfAbsent(phase, k -> new java.util.ArrayList<>())
                          .add(action);
        }
        
        summary.put("actionsByDay", dayPhaseActions);
        summary.put("totalDays", dayPhaseActions.size());
        summary.put("totalActions", allHistory.size());
        
        return ResponseEntity.ok(summary);
    }
    
    /**
     * Export game history (for future CSV/JSON export functionality)
     */
    @GetMapping("/game/{gameCode}/export")
    public ResponseEntity<Map<String, Object>> exportGameHistory(
            @PathVariable @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Invalid game code format") String gameCode,
            @RequestParam(defaultValue = "json") String format) {
        validationService.validateGameCode(gameCode);
        
        List<ActionHistory> history = actionHistoryService.getGameHistoryByCode(gameCode);
        
        Map<String, Object> exportData = new java.util.HashMap<>();
        exportData.put("gameCode", gameCode);
        exportData.put("exportTime", java.time.LocalDateTime.now());
        exportData.put("format", format);
        exportData.put("totalActions", history.size());
        exportData.put("actions", history);
        
        if (!history.isEmpty()) {
            exportData.put("gameId", history.get(0).getGame().getId());
            exportData.put("gameStartTime", history.get(0).getTimestamp());
            exportData.put("gameEndTime", history.get(history.size() - 1).getTimestamp());
        }
        
        return ResponseEntity.ok(exportData);
    }
} 
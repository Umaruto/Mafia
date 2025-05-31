package com.victadore.webmafia.mafia_web_of_lies.controller;

import com.victadore.webmafia.mafia_web_of_lies.model.PlayerStatistics;
import com.victadore.webmafia.mafia_web_of_lies.model.Game;
import com.victadore.webmafia.mafia_web_of_lies.model.GameState;
import com.victadore.webmafia.mafia_web_of_lies.repository.GameRepository;
import com.victadore.webmafia.mafia_web_of_lies.repository.PlayerStatisticsRepository;
import com.victadore.webmafia.mafia_web_of_lies.service.PlayerStatisticsService;
import com.victadore.webmafia.mafia_web_of_lies.service.ValidationService;
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
import java.util.ArrayList;
import java.util.HashMap;

@Controller
@RequestMapping("/statistics")
public class StatisticsController {
    
    private final PlayerStatisticsService playerStatisticsService;
    private final ValidationService validationService;
    private final GameRepository gameRepository;
    private final PlayerStatisticsRepository playerStatisticsRepository;
    
    public StatisticsController(PlayerStatisticsService playerStatisticsService,
                              ValidationService validationService,
                              GameRepository gameRepository,
                              PlayerStatisticsRepository playerStatisticsRepository) {
        this.playerStatisticsService = playerStatisticsService;
        this.validationService = validationService;
        this.gameRepository = gameRepository;
        this.playerStatisticsRepository = playerStatisticsRepository;
    }
    
    /**
     * Statistics dashboard page
     */
    @GetMapping
    public String statisticsDashboard(Model model) {
        try {
            // Get global statistics
            Map<String, Object> globalStats = playerStatisticsService.getGlobalStatistics();
            model.addAttribute("globalStats", globalStats);
            
            // Get top leaderboards (limited for dashboard)
            model.addAttribute("topWinRate", playerStatisticsService.getWinRateLeaderboard(5, 3));
            model.addAttribute("topGamesPlayed", playerStatisticsService.getGamesPlayedLeaderboard(5));
            model.addAttribute("topSurvival", playerStatisticsService.getSurvivalRateLeaderboard(5, 3));
            model.addAttribute("recentlyActive", playerStatisticsService.getRecentlyActivePlayers(7, 10));
            model.addAttribute("achievements", playerStatisticsService.getPlayersWithAchievements(5));
        } catch (Exception e) {
            // If database is empty or statistics don't exist, provide empty data
            Map<String, Object> emptyStats = new HashMap<>();
            emptyStats.put("totalPlayers", 0);
            emptyStats.put("totalGamesPlayed", 0);
            emptyStats.put("averageWinRate", 0.0);
            emptyStats.put("averageSurvivalRate", 0.0);
            
            model.addAttribute("globalStats", emptyStats);
            model.addAttribute("topWinRate", new ArrayList<>());
            model.addAttribute("topGamesPlayed", new ArrayList<>());
            model.addAttribute("topSurvival", new ArrayList<>());
            model.addAttribute("recentlyActive", new ArrayList<>());
            model.addAttribute("achievements", new ArrayList<>());
            model.addAttribute("noDataMessage", "No game statistics available yet. Play some games to see statistics!");
        }
        
        return "statistics/dashboard";
    }
    
    /**
     * Player profile page
     */
    @GetMapping("/player/{username}")
    public String playerProfile(@PathVariable @Size(min = 2, max = 20) String username, Model model) {
        try {
            System.out.println("DEBUG: Attempting to get player profile for username: " + username);
            
            // Validate username
            validationService.validateUsername(username);
            System.out.println("DEBUG: Username validation passed");
            
            // Check if player statistics exist first
            boolean statsExist = playerStatisticsRepository.existsByUsername(username);
            System.out.println("DEBUG: Player statistics exist: " + statsExist);
            
            if (!statsExist) {
                System.out.println("DEBUG: No statistics found for player, redirecting to not found page");
                model.addAttribute("error", "This player hasn't completed any games yet or statistics are still being generated.");
                model.addAttribute("username", username);
                return "statistics/player-not-found";
            }
            
            // Try to get player statistics
            PlayerStatistics stats = playerStatisticsService.getPlayerStatistics(username);
            System.out.println("DEBUG: Player statistics retrieved successfully");
            
            model.addAttribute("playerStats", stats);
            model.addAttribute("username", username);
            return "statistics/player-profile";
            
        } catch (Exception e) {
            System.err.println("DEBUG: Exception in playerProfile for username '" + username + "':");
            System.err.println("DEBUG: Exception type: " + e.getClass().getSimpleName());
            System.err.println("DEBUG: Exception message: " + e.getMessage());
            e.printStackTrace();
            
            model.addAttribute("error", "Player statistics not found. This player hasn't completed any games yet.");
            model.addAttribute("username", username);
            return "statistics/player-not-found";
        }
    }
    
    /**
     * Player search endpoint (for form submission)
     */
    @GetMapping("/player")
    public String playerProfileSearch(@RequestParam String username, Model model) {
        try {
            // Validate username format
            if (username == null || username.trim().isEmpty()) {
                model.addAttribute("error", "Please enter a username to search for.");
                model.addAttribute("username", "");
                return "statistics/player-not-found";
            }
            
            username = username.trim();
            
            // Basic validation
            if (username.length() < 2 || username.length() > 20) {
                model.addAttribute("error", "Username must be between 2 and 20 characters long.");
                model.addAttribute("username", username);
                return "statistics/player-not-found";
            }
            
            return playerProfile(username, model);
            
        } catch (Exception e) {
            // Log the error for debugging
            System.err.println("Error in playerProfileSearch for username '" + username + "': " + e.getMessage());
            e.printStackTrace();
            
            model.addAttribute("error", "Unable to search for player statistics. Please try again later.");
            model.addAttribute("username", username != null ? username : "");
            return "statistics/player-not-found";
        }
    }
    
    /**
     * Leaderboards page
     */
    @GetMapping("/leaderboards")
    public String leaderboards(@RequestParam(defaultValue = "winrate") String type,
                             @RequestParam(defaultValue = "10") int limit,
                             @RequestParam(defaultValue = "3") int minGames,
                             Model model) {
        
        model.addAttribute("currentType", type);
        model.addAttribute("limit", limit);
        model.addAttribute("minGames", minGames);
        
        try {
            List<PlayerStatistics> leaderboard;
            String title;
            
            switch (type.toLowerCase()) {
                case "winrate":
                    leaderboard = playerStatisticsService.getWinRateLeaderboard(limit, minGames);
                    title = "Win Rate Leaderboard";
                    break;
                case "games":
                    leaderboard = playerStatisticsService.getGamesPlayedLeaderboard(limit);
                    title = "Most Games Played";
                    break;
                case "survival":
                    leaderboard = playerStatisticsService.getSurvivalRateLeaderboard(limit, minGames);
                    title = "Survival Rate Leaderboard";
                    break;
                case "achievements":
                    leaderboard = playerStatisticsService.getPlayersWithAchievements(limit);
                    title = "Achievement Leaders";
                    break;
                default:
                    leaderboard = playerStatisticsService.getWinRateLeaderboard(limit, minGames);
                    title = "Win Rate Leaderboard";
            }
            
            model.addAttribute("leaderboard", leaderboard);
            model.addAttribute("title", title);
            
            if (leaderboard.isEmpty()) {
                model.addAttribute("noDataMessage", "No statistics available yet. Complete some games to appear on the leaderboards!");
            }
        } catch (Exception e) {
            model.addAttribute("leaderboard", new ArrayList<>());
            model.addAttribute("title", "Leaderboard");
            model.addAttribute("noDataMessage", "Statistics system is not ready yet. Play some games to generate data!");
        }
        
        return "statistics/leaderboards";
    }
    
    /**
     * Role-specific leaderboards page
     */
    @GetMapping("/leaderboards/roles")
    public String roleLeaderboards(@RequestParam(defaultValue = "10") @Min(5) @Max(50) int limit,
                                 @RequestParam(defaultValue = "3") @Min(1) @Max(10) int minGames,
                                 Model model) {
        
        Map<String, List<PlayerStatistics>> roleLeaderboards = 
            playerStatisticsService.getRoleSpecificLeaderboards(limit, minGames);
        
        model.addAttribute("roleLeaderboards", roleLeaderboards);
        model.addAttribute("limit", limit);
        model.addAttribute("minGames", minGames);
        
        return "statistics/role-leaderboards";
    }
    
    // REST API Endpoints
    
    /**
     * Get player statistics via API
     */
    @GetMapping("/api/player/{username}")
    @ResponseBody
    public ResponseEntity<PlayerStatistics> getPlayerStatistics(
            @PathVariable @Size(min = 2, max = 20) String username) {
        validationService.validateUsername(username);
        
        try {
            PlayerStatistics stats = playerStatisticsService.getPlayerStatistics(username);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get global statistics via API
     */
    @GetMapping("/api/global")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getGlobalStatistics() {
        Map<String, Object> stats = playerStatisticsService.getGlobalStatistics();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get win rate leaderboard via API
     */
    @GetMapping("/api/leaderboard/winrate")
    @ResponseBody
    public ResponseEntity<List<PlayerStatistics>> getWinRateLeaderboard(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "3") @Min(1) @Max(50) int minGames) {
        
        List<PlayerStatistics> leaderboard = playerStatisticsService.getWinRateLeaderboard(limit, minGames);
        return ResponseEntity.ok(leaderboard);
    }
    
    /**
     * Get games played leaderboard via API
     */
    @GetMapping("/api/leaderboard/games")
    @ResponseBody
    public ResponseEntity<List<PlayerStatistics>> getGamesPlayedLeaderboard(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        
        List<PlayerStatistics> leaderboard = playerStatisticsService.getGamesPlayedLeaderboard(limit);
        return ResponseEntity.ok(leaderboard);
    }
    
    /**
     * Get survival rate leaderboard via API
     */
    @GetMapping("/api/leaderboard/survival")
    @ResponseBody
    public ResponseEntity<List<PlayerStatistics>> getSurvivalRateLeaderboard(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "3") @Min(1) @Max(50) int minGames) {
        
        List<PlayerStatistics> leaderboard = playerStatisticsService.getSurvivalRateLeaderboard(limit, minGames);
        return ResponseEntity.ok(leaderboard);
    }
    
    /**
     * Get role-specific leaderboards via API
     */
    @GetMapping("/api/leaderboard/roles")
    @ResponseBody
    public ResponseEntity<Map<String, List<PlayerStatistics>>> getRoleLeaderboards(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "3") @Min(1) @Max(20) int minGames) {
        
        Map<String, List<PlayerStatistics>> leaderboards = 
            playerStatisticsService.getRoleSpecificLeaderboards(limit, minGames);
        return ResponseEntity.ok(leaderboards);
    }
    
    /**
     * Search players via API
     */
    @GetMapping("/api/search")
    @ResponseBody
    public ResponseEntity<List<PlayerStatistics>> searchPlayers(
            @RequestParam @Size(min = 1, max = 20) String query,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        
        List<PlayerStatistics> results = playerStatisticsService.searchPlayers(query, limit);
        return ResponseEntity.ok(results);
    }
    
    /**
     * Get recently active players via API
     */
    @GetMapping("/api/recent")
    @ResponseBody
    public ResponseEntity<List<PlayerStatistics>> getRecentlyActivePlayers(
            @RequestParam(defaultValue = "7") @Min(1) @Max(365) int days,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        
        List<PlayerStatistics> players = playerStatisticsService.getRecentlyActivePlayers(days, limit);
        return ResponseEntity.ok(players);
    }
    
    /**
     * Get players with achievements via API
     */
    @GetMapping("/api/achievements")
    @ResponseBody
    public ResponseEntity<List<PlayerStatistics>> getPlayersWithAchievements(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        
        List<PlayerStatistics> players = playerStatisticsService.getPlayersWithAchievements(limit);
        return ResponseEntity.ok(players);
    }
    
    /**
     * Test endpoint to manually update statistics for a specific game
     * This is for debugging purposes
     */
    @PostMapping("/api/test/update-game-stats/{gameCode}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testUpdateGameStatistics(
            @PathVariable @Size(min = 6, max = 6) String gameCode) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            playerStatisticsService.updatePlayerStatisticsAfterGame(gameCode);
            result.put("success", true);
            result.put("message", "Statistics updated successfully for game " + gameCode);
            result.put("gameCode", gameCode);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("gameCode", gameCode);
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Test endpoint to update statistics for all finished games
     * This is for debugging purposes
     */
    @PostMapping("/api/test/update-all-stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testUpdateAllStatistics() {
        
        Map<String, Object> result = new HashMap<>();
        List<String> successfulUpdates = new ArrayList<>();
        List<String> failedUpdates = new ArrayList<>();
        
        try {
            // Get all finished games from the game repository
            List<Game> finishedGames = gameRepository.findByGameState(GameState.FINISHED);
            
            for (Game game : finishedGames) {
                try {
                    playerStatisticsService.updatePlayerStatisticsAfterGame(game.getGameCode());
                    successfulUpdates.add(game.getGameCode());
                } catch (Exception e) {
                    failedUpdates.add(game.getGameCode() + ": " + e.getMessage());
                }
            }
            
            result.put("success", true);
            result.put("totalGames", finishedGames.size());
            result.put("successfulUpdates", successfulUpdates);
            result.put("failedUpdates", failedUpdates);
            result.put("message", "Processed " + finishedGames.size() + " finished games");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Debug endpoint to test global statistics query
     */
    @GetMapping("/api/debug/global")
    @ResponseBody
    public ResponseEntity<Object> debugGlobalStatistics() {
        try {
            Object[] rawStats = playerStatisticsRepository.getGlobalStatistics();
            Map<String, Object> debug = new HashMap<>();
            debug.put("rawResult", rawStats);
            debug.put("length", rawStats != null ? rawStats.length : 0);
            if (rawStats != null) {
                for (int i = 0; i < rawStats.length; i++) {
                    debug.put("index" + i, rawStats[i]);
                }
            }
            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("type", e.getClass().getSimpleName());
            return ResponseEntity.ok(error);
        }
    }
} 
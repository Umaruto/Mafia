package com.victadore.webmafia.mafia_web_of_lies.service;

import com.victadore.webmafia.mafia_web_of_lies.model.*;
import com.victadore.webmafia.mafia_web_of_lies.repository.*;
import com.victadore.webmafia.mafia_web_of_lies.exception.GameException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GameHistoryService {
    
    private final GameRepository gameRepository;
    private final ActionHistoryRepository actionHistoryRepository;
    private final PlayerRepository playerRepository;
    
    public GameHistoryService(GameRepository gameRepository,
                            ActionHistoryRepository actionHistoryRepository,
                            PlayerRepository playerRepository) {
        this.gameRepository = gameRepository;
        this.actionHistoryRepository = actionHistoryRepository;
        this.playerRepository = playerRepository;
    }
    
    /**
     * Get complete game replay data
     */
    public Map<String, Object> getGameReplay(String gameCode) {
        Game game = gameRepository.findByGameCode(gameCode);
        if (game == null) {
            throw new GameException("Game not found");
        }
        
        Map<String, Object> replay = new HashMap<>();
        
        try {
            // Basic game information
            replay.put("gameCode", game.getGameCode());
            replay.put("gameState", game.getGameState());
            replay.put("winner", game.getWinner());
            replay.put("totalDays", game.getCurrentDay());
            replay.put("createdBy", game.getCreatedBy());
            
            // Player information with final roles (only for finished games)
            List<Map<String, Object>> players = getPlayersWithRoles(game);
            replay.put("players", players);
            
            // Simple timeline of events
            List<Map<String, Object>> timeline = getSimpleGameTimeline(game.getId());
            replay.put("timeline", timeline);
            
            // Basic game statistics
            Map<String, Object> gameStats = getBasicGameStatistics(game.getId());
            replay.put("statistics", gameStats);
            
        } catch (Exception e) {
            // Log the error and provide minimal data
            System.err.println("Error building game replay: " + e.getMessage());
            e.printStackTrace();
            
            // Provide minimal safe data
            replay.put("gameCode", game.getGameCode());
            replay.put("gameState", game.getGameState());
            replay.put("winner", game.getWinner());
            replay.put("totalDays", game.getCurrentDay());
            replay.put("createdBy", game.getCreatedBy());
            replay.put("players", new ArrayList<>());
            replay.put("timeline", new ArrayList<>());
            replay.put("statistics", new HashMap<>());
        }
        
        return replay;
    }
    
    /**
     * Get simple game timeline without complex processing
     */
    private List<Map<String, Object>> getSimpleGameTimeline(Long gameId) {
        try {
            List<ActionHistory> actions = actionHistoryRepository.findByGameIdOrderByTimestamp(gameId);
            
            return actions.stream()
                .limit(50) // Limit to prevent memory issues
                .map(action -> {
                    Map<String, Object> timelineEvent = new HashMap<>();
                    timelineEvent.put("gameDay", action.getGameDay());
                    timelineEvent.put("gamePhase", action.getGamePhase());
                    timelineEvent.put("description", action.getActionDetails() != null ? 
                        action.getActionDetails() : action.getActionType());
                    return timelineEvent;
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error getting timeline: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Get basic game statistics without complex processing
     */
    private Map<String, Object> getBasicGameStatistics(Long gameId) {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            List<ActionHistory> allActions = actionHistoryRepository.findByGameIdOrderByTimestamp(gameId);
            
            if (!allActions.isEmpty()) {
                LocalDateTime gameStart = allActions.get(0).getTimestamp();
                LocalDateTime gameEnd = allActions.get(allActions.size() - 1).getTimestamp();
                
                stats.put("gameStart", gameStart);
                stats.put("gameEnd", gameEnd);
                
                long minutes = java.time.Duration.between(gameStart, gameEnd).toMinutes();
                stats.put("duration", minutes + " minutes");
            }
            
            return stats;
        } catch (Exception e) {
            System.err.println("Error getting statistics: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * Get players with their roles (only for finished games)
     */
    private List<Map<String, Object>> getPlayersWithRoles(Game game) {
        List<Map<String, Object>> players = new ArrayList<>();
        
        for (Player player : game.getPlayers()) {
            Map<String, Object> playerInfo = new HashMap<>();
            playerInfo.put("username", player.getUsername());
            playerInfo.put("isAlive", player.isAlive());
            
            // Only reveal roles for finished games
            if (GameState.FINISHED.equals(game.getGameState())) {
                playerInfo.put("role", player.getRole().name());
                playerInfo.put("roleDescription", getRoleDescription(player.getRole()));
            }
            
            players.add(playerInfo);
        }
        
        return players;
    }
    
    /**
     * Get complete game timeline
     */
    private List<Map<String, Object>> getGameTimeline(Long gameId) {
        List<ActionHistory> actions = actionHistoryRepository.findByGameIdOrderByTimestamp(gameId);
        
        return actions.stream()
            .map(this::formatActionForTimeline)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    /**
     * Format action for timeline display
     */
    private Map<String, Object> formatActionForTimeline(ActionHistory action) {
        Map<String, Object> timelineEvent = new HashMap<>();
        
        timelineEvent.put("id", action.getId());
        timelineEvent.put("timestamp", action.getTimestamp());
        timelineEvent.put("day", action.getGameDay());
        timelineEvent.put("phase", action.getGamePhase() == 0 ? "DAY" : "NIGHT");
        timelineEvent.put("actionType", action.getActionType());
        timelineEvent.put("actor", action.getActorUsername());
        timelineEvent.put("target", action.getTargetUsername());
        timelineEvent.put("successful", action.getSuccessful());
        
        // Format description based on action type
        String description = formatActionDescription(action);
        timelineEvent.put("description", description);
        
        // Add icon and color for UI
        timelineEvent.put("icon", getActionIcon(action.getActionType()));
        timelineEvent.put("color", getActionColor(action.getActionType()));
        
        return timelineEvent;
    }
    
    /**
     * Format action description for display
     */
    private String formatActionDescription(ActionHistory action) {
        switch (action.getActionType()) {
            case "GAME_START":
                return "Game started with " + action.getActionDetails();
            case "GAME_END":
                return "Game ended - " + action.getResult();
            case "PHASE_TRANSITION":
                return action.getActionDetails();
            case "VOTE":
                return String.format("%s voted for %s", action.getActorUsername(), action.getTargetUsername());
            case "SKIP_VOTE":
                return String.format("%s skipped voting", action.getActorUsername());
            case "ELIMINATE":
                return String.format("%s was eliminated with %d votes", 
                    action.getTargetUsername(), action.getVoteCount() != null ? action.getVoteCount() : 0);
            case "KILL":
                return String.format("%s attempted to eliminate %s", action.getActorUsername(), action.getTargetUsername());
            case "SAVE":
                return String.format("%s attempted to save %s", action.getActorUsername(), action.getTargetUsername());
            case "INVESTIGATE":
                return String.format("%s investigated %s", action.getActorUsername(), action.getTargetUsername());
            case "MAFIA_TARGET_CHOSEN":
                return "Mafia team reached consensus on elimination target";
            case "MAFIA_TIE":
                return "Mafia votes resulted in a tie - no elimination";
            default:
                return action.getActionDetails() != null ? action.getActionDetails() : "Unknown action";
        }
    }
    
    /**
     * Get day-by-day breakdown
     */
    private Map<Integer, Map<String, Object>> getDayByDayBreakdown(Long gameId, int totalDays) {
        Map<Integer, Map<String, Object>> breakdown = new HashMap<>();
        
        for (int day = 1; day <= totalDays; day++) {
            Map<String, Object> dayData = new HashMap<>();
            
            // Day phase actions
            List<ActionHistory> dayActions = actionHistoryRepository
                .findByGameIdAndGameDayAndGamePhaseOrderByTimestamp(gameId, day, 0);
            dayData.put("dayActions", formatActionsForBreakdown(dayActions));
            
            // Night phase actions
            List<ActionHistory> nightActions = actionHistoryRepository
                .findByGameIdAndGameDayAndGamePhaseOrderByTimestamp(gameId, day, 1);
            dayData.put("nightActions", formatActionsForBreakdown(nightActions));
            
            // Voting summary for this day
            Map<String, Object> votingSummary = getVotingSummary(gameId, day);
            dayData.put("votingSummary", votingSummary);
            
            // Night actions summary
            Map<String, Object> nightSummary = getNightActionsSummary(gameId, day);
            dayData.put("nightSummary", nightSummary);
            
            breakdown.put(day, dayData);
        }
        
        return breakdown;
    }
    
    /**
     * Format actions for day breakdown
     */
    private List<Map<String, Object>> formatActionsForBreakdown(List<ActionHistory> actions) {
        return actions.stream()
            .map(action -> {
                Map<String, Object> actionData = new HashMap<>();
                actionData.put("timestamp", action.getTimestamp());
                actionData.put("type", action.getActionType());
                actionData.put("actor", action.getActorUsername());
                actionData.put("target", action.getTargetUsername());
                actionData.put("description", formatActionDescription(action));
                actionData.put("successful", action.getSuccessful());
                return actionData;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Get voting summary for a specific day
     */
    private Map<String, Object> getVotingSummary(Long gameId, int day) {
        List<ActionHistory> votes = actionHistoryRepository.findVoteActionsInDay(gameId, day);
        
        Map<String, Object> summary = new HashMap<>();
        Map<String, Integer> voteCount = new HashMap<>();
        List<String> skipVoters = new ArrayList<>();
        
        for (ActionHistory vote : votes) {
            if ("SKIP_VOTE".equals(vote.getActionType())) {
                skipVoters.add(vote.getActorUsername());
            } else if ("VOTE".equals(vote.getActionType()) && vote.getTargetUsername() != null) {
                voteCount.put(vote.getTargetUsername(), 
                    voteCount.getOrDefault(vote.getTargetUsername(), 0) + 1);
            }
        }
        
        summary.put("voteCount", voteCount);
        summary.put("skipVoters", skipVoters);
        summary.put("totalVotes", votes.size());
        
        // Find who was eliminated
        List<ActionHistory> eliminations = actionHistoryRepository
            .findByGameIdAndGameDayAndGamePhaseOrderByTimestamp(gameId, day, 0)
            .stream()
            .filter(action -> "ELIMINATE".equals(action.getActionType()))
            .collect(Collectors.toList());
        
        if (!eliminations.isEmpty()) {
            ActionHistory elimination = eliminations.get(0);
            summary.put("eliminated", elimination.getTargetUsername());
            summary.put("eliminationVotes", elimination.getVoteCount());
        }
        
        return summary;
    }
    
    /**
     * Get night actions summary for a specific day
     */
    private Map<String, Object> getNightActionsSummary(Long gameId, int day) {
        List<ActionHistory> nightActions = actionHistoryRepository.findNightActionsInNight(gameId, day);
        
        Map<String, Object> summary = new HashMap<>();
        Map<String, List<String>> actionsByType = new HashMap<>();
        
        for (ActionHistory action : nightActions) {
            String actionType = action.getActionType();
            actionsByType.computeIfAbsent(actionType, k -> new ArrayList<>())
                .add(String.format("%s -> %s", action.getActorUsername(), action.getTargetUsername()));
        }
        
        summary.put("actionsByType", actionsByType);
        summary.put("totalActions", nightActions.size());
        
        // Check for deaths during night
        List<ActionHistory> deaths = actionHistoryRepository
            .findByGameIdAndGameDayAndGamePhaseOrderByTimestamp(gameId, day + 1, 0) // Deaths are revealed next day
            .stream()
            .filter(action -> action.getActionDetails() != null && 
                    action.getActionDetails().contains("died last night"))
            .collect(Collectors.toList());
        
        if (!deaths.isEmpty()) {
            summary.put("deaths", deaths.stream()
                .map(ActionHistory::getTargetUsername)
                .collect(Collectors.toList()));
        }
        
        return summary;
    }
    
    /**
     * Get game statistics
     */
    private Map<String, Object> getGameStatistics(Long gameId) {
        Map<String, Object> stats = new HashMap<>();
        
        List<ActionHistory> allActions = actionHistoryRepository.findByGameIdOrderByTimestamp(gameId);
        
        if (!allActions.isEmpty()) {
            LocalDateTime gameStart = allActions.get(0).getTimestamp();
            LocalDateTime gameEnd = allActions.get(allActions.size() - 1).getTimestamp();
            
            stats.put("gameStart", gameStart);
            stats.put("gameEnd", gameEnd);
            stats.put("duration", java.time.Duration.between(gameStart, gameEnd).toMinutes() + " minutes");
        }
        
        // Action type counts
        Map<String, Long> actionCounts = allActions.stream()
            .collect(Collectors.groupingBy(ActionHistory::getActionType, Collectors.counting()));
        stats.put("actionCounts", actionCounts);
        
        // Player participation
        Map<String, Long> playerActionCounts = allActions.stream()
            .filter(action -> !"SYSTEM".equals(action.getActorUsername()))
            .collect(Collectors.groupingBy(ActionHistory::getActorUsername, Collectors.counting()));
        stats.put("playerParticipation", playerActionCounts);
        
        return stats;
    }
    
    /**
     * Get list of finished games for history browser
     */
    public List<Map<String, Object>> getFinishedGames(int limit, int offset) {
        // This would need a custom query in GameRepository
        // For now, we'll get all games and filter
        List<Game> allGames = gameRepository.findAll();
        
        return allGames.stream()
            .filter(game -> GameState.FINISHED.equals(game.getGameState()))
            .sorted((g1, g2) -> g2.getId().compareTo(g1.getId())) // Most recent first
            .skip(offset)
            .limit(limit)
            .map(this::formatGameSummary)
            .collect(Collectors.toList());
    }
    
    /**
     * Format game summary for list display
     */
    private Map<String, Object> formatGameSummary(Game game) {
        Map<String, Object> summary = new HashMap<>();
        
        summary.put("gameCode", game.getGameCode());
        summary.put("winner", game.getWinner());
        summary.put("totalDays", game.getCurrentDay());
        summary.put("playerCount", game.getPlayers().size());
        summary.put("createdBy", game.getCreatedBy());
        
        // Get game duration from action history
        List<ActionHistory> actions = actionHistoryRepository.findByGameIdOrderByTimestamp(game.getId());
        if (!actions.isEmpty()) {
            LocalDateTime start = actions.get(0).getTimestamp();
            LocalDateTime end = actions.get(actions.size() - 1).getTimestamp();
            summary.put("startTime", start);
            summary.put("endTime", end);
            summary.put("duration", java.time.Duration.between(start, end).toMinutes() + " minutes");
        }
        
        return summary;
    }
    
    /**
     * Search games by criteria
     */
    public List<Map<String, Object>> searchGames(String createdBy, String winner, Integer minDays, Integer maxDays, int limit) {
        List<Game> allGames = gameRepository.findAll();
        
        return allGames.stream()
            .filter(game -> GameState.FINISHED.equals(game.getGameState()))
            .filter(game -> {
                // Filter by creator - handle null, empty, and case-insensitive matching
                if (createdBy == null || createdBy.trim().isEmpty()) {
                    return true; // No filter applied
                }
                String gameCreator = game.getCreatedBy();
                return gameCreator != null && 
                       gameCreator.toLowerCase().contains(createdBy.trim().toLowerCase());
            })
            .filter(game -> {
                // Filter by winner - handle null, empty, and case-insensitive matching
                if (winner == null || winner.trim().isEmpty()) {
                    return true; // No filter applied
                }
                String gameWinner = game.getWinner();
                return gameWinner != null && gameWinner.equalsIgnoreCase(winner.trim());
            })
            .filter(game -> minDays == null || game.getCurrentDay() >= minDays)
            .filter(game -> maxDays == null || game.getCurrentDay() <= maxDays)
            .sorted((g1, g2) -> g2.getId().compareTo(g1.getId()))
            .limit(limit)
            .map(this::formatGameSummary)
            .collect(Collectors.toList());
    }
    
    /**
     * Get player's game history
     */
    public List<Map<String, Object>> getPlayerGameHistory(String username, int limit) {
        List<Game> allGames = gameRepository.findAll();
        
        return allGames.stream()
            .filter(game -> GameState.FINISHED.equals(game.getGameState()))
            .filter(game -> game.getPlayers().stream()
                .anyMatch(player -> player.getUsername().equals(username)))
            .sorted((g1, g2) -> g2.getId().compareTo(g1.getId()))
            .limit(limit)
            .map(game -> {
                Map<String, Object> summary = formatGameSummary(game);
                
                // Add player-specific information
                Player player = game.getPlayers().stream()
                    .filter(p -> p.getUsername().equals(username))
                    .findFirst()
                    .orElse(null);
                
                if (player != null) {
                    summary.put("playerRole", player.getRole().name());
                    summary.put("playerSurvived", player.isAlive());
                    
                    // Determine if player won
                    boolean won = false;
                    if ("MAFIA".equals(game.getWinner()) && player.getRole() == Role.MAFIA) {
                        won = true;
                    } else if ("CITIZENS".equals(game.getWinner()) && player.getRole() != Role.MAFIA) {
                        won = true;
                    }
                    summary.put("playerWon", won);
                }
                
                return summary;
            })
            .collect(Collectors.toList());
    }
    
    // Helper methods
    
    private String getRoleDescription(Role role) {
        switch (role) {
            case MAFIA:
                return "Eliminate town members during night phases";
            case DETECTIVE:
                return "Investigate players to find Mafia members";
            case DOCTOR:
                return "Save players from elimination during night phases";
            case CITIZEN:
                return "Vote to eliminate suspected Mafia members";
            default:
                return "Unknown role";
        }
    }
    
    private String getActionIcon(String actionType) {
        switch (actionType) {
            case "GAME_START": return "🎮";
            case "GAME_END": return "🏁";
            case "PHASE_TRANSITION": return "🔄";
            case "VOTE": return "🗳️";
            case "SKIP_VOTE": return "⏭️";
            case "ELIMINATE": return "💀";
            case "KILL": return "🗡️";
            case "SAVE": return "🛡️";
            case "INVESTIGATE": return "🔍";
            case "MAFIA_TARGET_CHOSEN": return "🎯";
            case "MAFIA_TIE": return "⚖️";
            default: return "📋";
        }
    }
    
    private String getActionColor(String actionType) {
        switch (actionType) {
            case "GAME_START": return "success";
            case "GAME_END": return "warning";
            case "PHASE_TRANSITION": return "info";
            case "VOTE": return "primary";
            case "SKIP_VOTE": return "secondary";
            case "ELIMINATE": return "danger";
            case "KILL": return "danger";
            case "SAVE": return "success";
            case "INVESTIGATE": return "info";
            case "MAFIA_TARGET_CHOSEN": return "warning";
            case "MAFIA_TIE": return "secondary";
            default: return "light";
        }
    }
} 
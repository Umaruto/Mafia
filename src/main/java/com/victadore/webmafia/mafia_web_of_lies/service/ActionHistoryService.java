package com.victadore.webmafia.mafia_web_of_lies.service;

import com.victadore.webmafia.mafia_web_of_lies.model.ActionHistory;
import com.victadore.webmafia.mafia_web_of_lies.model.Game;
import com.victadore.webmafia.mafia_web_of_lies.model.GameState;
import com.victadore.webmafia.mafia_web_of_lies.repository.ActionHistoryRepository;
import com.victadore.webmafia.mafia_web_of_lies.repository.GameRepository;
import com.victadore.webmafia.mafia_web_of_lies.exception.GameException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ActionHistoryService {
    
    private final ActionHistoryRepository actionHistoryRepository;
    private final GameRepository gameRepository;
    
    public ActionHistoryService(ActionHistoryRepository actionHistoryRepository, 
                               GameRepository gameRepository) {
        this.actionHistoryRepository = actionHistoryRepository;
        this.gameRepository = gameRepository;
    }
    
    /**
     * Records a new action in the game history
     */
    public ActionHistory recordAction(Long gameId, String actionType, String actorUsername, 
                                    String targetUsername, String actionDetails, String result, 
                                    boolean successful) {
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new GameException("Game not found"));
        
        ActionHistory action = new ActionHistory();
        action.setGame(game);
        action.setActionType(actionType);
        action.setActorUsername(actorUsername);
        action.setTargetUsername(targetUsername);
        action.setGameDay(game.getCurrentDay());
        action.setGamePhase(game.getCurrentPhase());
        action.setTimestamp(LocalDateTime.now());
        action.setActionDetails(actionDetails);
        action.setResult(result);
        action.setSuccessful(successful);
        
        return actionHistoryRepository.save(action);
    }
    
    /**
     * Records a vote action with specific vote details
     */
    public ActionHistory recordVoteAction(Long gameId, String actorUsername, String targetUsername, 
                                        boolean isSkip, boolean successful) {
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new GameException("Game not found"));
        
        ActionHistory action = new ActionHistory();
        action.setGame(game);
        action.setActionType(isSkip ? "SKIP_VOTE" : "VOTE");
        action.setActorUsername(actorUsername);
        action.setTargetUsername(isSkip ? null : targetUsername);
        action.setGameDay(game.getCurrentDay());
        action.setGamePhase(game.getCurrentPhase());
        action.setTimestamp(LocalDateTime.now());
        action.setSuccessful(successful);
        action.setIsSkipVote(isSkip);
        
        String details = isSkip ? 
            String.format("Player %s skipped voting", actorUsername) :
            String.format("Player %s voted for %s", actorUsername, targetUsername);
        action.setActionDetails(details);
        
        String result = successful ? "Vote recorded successfully" : "Vote failed";
        action.setResult(result);
        
        return actionHistoryRepository.save(action);
    }
    
    /**
     * Records a night action (KILL, SAVE, INVESTIGATE)
     */
    public ActionHistory recordNightAction(Long gameId, String actionType, String actorUsername, 
                                         String targetUsername, boolean successful, String additionalData) {
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new GameException("Game not found"));
        
        ActionHistory action = new ActionHistory();
        action.setGame(game);
        action.setActionType(actionType);
        action.setActorUsername(actorUsername);
        action.setTargetUsername(targetUsername);
        action.setGameDay(game.getCurrentDay());
        action.setGamePhase(game.getCurrentPhase());
        action.setTimestamp(LocalDateTime.now());
        action.setSuccessful(successful);
        action.setAdditionalData(additionalData);
        
        String details = String.format("Player %s performed %s on %s", 
                                      actorUsername, actionType.toLowerCase(), targetUsername);
        action.setActionDetails(details);
        
        String result = successful ? 
            String.format("%s action completed successfully", actionType) :
            String.format("%s action failed", actionType);
        action.setResult(result);
        
        return actionHistoryRepository.save(action);
    }
    
    /**
     * Records a player elimination
     */
    public ActionHistory recordElimination(Long gameId, String eliminatedUsername, String eliminationType, 
                                         int voteCount, String additionalDetails) {
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new GameException("Game not found"));
        
        ActionHistory action = new ActionHistory();
        action.setGame(game);
        action.setActionType("ELIMINATE");
        action.setActorUsername("SYSTEM"); // System action
        action.setTargetUsername(eliminatedUsername);
        action.setGameDay(game.getCurrentDay());
        action.setGamePhase(game.getCurrentPhase());
        action.setTimestamp(LocalDateTime.now());
        action.setSuccessful(true);
        action.setVoteCount(voteCount);
        
        String details = String.format("Player %s was eliminated by %s. %s", 
                                      eliminatedUsername, eliminationType, additionalDetails);
        action.setActionDetails(details);
        action.setResult("Player eliminated");
        
        return actionHistoryRepository.save(action);
    }
    
    /**
     * Records game phase transitions
     */
    public ActionHistory recordPhaseTransition(Long gameId, String fromPhase, String toPhase) {
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new GameException("Game not found"));
        
        ActionHistory action = new ActionHistory();
        action.setGame(game);
        action.setActionType("PHASE_TRANSITION");
        action.setActorUsername("SYSTEM");
        action.setGameDay(game.getCurrentDay());
        action.setGamePhase(game.getCurrentPhase());
        action.setTimestamp(LocalDateTime.now());
        action.setSuccessful(true);
        
        String details = String.format("Game transitioned from %s to %s (Day %d)", 
                                      fromPhase, toPhase, game.getCurrentDay());
        action.setActionDetails(details);
        action.setResult("Phase transition completed");
        
        return actionHistoryRepository.save(action);
    }
    
    /**
     * Records game start/end events
     */
    public ActionHistory recordGameEvent(Long gameId, String eventType, String details, String result) {
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new GameException("Game not found"));
        
        ActionHistory action = new ActionHistory();
        action.setGame(game);
        action.setActionType(eventType);
        action.setActorUsername("SYSTEM");
        action.setGameDay(game.getCurrentDay());
        action.setGamePhase(game.getCurrentPhase());
        action.setTimestamp(LocalDateTime.now());
        action.setSuccessful(true);
        action.setActionDetails(details);
        action.setResult(result);
        
        return actionHistoryRepository.save(action);
    }
    
    /**
     * Get complete game history
     */
    public List<ActionHistory> getGameHistory(Long gameId) {
        return actionHistoryRepository.findByGameIdOrderByTimestamp(gameId);
    }
    
    /**
     * Get game history by game code
     */
    public List<ActionHistory> getGameHistoryByCode(String gameCode) {
        Game game = gameRepository.findByGameCode(gameCode);
        if (game == null) {
            throw new GameException("Game not found");
        }
        return getGameHistory(game.getId());
    }
    
    /**
     * Get player's action history
     */
    public List<ActionHistory> getPlayerHistory(Long gameId, String username) {
        return actionHistoryRepository.findByGameIdAndActorUsernameOrderByTimestamp(gameId, username);
    }
    
    /**
     * Get actions by type
     */
    public List<ActionHistory> getActionsByType(Long gameId, String actionType) {
        return actionHistoryRepository.findByGameIdAndActionTypeOrderByTimestamp(gameId, actionType);
    }
    
    /**
     * Get actions in a specific phase
     */
    public List<ActionHistory> getActionsInPhase(Long gameId, int gameDay, int gamePhase) {
        return actionHistoryRepository.findByGameIdAndGameDayAndGamePhaseOrderByTimestamp(gameId, gameDay, gamePhase);
    }
    
    /**
     * Get voting history for a specific day
     */
    public List<ActionHistory> getVotingHistory(Long gameId, int gameDay) {
        return actionHistoryRepository.findVoteActionsInDay(gameId, gameDay);
    }
    
    /**
     * Get night actions for a specific night
     */
    public List<ActionHistory> getNightActions(Long gameId, int gameDay) {
        return actionHistoryRepository.findNightActionsInNight(gameId, gameDay);
    }
    
    /**
     * Get elimination history
     */
    public List<ActionHistory> getEliminationHistory(Long gameId) {
        return actionHistoryRepository.findEliminationActions(gameId);
    }
    
    /**
     * Get action statistics for the game
     */
    public Map<String, Object> getGameStatistics(Long gameId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Total action count
        stats.put("totalActions", actionHistoryRepository.countByGameId(gameId));
        
        // Success rate
        long successfulActions = actionHistoryRepository.countByGameIdAndSuccessful(gameId, true);
        long totalActions = actionHistoryRepository.countByGameId(gameId);
        double successRate = totalActions > 0 ? (double) successfulActions / totalActions * 100 : 0;
        stats.put("successRate", Math.round(successRate * 100.0) / 100.0);
        
        // Action type breakdown
        List<Object[]> actionStats = actionHistoryRepository.getActionStatistics(gameId);
        Map<String, Long> actionCounts = new HashMap<>();
        for (Object[] stat : actionStats) {
            actionCounts.put((String) stat[0], (Long) stat[1]);
        }
        stats.put("actionBreakdown", actionCounts);
        
        // Game duration (first to last action)
        List<ActionHistory> allActions = actionHistoryRepository.findByGameIdOrderByTimestamp(gameId);
        if (!allActions.isEmpty()) {
            LocalDateTime firstAction = allActions.get(0).getTimestamp();
            LocalDateTime lastAction = allActions.get(allActions.size() - 1).getTimestamp();
            stats.put("firstActionTime", firstAction);
            stats.put("lastActionTime", lastAction);
            stats.put("gameDuration", java.time.Duration.between(firstAction, lastAction).toMinutes() + " minutes");
        }
        
        return stats;
    }
    
    /**
     * Get player statistics
     */
    public Map<String, Object> getPlayerStatistics(Long gameId, String username) {
        Map<String, Object> stats = new HashMap<>();
        
        List<ActionHistory> playerActions = getPlayerHistory(gameId, username);
        stats.put("totalActions", playerActions.size());
        
        long successfulActions = playerActions.stream()
            .mapToLong(action -> action.getSuccessful() ? 1 : 0)
            .sum();
        
        double successRate = playerActions.size() > 0 ? 
            (double) successfulActions / playerActions.size() * 100 : 0;
        stats.put("successRate", Math.round(successRate * 100.0) / 100.0);
        
        // Action type breakdown for player
        Map<String, Long> actionCounts = playerActions.stream()
            .collect(Collectors.groupingBy(ActionHistory::getActionType, Collectors.counting()));
        stats.put("actionBreakdown", actionCounts);
        
        // Times targeted by others
        List<ActionHistory> targetedActions = actionHistoryRepository
            .findByGameIdAndTargetUsernameOrderByTimestamp(gameId, username);
        stats.put("timesTargeted", targetedActions.size());
        
        return stats;
    }
    
    /**
     * Get formatted action history for display - filters sensitive information during active games
     */
    public List<Map<String, Object>> getFormattedHistory(Long gameId) {
        List<ActionHistory> actions = getGameHistory(gameId);
        
        // Check if game is finished to determine what information to show
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new GameException("Game not found"));
        
        boolean gameFinished = GameState.FINISHED.equals(game.getGameState());
        
        return actions.stream()
            .map(action -> gameFinished ? 
                formatActionForDisplay(action) : 
                formatActionForPublicDisplay(action))
            .filter(Objects::nonNull) // Remove hidden actions
            .collect(Collectors.toList());
    }
    
    /**
     * Format a single action for display (full details - for finished games)
     */
    private Map<String, Object> formatActionForDisplay(ActionHistory action) {
        Map<String, Object> formatted = new HashMap<>();
        formatted.put("id", action.getId());
        formatted.put("timestamp", action.getTimestamp());
        formatted.put("day", action.getGameDay());
        formatted.put("phase", action.getGamePhase() == 0 ? "Day" : "Night");
        formatted.put("actionType", action.getActionType());
        formatted.put("actor", action.getActorUsername());
        formatted.put("target", action.getTargetUsername());
        formatted.put("details", action.getActionDetails());
        formatted.put("result", action.getResult());
        formatted.put("successful", action.getSuccessful());
        
        if (action.getIsSkipVote() != null) {
            formatted.put("isSkipVote", action.getIsSkipVote());
        }
        if (action.getVoteCount() != null) {
            formatted.put("voteCount", action.getVoteCount());
        }
        if (action.getAdditionalData() != null) {
            formatted.put("additionalData", action.getAdditionalData());
        }
        
        return formatted;
    }
    
    /**
     * Format a single action for public display (filtered for active games)
     */
    private Map<String, Object> formatActionForPublicDisplay(ActionHistory action) {
        Map<String, Object> formatted = new HashMap<>();
        String actionType = action.getActionType();
        
        // Filter out sensitive actions that shouldn't be shown during active games
        if (shouldHideAction(actionType)) {
            return null; // Will be filtered out
        }
        
        // Get public-safe action type - if null, hide the action
        String publicActionType = getPublicActionType(actionType);
        if (publicActionType == null) {
            return null; // Will be filtered out
        }
        
        // Get public-safe details - if null, hide the action  
        String publicDetails = getPublicActionDetails(action);
        if (publicDetails == null) {
            return null; // Will be filtered out
        }
        
        formatted.put("id", action.getId());
        formatted.put("timestamp", action.getTimestamp());
        formatted.put("day", action.getGameDay());
        formatted.put("phase", action.getGamePhase() == 0 ? "Day" : "Night");
        formatted.put("actionType", publicActionType);
        formatted.put("details", publicDetails);
        formatted.put("successful", true); // Always show as successful to avoid revealing failures
        
        return formatted;
    }
    
    /**
     * Determine if an action should be hidden from public view during active games
     */
    private boolean shouldHideAction(String actionType) {
        return Arrays.asList(
            "ROLE_ASSIGNMENT", // Never show role assignments
            "KILL", // Don't reveal who performed kills
            "SAVE", // Don't reveal who performed saves  
            "INVESTIGATE", // Don't reveal who performed investigations
            "MAFIA_CONSENSUS", // Don't reveal internal mafia coordination
            "NIGHT_ACTION", // Generic night actions should be hidden
            "MAFIA_KILL", // Don't show mafia kill details during game
            "DOCTOR_SAVE", // Don't show doctor save details during game
            "DETECTIVE_INVESTIGATE", // Don't show detective investigation details during game
            "SETUP", // Hide setup phase actions
            "ROLE_REVEAL", // Hide role reveals during game
            "TEAM_ASSIGNMENT" // Hide team assignments
        ).contains(actionType);
    }
    
    /**
     * Get public-safe action type
     */
    private String getPublicActionType(String actionType) {
        switch (actionType) {
            case "VOTE":
                return "VOTE";
            case "SKIP_VOTE":
                return "SKIP_VOTE";
            case "ELIMINATE":
            case "MAFIA_KILL":
                return "ELIMINATION";
            case "PHASE_TRANSITION":
                return "PHASE_CHANGE";
            case "GAME_START":
                return "GAME_START";
            case "GAME_END":
                return "GAME_END";
            // Hide these completely by returning null - they'll be filtered out
            case "ROLE_ASSIGNMENT":
            case "KILL":
            case "SAVE":
            case "INVESTIGATE":
            case "MAFIA_CONSENSUS":
            case "NIGHT_ACTION":
            case "SETUP":
            case "ROLE_REVEAL":
            case "TEAM_ASSIGNMENT":
                return null; // Will cause the action to be filtered out
            default:
                // For unknown action types, hide them completely during active games
                return null;
        }
    }
    
    /**
     * Get public-safe action details
     */
    private String getPublicActionDetails(ActionHistory action) {
        String actionType = action.getActionType();
        
        switch (actionType) {
            case "VOTE":
                return String.format("Player %s voted for %s", 
                    action.getActorUsername(), action.getTargetUsername());
            case "SKIP_VOTE":
                return String.format("Player %s skipped voting", action.getActorUsername());
            case "ELIMINATE":
            case "MAFIA_KILL":
                return String.format("Player %s was eliminated", action.getTargetUsername());
            case "PHASE_TRANSITION":
                return String.format("Game phase changed to %s", 
                    action.getGamePhase() == 0 ? "Day" : "Night");
            case "GAME_START":
                return "Game started";
            case "GAME_END":
                return action.getActionDetails(); // Safe to show game end details
            default:
                return null; // Hidden actions
        }
    }
    
    /**
     * Format action for full display (public method for finished games)
     */
    public Map<String, Object> formatActionForFullDisplay(ActionHistory action) {
        return formatActionForDisplay(action);
    }
    
    /**
     * Delete all action history for a game (when game is deleted)
     */
    public void deleteGameHistory(Long gameId) {
        List<ActionHistory> actions = actionHistoryRepository.findByGameIdOrderByTimestamp(gameId);
        actionHistoryRepository.deleteAll(actions);
    }
} 
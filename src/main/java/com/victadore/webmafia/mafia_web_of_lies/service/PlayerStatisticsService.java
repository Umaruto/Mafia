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
@Transactional
public class PlayerStatisticsService {
    
    private final PlayerStatisticsRepository playerStatisticsRepository;
    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final ActionHistoryRepository actionHistoryRepository;
    
    public PlayerStatisticsService(PlayerStatisticsRepository playerStatisticsRepository,
                                 GameRepository gameRepository,
                                 PlayerRepository playerRepository,
                                 ActionHistoryRepository actionHistoryRepository) {
        this.playerStatisticsRepository = playerStatisticsRepository;
        this.gameRepository = gameRepository;
        this.playerRepository = playerRepository;
        this.actionHistoryRepository = actionHistoryRepository;
    }
    
    /**
     * Get or create player statistics
     */
    public PlayerStatistics getOrCreatePlayerStatistics(String username) {
        return playerStatisticsRepository.findByUsername(username)
            .orElseGet(() -> createNewPlayerStatistics(username));
    }
    
    /**
     * Create new player statistics record
     */
    private PlayerStatistics createNewPlayerStatistics(String username) {
        PlayerStatistics stats = new PlayerStatistics();
        stats.setUsername(username);
        LocalDateTime now = LocalDateTime.now();
        stats.setFirstGameDate(now);
        stats.setLastGameDate(now);
        stats.setLastUpdated(now);
        return playerStatisticsRepository.save(stats);
    }
    
    /**
     * Update player statistics after a game ends
     */
    public void updatePlayerStatisticsAfterGame(String gameCode) {
        Game game = gameRepository.findByGameCode(gameCode);
        if (game == null) {
            throw new GameException("Game not found");
        }
        
        // Check if game is finished or has a winner (more flexible than checking database state)
        if (game.getWinner() == null) {
            throw new GameException("Game has no winner - cannot update statistics");
        }
        
        List<Player> players = game.getPlayers();
        if (players == null || players.isEmpty()) {
            throw new GameException("Game has no players - cannot update statistics");
        }
        
        String winner = game.getWinner(); // "MAFIA" or "CITIZENS"
        
        for (Player player : players) {
            try {
                updateIndividualPlayerStats(player, winner, game);
            } catch (Exception e) {
                System.err.println("Failed to update statistics for player " + player.getUsername() + ": " + e.getMessage());
                // Continue with other players even if one fails
            }
        }
    }
    
    /**
     * Update individual player statistics
     */
    private void updateIndividualPlayerStats(Player player, String winner, Game game) {
        PlayerStatistics stats = getOrCreatePlayerStatistics(player.getUsername());
        
        // Basic game statistics
        stats.setTotalGames(stats.getTotalGames() + 1);
        stats.setLastGameDate(LocalDateTime.now());
        
        // Determine if player won
        boolean playerWon = isPlayerWinner(player, winner);
        if (playerWon) {
            stats.setGamesWon(stats.getGamesWon() + 1);
        } else {
            stats.setGamesLost(stats.getGamesLost() + 1);
        }
        
        // Role-specific statistics
        updateRoleSpecificStats(stats, player.getRole(), playerWon);
        
        // Survival statistics
        if (player.isAlive()) {
            stats.setTimesSurvived(stats.getTimesSurvived() + 1);
        } else {
            stats.setTimesEliminated(stats.getTimesEliminated() + 1);
            updateEliminationStats(stats, player, game);
        }
        
        // Action-based statistics
        updateActionBasedStats(stats, player, game);
        
        // Achievement calculations
        updateAchievements(stats, player, game, playerWon);
        
        playerStatisticsRepository.save(stats);
    }
    
    /**
     * Determine if player won based on their role and game outcome
     */
    private boolean isPlayerWinner(Player player, String winner) {
        if ("MAFIA".equals(winner)) {
            return player.getRole() == Role.MAFIA;
        } else if ("CITIZENS".equals(winner)) {
            return player.getRole() != Role.MAFIA;
        }
        return false;
    }
    
    /**
     * Update role-specific statistics
     */
    private void updateRoleSpecificStats(PlayerStatistics stats, Role role, boolean won) {
        switch (role) {
            case MAFIA:
                stats.setMafiaGames(stats.getMafiaGames() + 1);
                if (won) stats.setMafiaWins(stats.getMafiaWins() + 1);
                break;
            case DETECTIVE:
                stats.setDetectiveGames(stats.getDetectiveGames() + 1);
                if (won) stats.setDetectiveWins(stats.getDetectiveWins() + 1);
                break;
            case DOCTOR:
                stats.setDoctorGames(stats.getDoctorGames() + 1);
                if (won) stats.setDoctorWins(stats.getDoctorWins() + 1);
                break;
            case CITIZEN:
                stats.setCitizenGames(stats.getCitizenGames() + 1);
                if (won) stats.setCitizenWins(stats.getCitizenWins() + 1);
                break;
        }
    }
    
    /**
     * Update elimination statistics
     */
    private void updateEliminationStats(PlayerStatistics stats, Player player, Game game) {
        // Find how the player was eliminated
        List<ActionHistory> eliminations = actionHistoryRepository
            .findByGameIdAndTargetUsernameOrderByTimestamp(game.getId(), player.getUsername());
        
        for (ActionHistory action : eliminations) {
            if ("ELIMINATE".equals(action.getActionType()) || "MAFIA_KILL".equals(action.getActionType())) {
                if (action.getActionDetails() != null && action.getActionDetails().contains("voting")) {
                    stats.setTimesEliminatedByVoting(stats.getTimesEliminatedByVoting() + 1);
                } else if (action.getActionDetails() != null && action.getActionDetails().contains("Mafia")) {
                    stats.setTimesEliminatedByMafia(stats.getTimesEliminatedByMafia() + 1);
                }
                break; // Only count the first elimination
            }
        }
    }
    
    /**
     * Update action-based statistics
     */
    private void updateActionBasedStats(PlayerStatistics stats, Player player, Game game) {
        List<ActionHistory> playerActions = actionHistoryRepository
            .findByGameIdAndActorUsernameOrderByTimestamp(game.getId(), player.getUsername());
        
        for (ActionHistory action : playerActions) {
            switch (action.getActionType()) {
                case "VOTE":
                    stats.setTotalVotesCast(stats.getTotalVotesCast() + 1);
                    // Check if vote was for a Mafia member
                    if (isVoteForMafia(action, game)) {
                        stats.setCorrectMafiaVotes(stats.getCorrectMafiaVotes() + 1);
                    }
                    break;
                case "SKIP_VOTE":
                    stats.setSkipVotes(stats.getSkipVotes() + 1);
                    break;
                case "INVESTIGATE":
                    stats.setInvestigationsPerformed(stats.getInvestigationsPerformed() + 1);
                    stats.setNightActionsPerformed(stats.getNightActionsPerformed() + 1);
                    if (action.getSuccessful()) {
                        stats.setSuccessfulNightActions(stats.getSuccessfulNightActions() + 1);
                        // Check if investigation found Mafia
                        if (isInvestigationSuccessful(action, game)) {
                            stats.setMafiaFound(stats.getMafiaFound() + 1);
                        }
                    }
                    break;
                case "SAVE":
                    stats.setSavesAttempted(stats.getSavesAttempted() + 1);
                    stats.setNightActionsPerformed(stats.getNightActionsPerformed() + 1);
                    if (action.getSuccessful()) {
                        stats.setSuccessfulNightActions(stats.getSuccessfulNightActions() + 1);
                        stats.setSuccessfulSaves(stats.getSuccessfulSaves() + 1);
                    }
                    break;
                case "KILL":
                    stats.setMafiaKillsAttempted(stats.getMafiaKillsAttempted() + 1);
                    stats.setNightActionsPerformed(stats.getNightActionsPerformed() + 1);
                    if (action.getSuccessful()) {
                        stats.setSuccessfulNightActions(stats.getSuccessfulNightActions() + 1);
                        stats.setSuccessfulMafiaKills(stats.getSuccessfulMafiaKills() + 1);
                    }
                    break;
            }
        }
    }
    
    /**
     * Check if a vote was for a Mafia member
     */
    private boolean isVoteForMafia(ActionHistory voteAction, Game game) {
        if (voteAction.getTargetUsername() == null) return false;
        
        Player targetPlayer = game.getPlayers().stream()
            .filter(p -> p.getUsername().equals(voteAction.getTargetUsername()))
            .findFirst()
            .orElse(null);
        
        return targetPlayer != null && targetPlayer.getRole() == Role.MAFIA;
    }
    
    /**
     * Check if an investigation was successful (found Mafia)
     */
    private boolean isInvestigationSuccessful(ActionHistory investigateAction, Game game) {
        if (investigateAction.getTargetUsername() == null) return false;
        
        Player targetPlayer = game.getPlayers().stream()
            .filter(p -> p.getUsername().equals(investigateAction.getTargetUsername()))
            .findFirst()
            .orElse(null);
        
        return targetPlayer != null && targetPlayer.getRole() == Role.MAFIA;
    }
    
    /**
     * Update achievements and special recognitions
     */
    private void updateAchievements(PlayerStatistics stats, Player player, Game game, boolean won) {
        // Perfect game: Won without being eliminated and performed optimally
        if (won && player.isAlive()) {
            boolean perfectGame = false;
            
            switch (player.getRole()) {
                case DETECTIVE:
                    // Perfect detective: Found at least one Mafia
                    perfectGame = hasFoundMafiaInGame(player, game);
                    break;
                case DOCTOR:
                    // Perfect doctor: Made at least one successful save
                    perfectGame = hasMadeSuccessfulSaveInGame(player, game);
                    break;
                case MAFIA:
                    // Perfect Mafia: Won without being suspected (no votes against)
                    perfectGame = !wasVotedAgainst(player, game);
                    break;
                case CITIZEN:
                    // Perfect citizen: Voted correctly for Mafia at least once
                    perfectGame = hasVotedForMafiaInGame(player, game);
                    break;
            }
            
            if (perfectGame) {
                stats.setPerfectGames(stats.getPerfectGames() + 1);
            }
        }
        
        // MVP calculation (simplified - could be more complex)
        if (won && isPlayerMVP(player, game)) {
            stats.setMvpAwards(stats.getMvpAwards() + 1);
        }
    }
    
    /**
     * Check if detective found Mafia in this game
     */
    private boolean hasFoundMafiaInGame(Player player, Game game) {
        List<ActionHistory> investigations = actionHistoryRepository
            .findByGameIdAndActorUsernameOrderByTimestamp(game.getId(), player.getUsername());
        
        return investigations.stream()
            .filter(action -> "INVESTIGATE".equals(action.getActionType()))
            .anyMatch(action -> isInvestigationSuccessful(action, game));
    }
    
    /**
     * Check if doctor made successful save in this game
     */
    private boolean hasMadeSuccessfulSaveInGame(Player player, Game game) {
        List<ActionHistory> saves = actionHistoryRepository
            .findByGameIdAndActorUsernameOrderByTimestamp(game.getId(), player.getUsername());
        
        return saves.stream()
            .filter(action -> "SAVE".equals(action.getActionType()))
            .anyMatch(ActionHistory::getSuccessful);
    }
    
    /**
     * Check if player was voted against in this game
     */
    private boolean wasVotedAgainst(Player player, Game game) {
        List<ActionHistory> votesAgainst = actionHistoryRepository
            .findByGameIdAndTargetUsernameOrderByTimestamp(game.getId(), player.getUsername());
        
        return votesAgainst.stream()
            .anyMatch(action -> "VOTE".equals(action.getActionType()));
    }
    
    /**
     * Check if citizen voted for Mafia in this game
     */
    private boolean hasVotedForMafiaInGame(Player player, Game game) {
        List<ActionHistory> votes = actionHistoryRepository
            .findByGameIdAndActorUsernameOrderByTimestamp(game.getId(), player.getUsername());
        
        return votes.stream()
            .filter(action -> "VOTE".equals(action.getActionType()))
            .anyMatch(action -> isVoteForMafia(action, game));
    }
    
    /**
     * Simplified MVP calculation
     */
    private boolean isPlayerMVP(Player player, Game game) {
        // This is a simplified version - could be much more sophisticated
        switch (player.getRole()) {
            case DETECTIVE:
                return hasFoundMafiaInGame(player, game);
            case DOCTOR:
                return hasMadeSuccessfulSaveInGame(player, game);
            case MAFIA:
                return !wasVotedAgainst(player, game);
            case CITIZEN:
                return hasVotedForMafiaInGame(player, game);
            default:
                return false;
        }
    }
    
    /**
     * Get player statistics by username
     */
    public PlayerStatistics getPlayerStatistics(String username) {
        return playerStatisticsRepository.findByUsername(username)
            .orElseThrow(() -> new GameException("Player statistics not found"));
    }
    
    /**
     * Get leaderboard by win rate
     */
    public List<PlayerStatistics> getWinRateLeaderboard(int limit, int minGames) {
        try {
            return playerStatisticsRepository.findTopPlayersByWinRate(minGames)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * Get leaderboard by games played
     */
    public List<PlayerStatistics> getGamesPlayedLeaderboard(int limit) {
        try {
            return playerStatisticsRepository.findTopPlayersByGamesPlayed()
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * Get leaderboard by survival rate
     */
    public List<PlayerStatistics> getSurvivalRateLeaderboard(int limit, int minGames) {
        try {
            return playerStatisticsRepository.findTopPlayersBySurvivalRate(minGames)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * Get role-specific leaderboards
     */
    public Map<String, List<PlayerStatistics>> getRoleSpecificLeaderboards(int limit, int minGames) {
        Map<String, List<PlayerStatistics>> leaderboards = new HashMap<>();
        
        try {
            leaderboards.put("mafia", playerStatisticsRepository.findTopMafiaPlayers(minGames)
                .stream().limit(limit).collect(Collectors.toList()));
            
            leaderboards.put("detective", playerStatisticsRepository.findTopDetectivePlayers(3)
                .stream().limit(limit).collect(Collectors.toList()));
            
            leaderboards.put("doctor", playerStatisticsRepository.findTopDoctorPlayers(3)
                .stream().limit(limit).collect(Collectors.toList()));
        } catch (Exception e) {
            // Return empty lists if there's an error
            leaderboards.put("mafia", new ArrayList<>());
            leaderboards.put("detective", new ArrayList<>());
            leaderboards.put("doctor", new ArrayList<>());
        }
        
        return leaderboards;
    }
    
    /**
     * Get global statistics summary
     */
    public Map<String, Object> getGlobalStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            Object[] globalStats = playerStatisticsRepository.getGlobalStatistics();
            
            // Handle global statistics with defaults for empty database
            if (globalStats != null && globalStats.length > 0 && globalStats[0] != null) {
                // The query returns an array containing another array, so we need to extract the inner array
                Object[] actualStats = (Object[]) globalStats[0];
                
                if (actualStats.length >= 6) {
                    stats.put("totalPlayers", actualStats[0]);
                    stats.put("totalGamesPlayed", actualStats[1] != null ? actualStats[1] : 0);
                    // Convert averages from decimal (0.0-1.0) to percentage (0.0-100.0)
                    Double avgWinRate = actualStats[2] != null ? ((Number) actualStats[2]).doubleValue() * 100 : 0.0;
                    Double avgSurvivalRate = actualStats[3] != null ? ((Number) actualStats[3]).doubleValue() * 100 : 0.0;
                    stats.put("averageWinRate", avgWinRate);
                    stats.put("averageSurvivalRate", avgSurvivalRate);
                    stats.put("mostGamesPlayed", actualStats[4] != null ? actualStats[4] : 0);
                    stats.put("mostWins", actualStats[5] != null ? actualStats[5] : 0);
                } else {
                    // Default values for incomplete data
                    stats.put("totalPlayers", 0);
                    stats.put("totalGamesPlayed", 0);
                    stats.put("averageWinRate", 0.0);
                    stats.put("averageSurvivalRate", 0.0);
                    stats.put("mostGamesPlayed", 0);
                    stats.put("mostWins", 0);
                }
            } else {
                // Default values for empty database
                stats.put("totalPlayers", 0);
                stats.put("totalGamesPlayed", 0);
                stats.put("averageWinRate", 0.0);
                stats.put("averageSurvivalRate", 0.0);
                stats.put("mostGamesPlayed", 0);
                stats.put("mostWins", 0);
            }
            
            // Handle experience distribution with defaults
            try {
                Object[] experienceDistribution = playerStatisticsRepository.getExperienceLevelDistribution();
                if (experienceDistribution != null && experienceDistribution.length >= 6) {
                    Map<String, Object> experience = new HashMap<>();
                    experience.put("newcomers", experienceDistribution[0] != null ? experienceDistribution[0] : 0);
                    experience.put("beginners", experienceDistribution[1] != null ? experienceDistribution[1] : 0);
                    experience.put("intermediate", experienceDistribution[2] != null ? experienceDistribution[2] : 0);
                    experience.put("advanced", experienceDistribution[3] != null ? experienceDistribution[3] : 0);
                    experience.put("expert", experienceDistribution[4] != null ? experienceDistribution[4] : 0);
                    experience.put("master", experienceDistribution[5] != null ? experienceDistribution[5] : 0);
                    stats.put("experienceDistribution", experience);
                } else {
                    // Default empty experience distribution
                    Map<String, Object> experience = new HashMap<>();
                    experience.put("newcomers", 0);
                    experience.put("beginners", 0);
                    experience.put("intermediate", 0);
                    experience.put("advanced", 0);
                    experience.put("expert", 0);
                    experience.put("master", 0);
                    stats.put("experienceDistribution", experience);
                }
            } catch (Exception e) {
                // Default empty experience distribution
                Map<String, Object> experience = new HashMap<>();
                experience.put("newcomers", 0);
                experience.put("beginners", 0);
                experience.put("intermediate", 0);
                experience.put("advanced", 0);
                experience.put("expert", 0);
                experience.put("master", 0);
                stats.put("experienceDistribution", experience);
            }
            
            // Handle role distribution with defaults
            try {
                Object[] roleDistribution = playerStatisticsRepository.getRoleDistributionStatistics();
                if (roleDistribution != null && roleDistribution.length >= 4) {
                    Map<String, Object> roles = new HashMap<>();
                    roles.put("mafiaGames", roleDistribution[0] != null ? roleDistribution[0] : 0);
                    roles.put("detectiveGames", roleDistribution[1] != null ? roleDistribution[1] : 0);
                    roles.put("doctorGames", roleDistribution[2] != null ? roleDistribution[2] : 0);
                    roles.put("citizenGames", roleDistribution[3] != null ? roleDistribution[3] : 0);
                    stats.put("roleDistribution", roles);
                } else {
                    // Default values for empty database
                    Map<String, Object> roles = new HashMap<>();
                    roles.put("mafiaGames", 0);
                    roles.put("detectiveGames", 0);
                    roles.put("doctorGames", 0);
                    roles.put("citizenGames", 0);
                    stats.put("roleDistribution", roles);
                }
            } catch (Exception e) {
                // Default values for empty database
                Map<String, Object> roles = new HashMap<>();
                roles.put("mafiaGames", 0);
                roles.put("detectiveGames", 0);
                roles.put("doctorGames", 0);
                roles.put("citizenGames", 0);
                stats.put("roleDistribution", roles);
            }
            
        } catch (Exception e) {
            // If any error occurs, return default empty statistics
            stats.put("totalPlayers", 0);
            stats.put("totalGamesPlayed", 0);
            stats.put("averageWinRate", 0.0);
            stats.put("averageSurvivalRate", 0.0);
            stats.put("mostGamesPlayed", 0);
            stats.put("mostWins", 0);
            
            Map<String, Object> experience = new HashMap<>();
            experience.put("newcomers", 0);
            experience.put("beginners", 0);
            experience.put("intermediate", 0);
            experience.put("advanced", 0);
            experience.put("expert", 0);
            experience.put("master", 0);
            stats.put("experienceDistribution", experience);
            
            Map<String, Object> roles = new HashMap<>();
            roles.put("mafiaGames", 0);
            roles.put("detectiveGames", 0);
            roles.put("doctorGames", 0);
            roles.put("citizenGames", 0);
            stats.put("roleDistribution", roles);
        }
        
        return stats;
    }
    
    /**
     * Search players by username
     */
    public List<PlayerStatistics> searchPlayers(String searchTerm, int limit) {
        try {
            return playerStatisticsRepository.searchPlayersByUsername(searchTerm)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * Get recently active players
     */
    public List<PlayerStatistics> getRecentlyActivePlayers(int days, int limit) {
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(days);
            return playerStatisticsRepository.findRecentlyActivePlayers(since)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * Get players with achievements
     */
    public List<PlayerStatistics> getPlayersWithAchievements(int limit) {
        try {
            return playerStatisticsRepository.findPlayersWithAchievements()
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
} 
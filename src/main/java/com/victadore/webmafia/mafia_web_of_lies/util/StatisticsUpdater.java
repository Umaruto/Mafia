package com.victadore.webmafia.mafia_web_of_lies.util;

import com.victadore.webmafia.mafia_web_of_lies.model.Game;
import com.victadore.webmafia.mafia_web_of_lies.model.GameState;
import com.victadore.webmafia.mafia_web_of_lies.repository.GameRepository;
import com.victadore.webmafia.mafia_web_of_lies.service.PlayerStatisticsService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Utility class to manually update statistics for existing games
 * This can be run as a command line runner to fix statistics for games that were completed
 * before the statistics system was properly working
 */
@Component
public class StatisticsUpdater implements CommandLineRunner {
    
    private final GameRepository gameRepository;
    private final PlayerStatisticsService playerStatisticsService;
    
    public StatisticsUpdater(GameRepository gameRepository, PlayerStatisticsService playerStatisticsService) {
        this.gameRepository = gameRepository;
        this.playerStatisticsService = playerStatisticsService;
    }
    
    @Override
    public void run(String... args) throws Exception {
        // Only run if specifically requested via command line argument
        if (args.length > 0 && "update-stats".equals(args[0])) {
            updateStatisticsForExistingGames();
        }
    }
    
    public void updateStatisticsForExistingGames() {
        System.out.println("Starting statistics update for existing games...");
        
        List<Game> finishedGames = gameRepository.findByGameState(GameState.FINISHED);
        System.out.println("Found " + finishedGames.size() + " finished games");
        
        int successCount = 0;
        int failCount = 0;
        
        for (Game game : finishedGames) {
            try {
                System.out.println("Updating statistics for game: " + game.getGameCode());
                playerStatisticsService.updatePlayerStatisticsAfterGame(game.getGameCode());
                successCount++;
                System.out.println("✓ Successfully updated statistics for game: " + game.getGameCode());
            } catch (Exception e) {
                failCount++;
                System.err.println("✗ Failed to update statistics for game " + game.getGameCode() + ": " + e.getMessage());
            }
        }
        
        System.out.println("\nStatistics update completed:");
        System.out.println("- Successful updates: " + successCount);
        System.out.println("- Failed updates: " + failCount);
        System.out.println("- Total games processed: " + finishedGames.size());
    }
} 
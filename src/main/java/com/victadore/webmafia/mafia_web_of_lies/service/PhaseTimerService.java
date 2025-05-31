package com.victadore.webmafia.mafia_web_of_lies.service;

import com.victadore.webmafia.mafia_web_of_lies.model.Game;
import com.victadore.webmafia.mafia_web_of_lies.repository.GameRepository;
import com.victadore.webmafia.mafia_web_of_lies.exception.GameException;
import com.victadore.webmafia.mafia_web_of_lies.dto.GameEvent;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

@Service
@EnableScheduling
public class PhaseTimerService {
    private static final Logger logger = Logger.getLogger(PhaseTimerService.class.getName());
    
    // Phase durations in seconds
    public static final int DAY_PHASE_DURATION = 90;   // 1.5 minutes for day votes
    public static final int NIGHT_PHASE_DURATION = 60; // 1 minute for night actions
    
    private final GameRepository gameRepository;
    private final WebSocketService webSocketService;
    private final ApplicationEventPublisher eventPublisher;
    
    public PhaseTimerService(GameRepository gameRepository, 
                           WebSocketService webSocketService,
                           ApplicationEventPublisher eventPublisher) {
        this.gameRepository = gameRepository;
        this.webSocketService = webSocketService;
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * Start timer for a specific phase
     */
    public void startPhaseTimer(String gameCode, int phase) {
        Game game = gameRepository.findByGameCode(gameCode);
        if (game == null) {
            throw new GameException("Game not found");
        }
        
        int duration = (phase == 0) ? DAY_PHASE_DURATION : NIGHT_PHASE_DURATION;
        
        game.setPhaseStartTime(LocalDateTime.now());
        game.setPhaseDurationSeconds(duration);
        game.setTimerEnabled(true);
        
        gameRepository.save(game);
        
        logger.info("Started timer for game " + gameCode + ", phase " + 
                   (phase == 0 ? "DAY" : "NIGHT") + ", duration: " + duration + " seconds");
    }
    
    /**
     * Stop timer for a game
     */
    public void stopTimer(String gameCode) {
        Game game = gameRepository.findByGameCode(gameCode);
        if (game == null) {
            return;
        }
        
        game.setTimerEnabled(false);
        game.setPhaseStartTime(null);
        game.setPhaseDurationSeconds(null);
        
        gameRepository.save(game);
        
        logger.info("Stopped timer for game " + gameCode);
    }
    
    /**
     * Get timer status for a game
     */
    public TimerStatus getTimerStatus(String gameCode) {
        Game game = gameRepository.findByGameCode(gameCode);
        if (game == null) {
            throw new GameException("Game not found");
        }
        
        if (!Boolean.TRUE.equals(game.getTimerEnabled()) || game.getPhaseStartTime() == null || game.getPhaseDurationSeconds() == null) {
            return new TimerStatus(false, 0, 0);
        }
        
        long remainingSeconds = game.getRemainingTimeSeconds();
        return new TimerStatus(true, remainingSeconds, game.getPhaseDurationSeconds());
    }
    
    /**
     * Check for expired timers every 5 seconds and publish timer expiration events
     */
    @Scheduled(fixedRate = 5000) // Check every 5 seconds
    public void checkExpiredTimers() {
        try {
            List<Game> activeGames = gameRepository.findByIsActiveTrue();
            
            for (Game game : activeGames) {
                if (game.isTimerExpired() && game.getGameState().toString().equals("IN_PROGRESS")) {
                    logger.info("Timer expired for game " + game.getGameCode() + 
                               ", publishing timer expiration event for phase " + 
                               (game.getCurrentPhase() == 0 ? "DAY" : "NIGHT"));
                    
                    try {
                        // Publish an event instead of directly calling GameLogicService
                        eventPublisher.publishEvent(new TimerExpiredEvent(game.getGameCode()));
                        
                        // Broadcast timer expiration event to clients
                        webSocketService.broadcastGameUpdate(game.getGameCode(), 
                            new GameEvent("TIMER_EXPIRED", game.getGameCode(), 
                                java.util.Map.of("phase", game.getCurrentPhase() == 0 ? "DAY" : "NIGHT")));
                                
                    } catch (Exception e) {
                        logger.severe("Failed to handle timer expiration for game " + game.getGameCode() + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.severe("Error in timer check: " + e.getMessage());
        }
    }
    
    /**
     * Timer status data class
     */
    public static class TimerStatus {
        private final boolean active;
        private final long remainingSeconds;
        private final int totalSeconds;
        
        public TimerStatus(boolean active, long remainingSeconds, int totalSeconds) {
            this.active = active;
            this.remainingSeconds = remainingSeconds;
            this.totalSeconds = totalSeconds;
        }
        
        public boolean isActive() { return active; }
        public long getRemainingSeconds() { return remainingSeconds; }
        public int getTotalSeconds() { return totalSeconds; }
    }
    
    /**
     * Event class for timer expiration
     */
    public static class TimerExpiredEvent {
        private final String gameCode;
        
        public TimerExpiredEvent(String gameCode) {
            this.gameCode = gameCode;
        }
        
        public String getGameCode() {
            return gameCode;
        }
    }
} 
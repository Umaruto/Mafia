package com.victadore.webmafia.mafia_web_of_lies.service;

import java.util.UUID;
import java.util.ArrayList;

import com.victadore.webmafia.mafia_web_of_lies.model.Game;
import com.victadore.webmafia.mafia_web_of_lies.model.GameState;
import com.victadore.webmafia.mafia_web_of_lies.model.Player;
import com.victadore.webmafia.mafia_web_of_lies.model.Role;
import com.victadore.webmafia.mafia_web_of_lies.repository.GameRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.victadore.webmafia.mafia_web_of_lies.exception.GameException;
import org.springframework.context.annotation.Lazy;
import com.victadore.webmafia.mafia_web_of_lies.service.PlayerStatisticsService;

@Service
@Transactional
public class GameService {
    private final GameRepository gameRepository;
    private final PlayerService playerService;
    private final ActionHistoryService actionHistoryService;
    private final PlayerStatisticsService playerStatisticsService;
    
    public GameService(GameRepository gameRepository, PlayerService playerService, 
                      @Lazy ActionHistoryService actionHistoryService,
                      @Lazy PlayerStatisticsService playerStatisticsService) {
        this.gameRepository = gameRepository;
        this.playerService = playerService;
        this.actionHistoryService = actionHistoryService;
        this.playerStatisticsService = playerStatisticsService;
    }
    
    public Game createGame(String createdBy){
        Game game = new Game();
        game.setGameCode(generateGameCode());
        game.setCreatedBy(createdBy);
        game.setGameState(GameState.WAITING_FOR_PLAYERS);
        game.setActive(true);
        game.setCurrentDay(0);
        game.setCurrentPhase(0);
        game.setMinPlayers(4);
        game.setMaxPlayers(15);
        
        // Initialize players list
        game.setPlayers(new ArrayList<>());
        
        // Add creator as first player
        Player creator = new Player();
        creator.setUsername(createdBy);
        creator.setGame(game);
        creator.setAlive(true);
        creator.setRole(Role.CITIZEN); // Default role, will be reassigned when game starts
        creator.setInvestigated(false);
        game.getPlayers().add(creator);
        
        return gameRepository.save(game);
    }

    public Game joinGame(String gameCode, String username) {
        Game game = gameRepository.findByGameCode(gameCode);
        if (game == null) {
            throw new GameException("Game not found");
        }
        if (game.getGameState() != GameState.WAITING_FOR_PLAYERS) {
            throw new GameException("Game already started");
        }
        
        // Initialize players list if null
        if (game.getPlayers() == null) {
            game.setPlayers(new ArrayList<>());
        }
        
        // Check if player already exists in the game
        boolean playerExists = game.getPlayers().stream()
            .anyMatch(p -> p.getUsername().equals(username));
        if (playerExists) {
            throw new GameException("Player already in game");
        }
        
        // Check if game is full
        if (game.getPlayers().size() >= game.getMaxPlayers()) {
            throw new GameException("Game is full");
        }
        
        // Create new player
        Player player = new Player();
        player.setUsername(username);
        player.setGame(game);
        player.setAlive(true);
        player.setRole(Role.CITIZEN); // Set default role as CITIZEN
        player.setInvestigated(false);
        
        // Add player to game
        game.getPlayers().add(player);
        
        // Save the game (this will also save the new player due to cascade)
        return gameRepository.save(game);
    }

    public Game startGame(String gameCode){
        Game game = gameRepository.findByGameCode(gameCode);
        if (game == null) {
            throw new GameException("Game not found");
        }
        if (game.getPlayers().size() < game.getMinPlayers()) {
            throw new GameException("Not enough players to start the game");
        }
        
        // Assign roles to players
        playerService.assignRoles(game);
        
        game.setGameState(GameState.IN_PROGRESS);
        game.setCurrentDay(1);
        game.setCurrentPhase(1); // 1 = Night phase (game starts with night), 0 = Day phase
        
        Game savedGame = gameRepository.save(game);
        
        // Record game start events in action history (after game is saved)
        try {
            actionHistoryService.recordGameEvent(savedGame.getId(), "GAME_START", 
                                               String.format("Game started with %d players", savedGame.getPlayers().size()),
                                               "Game successfully started");
            
            // Record role assignments for audit purposes (without revealing roles)
            for (Player player : savedGame.getPlayers()) {
                actionHistoryService.recordAction(savedGame.getId(), "ROLE_ASSIGNED", "SYSTEM", 
                                                player.getUsername(), 
                                                "Role assigned to player during game start",
                                                "Role assignment completed", true);
            }
            
            // Record the first phase transition
            actionHistoryService.recordPhaseTransition(savedGame.getId(), "SETUP", "NIGHT");
        } catch (Exception e) {
            // If action history recording fails, log but don't fail the game start
            System.err.println("Failed to record action history for game start: " + e.getMessage());
        }
        
        return savedGame;
    }

    public Game advanceToNightPhase(String gameCode) {
        Game game = gameRepository.findByGameCode(gameCode);
        if (game == null) {
            throw new GameException("Game not found");
        }
        if (game.getCurrentPhase() != 0) {
            throw new GameException("Game is not in day phase");
        }
        
        game.setCurrentPhase(1); // Night phase
        
        // Clear votes and reset night targets for night phase
        game.getVotes().clear();
        game.getPlayersWhoVoted().clear();
        game.setMafiaTarget(null);
        game.setDoctorTarget(null);
        
        return gameRepository.save(game);
    }

    public Game advanceToDayPhase(String gameCode) {
        Game game = gameRepository.findByGameCode(gameCode);
        if (game == null) {
            throw new GameException("Game not found");
        }
        if (game.getCurrentPhase() != 1) {
            throw new GameException("Game is not in night phase");
        }
        
        game.setCurrentDay(game.getCurrentDay() + 1);
        game.setCurrentPhase(0); // Day phase
        
        // Clear votes and night actions for new day
        game.getVotes().clear();
        game.getPlayersWhoVoted().clear();
        game.getPlayersWhoActedAtNight().clear();
        game.setMafiaTarget(null);
        game.setDoctorTarget(null);
        
        return gameRepository.save(game);
    }

    public String checkWinCondition(String gameCode) {
        Game game = gameRepository.findByGameCode(gameCode);
        if (game == null) {
            throw new GameException("Game not found");
        }
        
        long aliveMafia = game.getPlayers().stream()
            .filter(Player::isAlive)
            .filter(p -> p.getRole() == Role.MAFIA)
            .count();
            
        long aliveCitizens = game.getPlayers().stream()
            .filter(Player::isAlive)
            .filter(p -> p.getRole() != Role.MAFIA)
            .count();
        
        if (aliveMafia == 0) {
            game.setGameState(GameState.FINISHED);
            game.setWinner("CITIZENS");
            gameRepository.save(game);
            
            // Record citizen victory
            try {
                actionHistoryService.recordGameEvent(game.getId(), "GAME_END", 
                                                   "Citizens eliminated all Mafia members",
                                                   "Citizens victory - All Mafia eliminated");
            } catch (Exception e) {
                System.err.println("Failed to record game end event: " + e.getMessage());
            }
            
            // Update player statistics
            try {
                playerStatisticsService.updatePlayerStatisticsAfterGame(game.getGameCode());
            } catch (Exception e) {
                System.err.println("Failed to update player statistics: " + e.getMessage());
            }
            
            return "CITIZENS";
        } else if (aliveMafia >= aliveCitizens) {
            game.setGameState(GameState.FINISHED);
            game.setWinner("MAFIA");
            gameRepository.save(game);
            
            // Record Mafia victory
            try {
                actionHistoryService.recordGameEvent(game.getId(), "GAME_END", 
                                                   String.format("Mafia achieved majority control (%d Mafia vs %d Citizens)", 
                                                               aliveMafia, aliveCitizens),
                                                   "Mafia victory - Majority control achieved");
            } catch (Exception e) {
                System.err.println("Failed to record game end event: " + e.getMessage());
            }
            
            // Update player statistics
            try {
                playerStatisticsService.updatePlayerStatisticsAfterGame(game.getGameCode());
            } catch (Exception e) {
                System.err.println("Failed to update player statistics: " + e.getMessage());
            }
            
            return "MAFIA";
        }
        
        return "ONGOING";
    }

    public Game getGameByCode(String gameCode) {
        Game game = gameRepository.findByGameCode(gameCode);
        if (game == null) {
            throw new GameException("Game not found");
        }
        return game;
    }

    private String generateGameCode(){
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
   
}

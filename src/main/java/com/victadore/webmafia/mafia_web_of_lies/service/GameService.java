package com.victadore.webmafia.mafia_web_of_lies.service;

import java.util.UUID;

import com.victadore.webmafia.mafia_web_of_lies.model.Game;
import com.victadore.webmafia.mafia_web_of_lies.model.GameState;
import com.victadore.webmafia.mafia_web_of_lies.model.Player;
import com.victadore.webmafia.mafia_web_of_lies.repository.GameRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.victadore.webmafia.mafia_web_of_lies.exception.GameException;



@Service
@Transactional
public class GameService {
    private final GameRepository gameRepository;

    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
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
        game.setMaxPlayers(10);
        
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
        game.setGameState(GameState.IN_PROGRESS);
        return gameRepository.save(game);
        
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

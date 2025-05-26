package com.victadore.webmafia.mafia_web_of_lies.service;

import com.victadore.webmafia.mafia_web_of_lies.model.Player;
import com.victadore.webmafia.mafia_web_of_lies.model.Role;
import com.victadore.webmafia.mafia_web_of_lies.model.Game;
import com.victadore.webmafia.mafia_web_of_lies.repository.PlayerRepository;
import com.victadore.webmafia.mafia_web_of_lies.exception.GameException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
@Transactional
public class PlayerService {
    private final PlayerRepository playerRepository;
    private final Random random = new Random();

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    // Assign roles to players at the start of the game
    public void assignRoles(Game game) {
        List<Player> players = game.getPlayers();
        int playerCount = players.size();

        // Calculate number of Mafia based on player count
        int mafiaCount = calculateMafiaCount(playerCount);
        
        // Create a list of all roles we need to assign
        List<Role> rolesToAssign = new ArrayList<>();
        
        // Add Mafia roles
        for (int i = 0; i < mafiaCount; i++) {
            rolesToAssign.add(Role.MAFIA);
        }

        // Add special roles if enough players
        if (playerCount >= 5) {
            rolesToAssign.add(Role.DOCTOR);
            rolesToAssign.add(Role.DETECTIVE);
        }

        // Fill remaining slots with Citizens
        while (rolesToAssign.size() < playerCount) {
            rolesToAssign.add(Role.CITIZEN);
        }

        // Shuffle the roles
        Collections.shuffle(rolesToAssign);

        // Assign shuffled roles to players
        for (int i = 0; i < playerCount; i++) {
            players.get(i).setRole(rolesToAssign.get(i));
        }

        // Save all players
        playerRepository.saveAll(players);
    }

    // Calculate number of Mafia based on player count
    private int calculateMafiaCount(int playerCount) {
        if (playerCount <= 5) return 1;
        if (playerCount <= 10) return 2;
        return 3; // for 9+ players
    }


    // Get all alive players in a game
    public List<Player> getAlivePlayers(Long gameId) {
        return playerRepository.findByGameIdAndIsAliveTrue(gameId);
    }

    // Get all players with a specific role in a game
    public List<Player> getPlayersByRole(Long gameId, Role role) {
        return playerRepository.findByGameIdAndRole(gameId, role);
    }

    // Kill a player
    public void killPlayer(Long playerId) {
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new GameException("Player not found"));
        player.setAlive(false);
        playerRepository.save(player);
    }

    public void savePlayer(Long playerId) {
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new GameException("Player not found"));
        player.setAlive(true);
        playerRepository.save(player);
    }

    public boolean investigatePlayer(Long playerId) {
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new GameException("Player not found"));
        player.setInvestigated(true);    
        if (player.getRole() == Role.DETECTIVE) {
            return true;
        }
        playerRepository.save(player);
        return false;
    }

    // Check if a player is alive
    public boolean isPlayerAlive(Long playerId) {
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new GameException("Player not found"));
        return player.isAlive();
    }
}
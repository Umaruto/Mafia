package com.victadore.webmafia.mafia_web_of_lies.service;

import com.victadore.webmafia.mafia_web_of_lies.dto.VoteRequest;
import com.victadore.webmafia.mafia_web_of_lies.exception.ValidationException;
import com.victadore.webmafia.mafia_web_of_lies.model.Game;
import com.victadore.webmafia.mafia_web_of_lies.model.GameState;
import com.victadore.webmafia.mafia_web_of_lies.model.Player;
import com.victadore.webmafia.mafia_web_of_lies.model.Role;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class ValidationService {
    
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final Pattern GAME_CODE_PATTERN = Pattern.compile("^[A-Z0-9]{6}$");
    private static final int MIN_USERNAME_LENGTH = 2;
    private static final int MAX_USERNAME_LENGTH = 20;
    
    /**
     * Validates username format and length
     */
    public void validateUsername(String username) {
        Map<String, List<String>> errors = new HashMap<>();
        List<String> usernameErrors = new ArrayList<>();
        
        if (username == null || username.trim().isEmpty()) {
            usernameErrors.add("Username is required");
        } else {
            String trimmedUsername = username.trim();
            
            if (trimmedUsername.length() < MIN_USERNAME_LENGTH) {
                usernameErrors.add("Username must be at least " + MIN_USERNAME_LENGTH + " characters");
            }
            
            if (trimmedUsername.length() > MAX_USERNAME_LENGTH) {
                usernameErrors.add("Username cannot exceed " + MAX_USERNAME_LENGTH + " characters");
            }
            
            if (!USERNAME_PATTERN.matcher(trimmedUsername).matches()) {
                usernameErrors.add("Username can only contain letters, numbers, underscores, and hyphens");
            }
        }
        
        if (!usernameErrors.isEmpty()) {
            errors.put("username", usernameErrors);
            throw new ValidationException("Username validation failed", errors);
        }
    }
    
    /**
     * Validates game code format
     */
    public void validateGameCode(String gameCode) {
        Map<String, List<String>> errors = new HashMap<>();
        List<String> gameCodeErrors = new ArrayList<>();
        
        if (gameCode == null || gameCode.trim().isEmpty()) {
            gameCodeErrors.add("Game code is required");
        } else {
            String trimmedCode = gameCode.trim().toUpperCase();
            
            if (trimmedCode.length() != 6) {
                gameCodeErrors.add("Game code must be exactly 6 characters");
            }
            
            if (!GAME_CODE_PATTERN.matcher(trimmedCode).matches()) {
                gameCodeErrors.add("Game code must contain only uppercase letters and numbers");
            }
        }
        
        if (!gameCodeErrors.isEmpty()) {
            errors.put("gameCode", gameCodeErrors);
            throw new ValidationException("Game code validation failed", errors);
        }
    }
    
    /**
     * Validates if a player can perform an action in the current game state
     */
    public void validatePlayerAction(Game game, String username, String action) {
        Map<String, List<String>> errors = new HashMap<>();
        
        // Check if game exists and is active
        if (game == null) {
            errors.put("game", List.of("Game not found"));
            throw new ValidationException("Game validation failed", errors);
        }
        
        if (game.getGameState() != GameState.IN_PROGRESS) {
            errors.put("game", List.of("Game is not in progress"));
            throw new ValidationException("Game validation failed", errors);
        }
        
        // Find the player
        Player player = game.getPlayers().stream()
            .filter(p -> p.getUsername().equals(username))
            .findFirst()
            .orElse(null);
            
        if (player == null) {
            errors.put("player", List.of("Player not found in game"));
            throw new ValidationException("Player validation failed", errors);
        }
        
        if (!player.isAlive()) {
            errors.put("player", List.of("Dead players cannot perform actions"));
            throw new ValidationException("Player validation failed", errors);
        }
        
        // Validate action based on current phase
        if (game.getCurrentPhase() == 0) { // Day phase
            if (!"VOTE".equals(action) && !"SKIP_VOTE".equals(action)) {
                errors.put("action", List.of("Only voting actions are allowed during day phase"));
            }
        } else { // Night phase
            if ("VOTE".equals(action) || "SKIP_VOTE".equals(action)) {
                errors.put("action", List.of("Voting actions are not allowed during night phase"));
            }
        }
        
        if (!errors.isEmpty()) {
            throw new ValidationException("Action validation failed", errors);
        }
    }
    
    /**
     * Validates night action based on player role
     */
    public void validateNightAction(Game game, String actorUsername, String actionType) {
        Map<String, List<String>> errors = new HashMap<>();
        
        if (game.getCurrentPhase() != 1) {
            errors.put("phase", List.of("Night actions are only allowed during night phase"));
            throw new ValidationException("Phase validation failed", errors);
        }
        
        Player actor = game.getPlayers().stream()
            .filter(p -> p.getUsername().equals(actorUsername))
            .findFirst()
            .orElse(null);
            
        if (actor == null || !actor.isAlive()) {
            errors.put("actor", List.of("Actor not found or not alive"));
            throw new ValidationException("Actor validation failed", errors);
        }
        
        // Validate action type based on role
        List<String> actionErrors = new ArrayList<>();
        switch (actor.getRole()) {
            case MAFIA:
                if (!"KILL".equals(actionType)) {
                    actionErrors.add("Mafia players can only perform KILL actions");
                }
                break;
            case DOCTOR:
                if (!"SAVE".equals(actionType)) {
                    actionErrors.add("Doctor players can only perform SAVE actions");
                }
                break;
            case DETECTIVE:
                if (!"INVESTIGATE".equals(actionType)) {
                    actionErrors.add("Detective players can only perform INVESTIGATE actions");
                }
                break;
            case CITIZEN:
                actionErrors.add("Citizens cannot perform night actions");
                break;
            default:
                actionErrors.add("Unknown role cannot perform night actions");
        }
        
        if (!actionErrors.isEmpty()) {
            errors.put("actionType", actionErrors);
            throw new ValidationException("Night action validation failed", errors);
        }
    }
    
    /**
     * Validates vote request
     */
    public void validateVoteRequest(VoteRequest voteRequest, Game game) {
        Map<String, List<String>> errors = new HashMap<>();
        
        if (!voteRequest.isValid()) {
            errors.put("vote", List.of("Either skip must be true or target username must be provided"));
        }
        
        if (game.getCurrentPhase() != 0) {
            errors.put("phase", List.of("Voting is only allowed during day phase"));
        }
        
        // Check if voter exists and is alive
        Player voter = game.getPlayers().stream()
            .filter(p -> p.getUsername().equals(voteRequest.getVoterUsername()))
            .findFirst()
            .orElse(null);
            
        if (voter == null || !voter.isAlive()) {
            errors.put("voter", List.of("Voter not found or not alive"));
        } else {
            // Check if player has already voted
            if (game.getPlayersWhoVoted().contains(voter.getId())) {
                errors.put("voter", List.of("Player has already voted"));
            }
        }
        
        // If not skipping, validate target
        if (voteRequest.getSkip() != null && !voteRequest.getSkip() && voteRequest.getTargetUsername() != null) {
            Player target = game.getPlayers().stream()
                .filter(p -> p.getUsername().equals(voteRequest.getTargetUsername()))
                .findFirst()
                .orElse(null);
                
            if (target == null) {
                errors.put("target", List.of("Target player not found"));
            } else if (!target.isAlive()) {
                errors.put("target", List.of("Cannot vote for dead players"));
            } else if (target.getUsername().equals(voteRequest.getVoterUsername())) {
                errors.put("target", List.of("Cannot vote for yourself"));
            }
        }
        
        if (!errors.isEmpty()) {
            throw new ValidationException("Vote validation failed", errors);
        }
    }
    
    /**
     * Validates game creation parameters
     */
    public void validateGameCreation(String createdBy, Integer minPlayers, Integer maxPlayers) {
        Map<String, List<String>> errors = new HashMap<>();
        
        validateUsername(createdBy);
        
        if (minPlayers != null && (minPlayers < 4 || minPlayers > 15)) {
            errors.put("minPlayers", List.of("Minimum players must be between 4 and 15"));
        }
        
        if (maxPlayers != null && (maxPlayers < 4 || maxPlayers > 15)) {
            errors.put("maxPlayers", List.of("Maximum players must be between 4 and 15"));
        }
        
        if (minPlayers != null && maxPlayers != null && maxPlayers < minPlayers) {
            errors.put("maxPlayers", List.of("Maximum players must be greater than or equal to minimum players"));
        }
        
        if (!errors.isEmpty()) {
            throw new ValidationException("Game creation validation failed", errors);
        }
    }
} 
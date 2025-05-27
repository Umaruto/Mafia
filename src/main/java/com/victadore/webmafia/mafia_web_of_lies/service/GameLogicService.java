package com.victadore.webmafia.mafia_web_of_lies.service;

import com.victadore.webmafia.mafia_web_of_lies.model.*;
import com.victadore.webmafia.mafia_web_of_lies.repository.GameRepository;
import com.victadore.webmafia.mafia_web_of_lies.dto.*;
import com.victadore.webmafia.mafia_web_of_lies.exception.GameException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class GameLogicService {
    private final GameService gameService;
    private final PlayerService playerService;
    private final GameRepository gameRepository;
    private final WebSocketService webSocketService;

    public GameLogicService(GameService gameService, PlayerService playerService, GameRepository gameRepository, WebSocketService webSocketService) {
        this.gameService = gameService;
        this.playerService = playerService;
        this.gameRepository = gameRepository;
        this.webSocketService = webSocketService;
    }

    // Start a new day phase
    public Game startDayPhase(String gameCode) {
        Game game = gameRepository.findByGameCode(gameCode);
        if (game == null) {
            throw new GameException("Game not found");
        }

        game.setCurrentPhase(0); // 0 for day
        game.setCurrentDay(game.getCurrentDay() + 1);
        Game savedGame = gameRepository.save(game);
        
        // Broadcast phase change
        webSocketService.broadcastGameUpdate(gameCode, 
            new GameEvent("PHASE_CHANGE", gameCode, Map.of(
                "phase", "DAY",
                "day", savedGame.getCurrentDay()
            ))
        );
        
        return savedGame;
    }

    // Start night phase
    public Game startNightPhase(String gameCode) {
        Game game = gameRepository.findByGameCode(gameCode);
        if (game == null) {
            throw new GameException("Game not found");
        }

        game.setCurrentPhase(1); // 1 for night
        Game savedGame = gameRepository.save(game);
        
        // Broadcast phase change
        webSocketService.broadcastGameUpdate(gameCode, 
            new GameEvent("PHASE_CHANGE", gameCode, Map.of(
                "phase", "NIGHT",
                "day", savedGame.getCurrentDay()
            ))
        );
        
        return savedGame;
    }


    // Handle night actions
    public Game handleNightAction(String gameCode, String actorUsername, String targetUsername, String actionType) {
        Game game = gameRepository.findByGameCode(gameCode);
        if (game == null) {
            throw new GameException("Game not found");
        }

        if (game.getCurrentPhase() != 1) {
            throw new GameException("Night actions are only allowed during night phase");
        }

        Player actor = game.getPlayers().stream()
            .filter(p -> p.getUsername().equals(actorUsername))
            .findFirst()
            .orElseThrow(() -> new GameException("Actor not found"));

        Player target = game.getPlayers().stream()
            .filter(p -> p.getUsername().equals(targetUsername))
            .findFirst()
            .orElseThrow(() -> new GameException("Target not found"));

        // Process different night actions based on role
        switch (actor.getRole()) {
            case MAFIA:
                if (actionType.equals("KILL")) {
                    playerService.killPlayer(target.getId());
                    // Notify about the kill
                    webSocketService.broadcastGameUpdate(gameCode, 
                        new GameEvent("PLAYER_KILLED", gameCode, Map.of(
                            "target", target.getUsername()
                        ))
                    );
                }
                break;
            case DOCTOR:
                if (actionType.equals("SAVE")) {
                    playerService.savePlayer(target.getId());
                    // Notify about the save
                    webSocketService.sendPrivateMessage(actor.getUsername(), 
                        new GameEvent("PLAYER_SAVED", gameCode, Map.of(
                            "target", target.getUsername()
                        ))
                    );
                }
                break;
            case DETECTIVE:
                if (actionType.equals("INVESTIGATE")) {
                    boolean isDetective = playerService.investigatePlayer(target.getId());
                    // Send investigation result only to the detective
                    webSocketService.sendPrivateMessage(actor.getUsername(), 
                        new GameEvent("INVESTIGATION_RESULT", gameCode, Map.of(
                            "target", target.getUsername(),
                            "isDetective", isDetective
                        ))
                    );
                }else if (actionType.equals("KILL")) {
                    playerService.killPlayer(target.getId());
                    // Notify about the kill
                    webSocketService.broadcastGameUpdate(gameCode, 
                        new GameEvent("PLAYER_KILLED", gameCode, Map.of(
                            "target", target.getUsername()
                        ))
                    );
                }
                
                break;
            default:
                throw new GameException("Invalid night action for role");
        }

        return gameRepository.save(game);
    }

    // Check win conditions
    public GameState checkWinConditions(Game game) {
        List<Player> alivePlayers = playerService.getAlivePlayers(game.getId());
        long mafiaCount = alivePlayers.stream()
            .filter(p -> p.getRole() == Role.MAFIA)
            .count();
        long citizenCount = alivePlayers.size() - mafiaCount;

        if (mafiaCount == 0) {
            return GameState.FINISHED; // Citizens win
        }
        if (mafiaCount >= citizenCount) {
            return GameState.FINISHED; // Mafia win
        }
        return GameState.IN_PROGRESS; // Game continues
    }

    public Game handleVote(String gameCode, VoteRequest voteRequest) {
        Game game = gameRepository.findByGameCode(gameCode);
        if (game == null) {
            throw new GameException("Game not found");
        }

        if (game.getCurrentPhase() != 0) {
            throw new GameException("Voting is only allowed during day phase");
        }

        Player voter = game.getPlayers().stream()
            .filter(p -> p.getUsername().equals(voteRequest.getVoterUsername()))
            .findFirst()
            .orElseThrow(() -> new GameException("Voter not found"));

        if (!voter.isAlive()) {
            throw new GameException("Dead players cannot vote");
        }

        // Check if player has already voted
        if (game.getPlayersWhoVoted().contains(voter.getId())) {
            throw new GameException("You have already voted");
        }

        // Handle skip vote
        if (voteRequest.isSkip()) {
            game.getPlayersWhoVoted().add(voter.getId());
            return gameRepository.save(game);
        }

        // Handle normal vote
        if (voteRequest.getTargetUsername() == null) {
            throw new GameException("Target username is required for voting");
        }

        Player target = game.getPlayers().stream()
            .filter(p -> p.getUsername().equals(voteRequest.getTargetUsername()))
            .findFirst()
            .orElseThrow(() -> new GameException("Target not found"));

        if (!target.isAlive()) {
            throw new GameException("Cannot vote for dead players");
        }

        // Process the vote
        processVote(game, voter, target);
        Game savedGame = gameRepository.save(game);
        
        // Broadcast vote
        webSocketService.broadcastGameUpdate(gameCode, 
            new GameEvent("VOTE_CAST", gameCode, Map.of(
                "voter", voteRequest.getVoterUsername(),
                "target", voteRequest.getTargetUsername(),
                "skip", voteRequest.isSkip()
            ))
        );
        
        return savedGame;
    }

    private void processVote(Game game, Player voter, Player target) {
        // Add vote
        Map<Long, Integer> votes = game.getVotes();
        votes.put(target.getId(), votes.getOrDefault(target.getId(), 0) + 1);
        
        // Mark player as voted
        game.getPlayersWhoVoted().add(voter.getId());
        
        // Check if voting phase is complete
        int totalVotes = (int) game.getPlayersWhoVoted().size();
        int requiredVotes = (int) game.getPlayers().stream()
            .filter(Player::isAlive)
            .count();
        
        if (totalVotes >= requiredVotes) {
            // Find player with most votes
            Long playerToEliminate = votes.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
                
            if (playerToEliminate != null) {
                // Eliminate the player
                playerService.killPlayer(playerToEliminate);
                
                // Clear votes and voting status for next round
                votes.clear();
                game.getPlayersWhoVoted().clear();
                
                // Check win conditions
                GameState newState = checkWinConditions(game);
                if (newState == GameState.FINISHED) {
                    game.setGameState(newState);
                }
            }
        }
    }

    // Add method to check if a player has voted (including skip)
    public boolean hasPlayerVoted(String gameCode, String username) {
        Game game = gameRepository.findByGameCode(gameCode);
        if (game == null) {
            throw new GameException("Game not found");
        }
        
        return game.getPlayers().stream()
            .filter(p -> p.getUsername().equals(username))
            .findFirst()
            .map(p -> game.getPlayersWhoVoted().contains(p.getId()))
            .orElse(false);
    }

    // Add method to get voting status
    public Map<String, String> getVotingStatus(String gameCode) {
        Game game = gameRepository.findByGameCode(gameCode);
        if (game == null) {
            throw new GameException("Game not found");
        }

        Map<String, String> status = new HashMap<>();
        for (Player player : game.getPlayers()) {
            if (player.isAlive()) {
                status.put(player.getUsername(), 
                    game.getPlayersWhoVoted().contains(player.getId()) ? "Voted" : "Not Voted");
            }
        }
        return status;
    }
}
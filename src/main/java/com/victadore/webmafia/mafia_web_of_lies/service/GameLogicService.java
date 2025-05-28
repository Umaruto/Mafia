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

        if (!actor.isAlive()) {
            throw new GameException("Dead players cannot perform night actions");
        }

        Player target = game.getPlayers().stream()
            .filter(p -> p.getUsername().equals(targetUsername))
            .findFirst()
            .orElseThrow(() -> new GameException("Target not found"));

        if (!target.isAlive()) {
            throw new GameException("Cannot target dead players");
        }

        // Process different night actions based on role
        switch (actor.getRole()) {
            case MAFIA:
                if (actionType.equals("KILL")) {
                    // Allow each Mafia member to vote for their preferred target
                    game.getMafiaVotes().put(actor.getId(), target.getId());
                    game.getPlayersWhoActedAtNight().add(actor.getId());
                    
                    // Check if all living Mafia members have voted
                    long livingMafiaCount = game.getPlayers().stream()
                        .filter(Player::isAlive)
                        .filter(p -> p.getRole() == Role.MAFIA)
                        .count();
                    
                    long mafiaVotesCount = game.getMafiaVotes().size();
                    
                    if (mafiaVotesCount >= livingMafiaCount) {
                        // All Mafia have voted, determine the target by majority
                        Long chosenTarget = determineMafiaTarget(game);
                        game.setMafiaTarget(chosenTarget);
                        
                        // Notify all Mafia members about the final decision
                        Player finalTarget = game.getPlayers().stream()
                            .filter(p -> p.getId().equals(chosenTarget))
                            .findFirst()
                            .orElse(null);
                            
                        if (finalTarget != null) {
                            game.getPlayers().stream()
                                .filter(p -> p.getRole() == Role.MAFIA && p.isAlive())
                                .forEach(mafiaPlayer -> {
                                    webSocketService.sendPrivateMessage(mafiaPlayer.getUsername(), 
                                        new GameEvent("MAFIA_TARGET_CHOSEN", gameCode, Map.of(
                                            "target", finalTarget.getUsername(),
                                            "message", "Your team has chosen " + finalTarget.getUsername() + " as the target."
                                        ))
                                    );
                                });
                        }
                    } else {
                        // Notify this Mafia member that their vote was recorded
                        webSocketService.sendPrivateMessage(actor.getUsername(), 
                            new GameEvent("MAFIA_VOTE_RECORDED", gameCode, Map.of(
                                "target", target.getUsername(),
                                "votesNeeded", livingMafiaCount - mafiaVotesCount,
                                "message", "Your vote for " + target.getUsername() + " has been recorded. Waiting for " + (livingMafiaCount - mafiaVotesCount) + " more Mafia votes."
                            ))
                        );
                        
                        // Notify other Mafia members about this vote
                        game.getPlayers().stream()
                            .filter(p -> p.getRole() == Role.MAFIA && p.isAlive() && !p.getId().equals(actor.getId()))
                            .forEach(mafiaPlayer -> {
                                webSocketService.sendPrivateMessage(mafiaPlayer.getUsername(), 
                                    new GameEvent("MAFIA_TEAMMATE_VOTED", gameCode, Map.of(
                                        "voter", actor.getUsername(),
                                        "target", target.getUsername(),
                                        "message", actor.getUsername() + " voted to eliminate " + target.getUsername()
                                    ))
                                );
                            });
                    }
                }
                break;
            case DOCTOR:
                if (actionType.equals("SAVE")) {
                    // Check if doctor has already acted this night
                    if (game.getPlayersWhoActedAtNight().contains(actor.getId())) {
                        throw new GameException("You have already performed your night action");
                    }
                    game.setDoctorTarget(target.getId());
                    game.getPlayersWhoActedAtNight().add(actor.getId());
                    
                    // Send confirmation to doctor
                    webSocketService.sendPrivateMessage(actor.getUsername(), 
                        new GameEvent("PLAYER_SAVED", gameCode, Map.of(
                            "target", target.getUsername()
                        ))
                    );
                }
                break;
            case DETECTIVE:
                if (actionType.equals("INVESTIGATE")) {
                    // Check if detective has already acted this night
                    if (game.getPlayersWhoActedAtNight().contains(actor.getId())) {
                        throw new GameException("You have already performed your night action");
                    }
                    boolean isMafia = target.getRole() == Role.MAFIA;
                    playerService.investigatePlayer(target.getId());
                    game.getPlayersWhoActedAtNight().add(actor.getId());
                    
                    // Send clear investigation result to detective
                    String resultMessage = isMafia ? 
                        target.getUsername() + " is MAFIA! They are your enemy." :
                        target.getUsername() + " is INNOCENT. They are not Mafia.";
                    
                    webSocketService.sendPrivateMessage(actor.getUsername(), 
                        new GameEvent("INVESTIGATION_RESULT", gameCode, Map.of(
                            "target", target.getUsername(),
                            "isMafia", isMafia,
                            "message", resultMessage
                        ))
                    );
                }
                break;
            default:
                throw new GameException("Invalid night action for role: " + actor.getRole());
        }

        // Remove the automatic marking of all Mafia members - each acts individually now

        // Check if all night actions are complete
        checkAndTransitionToDay(game);

        return gameRepository.save(game);
    }

    private void checkAndTransitionToDay(Game game) {
        // Count players with night actions who are still alive
        long mafiaCount = game.getPlayers().stream()
            .filter(Player::isAlive)
            .filter(p -> p.getRole() == Role.MAFIA)
            .count();
        
        long doctorCount = game.getPlayers().stream()
            .filter(Player::isAlive)
            .filter(p -> p.getRole() == Role.DOCTOR)
            .count();
            
        long detectiveCount = game.getPlayers().stream()
            .filter(Player::isAlive)
            .filter(p -> p.getRole() == Role.DETECTIVE)
            .count();

        // Calculate expected actions: 1 for Mafia team (regardless of count), 1 for Doctor, 1 for Detective
        long expectedActions = (mafiaCount > 0 ? 1 : 0) + doctorCount + detectiveCount;
        
        // Check if all expected night actions are complete
        // For Mafia: either one has acted OR none are alive
        // For Doctor/Detective: each individual must act
        boolean mafiaActed = mafiaCount == 0 || game.getMafiaTarget() != null;
        boolean doctorActed = doctorCount == 0 || game.getPlayersWhoActedAtNight().stream()
            .anyMatch(playerId -> game.getPlayers().stream()
                .anyMatch(p -> p.getId().equals(playerId) && p.getRole() == Role.DOCTOR));
        boolean detectiveActed = detectiveCount == 0 || game.getPlayersWhoActedAtNight().stream()
            .anyMatch(playerId -> game.getPlayers().stream()
                .anyMatch(p -> p.getId().equals(playerId) && p.getRole() == Role.DETECTIVE));

        if (mafiaActed && doctorActed && detectiveActed) {
            // Process night actions
            processNightActions(game);
            
            // Transition to day phase
            game.setCurrentPhase(0); // Day phase
            game.setCurrentDay(game.getCurrentDay() + 1);
            
            // Clear night actions for next night
            game.getPlayersWhoActedAtNight().clear();
            game.setMafiaTarget(null);
            game.setDoctorTarget(null);
            game.getMafiaVotes().clear(); // Clear Mafia votes for next night
            
            // Check win conditions before starting new day
            GameState newState = checkWinConditions(game);
            if (newState == GameState.FINISHED) {
                game.setGameState(newState);
                return;
            }
            
            // Broadcast phase change
            webSocketService.broadcastGameUpdate(game.getGameCode(), 
                new GameEvent("PHASE_CHANGE", game.getGameCode(), Map.of(
                    "phase", "DAY",
                    "day", game.getCurrentDay()
                ))
            );
        }
    }

    private void processNightActions(Game game) {
        // Process Mafia kill and Doctor save
        if (game.getMafiaTarget() != null) {
            boolean wasSaved = false;
            
            // Check if doctor saved the target
            if (game.getDoctorTarget() != null && game.getDoctorTarget().equals(game.getMafiaTarget())) {
                wasSaved = true;
                // Notify that someone was saved
                webSocketService.broadcastGameUpdate(game.getGameCode(), 
                    new GameEvent("PLAYER_SAVED_ANONYMOUSLY", game.getGameCode(), Map.of(
                        "message", "The doctor saved someone tonight!"
                    ))
                );
            }
            
            if (!wasSaved) {
                // Kill the target
                playerService.killPlayer(game.getMafiaTarget());
                
                // Find the killed player's name
                Player killedPlayer = game.getPlayers().stream()
                    .filter(p -> p.getId().equals(game.getMafiaTarget()))
                    .findFirst()
                    .orElse(null);
                    
                if (killedPlayer != null) {
                    // Notify about the death (revealed during day phase)
                    webSocketService.broadcastGameUpdate(game.getGameCode(), 
                        new GameEvent("PLAYER_DIED_LAST_NIGHT", game.getGameCode(), Map.of(
                            "target", killedPlayer.getUsername()
                        ))
                    );
                }
            }
        } else {
            // No one was targeted by Mafia
            webSocketService.broadcastGameUpdate(game.getGameCode(), 
                new GameEvent("NO_DEATHS_LAST_NIGHT", game.getGameCode(), Map.of(
                    "message", "No one died last night."
                ))
            );
        }
    }

    // Check win conditions
    public GameState checkWinConditions(Game game) {
        List<Player> alivePlayers = playerService.getAlivePlayers(game.getId());
        long mafiaCount = alivePlayers.stream()
            .filter(p -> p.getRole() == Role.MAFIA)
            .count();
        long citizenCount = alivePlayers.size() - mafiaCount;

        if (mafiaCount == 0) {
            game.setWinner("CITIZENS");
            return GameState.FINISHED; // Citizens win
        }
        if (mafiaCount >= citizenCount) {
            game.setWinner("MAFIA");
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
            // Track skip vote as null target
            game.getIndividualVotes().put(voter.getId(), null);
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
        
        // Track individual vote
        game.getIndividualVotes().put(voter.getId(), target.getId());
        
        // Mark player as voted
        game.getPlayersWhoVoted().add(voter.getId());
        
        // Check if voting phase is complete
        int totalVotes = (int) game.getPlayersWhoVoted().size();
        int requiredVotes = (int) game.getPlayers().stream()
            .filter(Player::isAlive)
            .count();
        
        if (totalVotes >= requiredVotes) {
            // Find the highest vote count
            int maxVotes = votes.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
            
            // Find all players with the highest vote count
            List<Long> playersWithMaxVotes = votes.entrySet().stream()
                .filter(entry -> entry.getValue() == maxVotes)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
            
            // Only eliminate if there's exactly one player with the most votes (no tie)
            if (playersWithMaxVotes.size() == 1 && maxVotes > 0) {
                Long playerToEliminate = playersWithMaxVotes.get(0);
                
                // Eliminate the player
                playerService.killPlayer(playerToEliminate);
                
                // Broadcast elimination
                Player eliminatedPlayer = game.getPlayers().stream()
                    .filter(p -> p.getId().equals(playerToEliminate))
                    .findFirst()
                    .orElse(null);
                    
                if (eliminatedPlayer != null) {
                    webSocketService.broadcastGameUpdate(game.getGameCode(), 
                        new GameEvent("PLAYER_ELIMINATED", game.getGameCode(), Map.of(
                            "target", eliminatedPlayer.getUsername(),
                            "reason", "voting"
                        ))
                    );
                }
                
                // Check win conditions after elimination
                GameState newState = checkWinConditions(game);
                if (newState == GameState.FINISHED) {
                    game.setGameState(newState);
                    // Clear votes and voting status
                    votes.clear();
                    game.getPlayersWhoVoted().clear();
                    game.getIndividualVotes().clear();
                    return; // Don't transition phases if game is over
                }
            } else if (playersWithMaxVotes.size() > 1) {
                // Handle tie vote - no elimination
                List<String> tiedPlayers = playersWithMaxVotes.stream()
                    .map(playerId -> game.getPlayers().stream()
                        .filter(p -> p.getId().equals(playerId))
                        .map(Player::getUsername)
                        .findFirst()
                        .orElse("Unknown"))
                    .collect(Collectors.toList());
                
                webSocketService.broadcastGameUpdate(game.getGameCode(), 
                    new GameEvent("VOTE_TIE", game.getGameCode(), Map.of(
                        "message", "Vote ended in a tie between: " + String.join(", ", tiedPlayers) + ". No one is eliminated.",
                        "tiedPlayers", tiedPlayers
                    ))
                );
            } else {
                // No votes or all votes were skipped
                webSocketService.broadcastGameUpdate(game.getGameCode(), 
                    new GameEvent("NO_ELIMINATION", game.getGameCode(), Map.of(
                        "message", "No one received enough votes. No elimination this round."
                    ))
                );
            }
            
            // Clear votes and voting status for next round (regardless of outcome)
            votes.clear();
            game.getPlayersWhoVoted().clear();
            game.getIndividualVotes().clear();
            
            // Automatically transition to night phase after voting is complete
            game.setCurrentPhase(1); // Night phase
            webSocketService.broadcastGameUpdate(game.getGameCode(), 
                new GameEvent("PHASE_CHANGE", game.getGameCode(), Map.of(
                    "phase", "NIGHT",
                    "day", game.getCurrentDay()
                ))
            );
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
                if (game.getPlayersWhoVoted().contains(player.getId())) {
                    Long targetId = game.getIndividualVotes().get(player.getId());
                    if (targetId == null) {
                        status.put(player.getUsername(), "Voted (Skip)");
                    } else {
                        String targetName = game.getPlayers().stream()
                            .filter(p -> p.getId().equals(targetId))
                            .map(Player::getUsername)
                            .findFirst()
                            .orElse("Unknown");
                        status.put(player.getUsername(), "Voted for " + targetName);
                    }
                } else {
                    status.put(player.getUsername(), "Not Voted");
                }
            }
        }
        return status;
    }

    public Game advancePhase(String gameCode) {
        Game game = gameRepository.findByGameCode(gameCode);
        if (game == null) {
            throw new GameException("Game not found");
        }

        if (game.getCurrentPhase() == 0) {
            // Day to Night transition
            game.setCurrentPhase(1);
            game.getVotes().clear();
            game.getPlayersWhoVoted().clear();
            game.getIndividualVotes().clear();
            game.setMafiaTarget(null);
            game.setDoctorTarget(null);
            game.getMafiaVotes().clear();
            
            webSocketService.broadcastGameUpdate(gameCode, 
                new GameEvent("PHASE_CHANGE", gameCode, Map.of(
                    "phase", "NIGHT",
                    "day", game.getCurrentDay()
                ))
            );
        } else {
            // Night to Day transition - process night actions first
            processNightActions(game);
            
            game.setCurrentPhase(0);
            game.setCurrentDay(game.getCurrentDay() + 1);
            game.getPlayersWhoActedAtNight().clear();
            game.setMafiaTarget(null);
            game.setDoctorTarget(null);
            game.getMafiaVotes().clear();
            
            // Check win conditions
            GameState newState = checkWinConditions(game);
            if (newState == GameState.FINISHED) {
                game.setGameState(newState);
            }
            
            webSocketService.broadcastGameUpdate(gameCode, 
                new GameEvent("PHASE_CHANGE", gameCode, Map.of(
                    "phase", "DAY",
                    "day", game.getCurrentDay()
                ))
            );
        }

        return gameRepository.save(game);
    }

    private Long determineMafiaTarget(Game game) {
        Map<Long, Integer> targetVotes = new HashMap<>();
        
        // Count votes for each target
        for (Long targetId : game.getMafiaVotes().values()) {
            targetVotes.put(targetId, targetVotes.getOrDefault(targetId, 0) + 1);
        }
        
        // Find the target with the most votes
        return targetVotes.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
}
package com.victadore.webmafia.mafia_web_of_lies.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.victadore.webmafia.mafia_web_of_lies.model.Player;
import com.victadore.webmafia.mafia_web_of_lies.repository.PlayerRepository;

import org.springframework.messaging.simp.SimpMessagingTemplate;

@Service
public class GameService {
    
    private final PlayerRepository playerRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final Random random = new Random();
    
    // Game state variables
    private Map<String, List<Player>> rooms = new HashMap<>();
    private Map<String, String> gamePhases = new HashMap<>();
    private Map<String, Map<String, Integer>> votes = new HashMap<>();
    private Map<String, Set<String>> playersDoneVoting = new HashMap<>();
    private Map<String, Map<String, String>> playerActions = new HashMap<>();
    private Map<String, Integer> phaseTimers = new HashMap<>();
    private Map<String, Boolean> roomsInSession = new HashMap<>();
    private Map<String, String> lastSaved = new HashMap<>(); // Track Doctor's last saved player
    private Map<String, String> lastInvestigated = new HashMap<>(); // Track Detective's last investigated player
    
    @Autowired
    public GameService(PlayerRepository playerRepository, SimpMessagingTemplate messagingTemplate) {
        this.playerRepository = playerRepository;
        this.messagingTemplate = messagingTemplate;
    }
    
    // Create a new game room
    public String createRoom(String hostUsername) {
        String roomCode = generateRoomCode();
        rooms.put(roomCode, new ArrayList<>());
        gamePhases.put(roomCode, "LOBBY");
        votes.put(roomCode, new HashMap<>());
        playersDoneVoting.put(roomCode, new HashSet<>());
        playerActions.put(roomCode, new HashMap<>());
        roomsInSession.put(roomCode, false);
        
        // Create and add host player
        Player host = new Player();
        host.setUsername(hostUsername);
        host.setRoomCode(roomCode);
        host.setHost(true);
        host.setAlive(true);
        
        playerRepository.save(host);
        rooms.get(roomCode).add(host);
        
        return roomCode;
    }
    
    // Join an existing room
    public boolean joinRoom(String username, String roomCode) {
        if (!rooms.containsKey(roomCode) || roomsInSession.get(roomCode)) {
            return false;
        }
        
        // Check if username already exists in the room
        boolean usernameExists = rooms.get(roomCode).stream()
                .anyMatch(p -> p.getUsername().equals(username));
        
        if (usernameExists) {
            return false;
        }
        
        Player player = new Player();
        player.setUsername(username);
        player.setRoomCode(roomCode);
        player.setHost(false);
        player.setAlive(true);
        
        playerRepository.save(player);
        rooms.get(roomCode).add(player);
        
        // Notify all players about the updated player list
        messagingTemplate.convertAndSend("/game/" + roomCode + "/players", rooms.get(roomCode));
        
        return true;
    }
    
    // Start the game
    public boolean startGame(String roomCode) {
        List<Player> players = rooms.get(roomCode);
        
        if (players.size() < 4) {
            return false; // Need at least 4 players
        }
        
        roomsInSession.put(roomCode, true);
        
        // Assign roles
        assignRoles(roomCode);
        
        // Set initial game phase to NIGHT
        gamePhases.put(roomCode, "NIGHT");
        phaseTimers.put(roomCode, 60); // 60 seconds for night phase
        
        // Start phase timer
        startPhaseTimer(roomCode);
        
        // Notify all players about game start and their roles
        messagingTemplate.convertAndSend("/game/" + roomCode + "/phase", gamePhases.get(roomCode));
        
        // Individually send role info
        for (Player player : players) {
            messagingTemplate.convertAndSendToUser(
                player.getUsername(), 
                "/game/" + roomCode + "/role", 
                player.getRole()
            );
        }
        
        return true;
    }
    
    // Assign roles to players
    private void assignRoles(String roomCode) {
        List<Player> players = rooms.get(roomCode);
        
        // Shuffle player list for random assignment
        Collections.shuffle(players);
        
        int mafiaCount = players.size() / 4; // 25% are mafia
        int doctorCount = 1;
        int detectiveCount = 1;
        
        for (int i = 0; i < players.size(); i++) {
            if (i < mafiaCount) {
                players.get(i).setRole("MAFIA");
            } else if (i < mafiaCount + doctorCount) {
                players.get(i).setRole("DOCTOR");
            } else if (i < mafiaCount + doctorCount + detectiveCount) {
                players.get(i).setRole("DETECTIVE");
            } else {
                players.get(i).setRole("CIVILIAN");
            }
            playerRepository.save(players.get(i));
        }
    }
    
    // Toggle game phase (day/night)
    public void togglePhase(String roomCode) {
        if (!gamePhases.containsKey(roomCode)) {
            return;
        }
        
        String currentPhase = gamePhases.get(roomCode);
        
        // Process actions from the last phase
        if (currentPhase.equals("NIGHT")) {
            // Process night phase actions (kills, saves, investigations)
            processNightActions(roomCode);
            gamePhases.put(roomCode, "DAY");
            phaseTimers.put(roomCode, 120); // 120 seconds for day phase
        } else {
            // Process day phase actions (votes)
            processDayVotes(roomCode);
            gamePhases.put(roomCode, "NIGHT");
            phaseTimers.put(roomCode, 60); // 60 seconds for night phase
        }
        
        // Reset votes for the new phase
        votes.put(roomCode, new HashMap<>());
        playersDoneVoting.put(roomCode, new HashSet<>());
        playerActions.put(roomCode, new HashMap<>());
        
        // Check game end condition
        boolean gameOver = checkGameEnd(roomCode);
        
        if (!gameOver) {
            // Start phase timer
            startPhaseTimer(roomCode);
            
            // Send updated game state to all players
            messagingTemplate.convertAndSend("/game/" + roomCode + "/phase", gamePhases.get(roomCode));
            sendPlayerUpdates(roomCode);
        }
    }
    
    // Process night actions
    private void processNightActions(String roomCode) {
        Map<String, String> actions = playerActions.get(roomCode);
        List<Player> players = rooms.get(roomCode);
        
        // Doctor save target
        String savedPlayer = null;
        for (Player player : players) {
            if (player.getRole().equals("DOCTOR") && player.isAlive()) {
                String target = actions.get(player.getUsername());
                if (target != null) {
                    savedPlayer = target;
                    lastSaved.put(roomCode + "_" + player.getUsername(), savedPlayer);
                }
            }
        }
        
        // Mafia kill target
        Set<String> mafiaTargets = new HashSet<>();
        for (Player player : players) {
            if (player.getRole().equals("MAFIA") && player.isAlive()) {
                String target = actions.get(player.getUsername());
                if (target != null) {
                    mafiaTargets.add(target);
                }
            }
        }
        
        // Find the most targeted player
        String mostTargeted = null;
        int maxTargets = 0;
        
        Map<String, Integer> targetCounts = new HashMap<>();
        for (String target : mafiaTargets) {
            targetCounts.put(target, targetCounts.getOrDefault(target, 0) + 1);
            if (targetCounts.get(target) > maxTargets) {
                maxTargets = targetCounts.get(target);
                mostTargeted = target;
            }
        }
        
        // Handle tie by randomly selecting one of the tied targets
        if (mostTargeted != null) {
            List<String> tiedTargets = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : targetCounts.entrySet()) {
                if (entry.getValue() == maxTargets) {
                    tiedTargets.add(entry.getKey());
                }
            }
            
            if (tiedTargets.size() > 1) {
                mostTargeted = tiedTargets.get(random.nextInt(tiedTargets.size()));
            }
            
            // If most targeted player was not saved by doctor, eliminate them
            if (savedPlayer == null || !savedPlayer.equals(mostTargeted)) {
                for (Player player : players) {
                    if (player.getUsername().equals(mostTargeted)) {
                        player.setAlive(false);
                        playerRepository.save(player);
                        
                        // Notify all players about the elimination
                        messagingTemplate.convertAndSend(
                            "/game/" + roomCode + "/elimination",
                            Map.of("player", mostTargeted, "byVote", false)
                        );
                        break;
                    }
                }
            } else {
                // Notify players that a save occurred (without revealing who was saved)
                messagingTemplate.convertAndSend(
                    "/game/" + roomCode + "/game-log",
                    "The Doctor successfully saved someone from the Mafia!"
                );
            }
        }
        
        // Process detective investigations
        for (Player player : players) {
            if (player.getRole().equals("DETECTIVE") && player.isAlive()) {
                String investigationTarget = actions.get(player.getUsername());
                if (investigationTarget != null) {
                    lastInvestigated.put(roomCode + "_" + player.getUsername(), investigationTarget);
                    
                    String targetRole = "UNKNOWN";
                    for (Player targetPlayer : players) {
                        if (targetPlayer.getUsername().equals(investigationTarget)) {
                            targetRole = targetPlayer.getRole().equals("MAFIA") ? "MAFIA" : "NOT_MAFIA";
                            break;
                        }
                    }
                    
                    // Send investigation result only to the detective
                    messagingTemplate.convertAndSendToUser(
                        player.getUsername(),
                        "/game/" + roomCode + "/investigation",
                        Map.of("target", investigationTarget, "result", targetRole)
                    );
                }
            }
        }
    }
    
    // Process day votes
    private void processDayVotes(String roomCode) {
        Map<String, Integer> voteCount = new HashMap<>();
        Map<String, Integer> playerVotes = votes.getOrDefault(roomCode, new HashMap<>());
        List<Player> players = rooms.get(roomCode);
        
        // Count votes for each player
        for (Integer targetId : playerVotes.values()) {
            for (Player player : players) {
                if (player.getId().equals(targetId)) {
                    String targetName = player.getUsername();
                    voteCount.put(targetName, voteCount.getOrDefault(targetName, 0) + 1);
                    break;
                }
            }
        }
        
        // Find player with most votes
        String mostVoted = null;
        int maxVotes = 0;
        
        for (Map.Entry<String, Integer> entry : voteCount.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                mostVoted = entry.getKey();
            }
        }
        
        // Handle ties by selecting randomly
        List<String> tiedPlayers = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : voteCount.entrySet()) {
            if (entry.getValue() == maxVotes) {
                tiedPlayers.add(entry.getKey());
            }
        }
        
        if (tiedPlayers.size() > 1) {
            // Randomize among tied players
            mostVoted = tiedPlayers.get(random.nextInt(tiedPlayers.size()));
            
            // Log that there was a tie
            messagingTemplate.convertAndSend(
                "/game/" + roomCode + "/game-log",
                "There was a tie in the voting! The town randomly selected someone to eliminate."
            );
        }
        
        // Eliminate the most voted player if there were any votes
        if (mostVoted != null && maxVotes > 0) {
            for (Player player : players) {
                if (player.getUsername().equals(mostVoted)) {
                    player.setAlive(false);
                    playerRepository.save(player);
                    
                    // Notify clients about the elimination
                    messagingTemplate.convertAndSend(
                        "/game/" + roomCode + "/elimination",
                        Map.of("player", mostVoted, "byVote", true, "voteCount", maxVotes)
                    );
                    break;
                }
            }
        } else {
            messagingTemplate.convertAndSend(
                "/game/" + roomCode + "/game-log",
                "No one was eliminated today. The town couldn't reach a consensus."
            );
        }
    }
    
    // Check if the game has ended
    private boolean checkGameEnd(String roomCode) {
        List<Player> players = rooms.get(roomCode);
        int mafiaCount = 0;
        int nonMafiaCount = 0;
        
        for (Player player : players) {
            if (player.isAlive()) {
                if (player.getRole().equals("MAFIA")) {
                    mafiaCount++;
                } else {
                    nonMafiaCount++;
                }
            }
        }
        
        if (mafiaCount == 0) {
            // Town wins
            messagingTemplate.convertAndSend(
                "/game/" + roomCode + "/game-over",
                Map.of("winner", "TOWN", "message", "All mafia have been eliminated. The town wins!")
            );
            return true;
        } else if (mafiaCount >= nonMafiaCount) {
            // Mafia wins
            messagingTemplate.convertAndSend(
                "/game/" + roomCode + "/game-over",
                Map.of("winner", "MAFIA", "message", "The mafia outnumbers the town. The mafia wins!")
            );
            return true;
        }
        
        return false;
    }
    
    // Submit role-specific action
    public void submitAction(String roomCode, String username, String targetUsername) {
        playerActions.computeIfAbsent(roomCode, k -> new HashMap<>())
                     .put(username, targetUsername);
        
        // Check if all players have submitted actions
        checkAllActionsSubmitted(roomCode);
    }
    
    // Submit a vote during day phase
    public void submitVote(String roomCode, String username, Integer targetId) {
        votes.computeIfAbsent(roomCode, k -> new HashMap<>())
             .put(username, targetId);
        playersDoneVoting.computeIfAbsent(roomCode, k -> new HashSet<>())
                         .add(username);
        
        // Send updated vote counts to all clients
        sendVoteUpdate(roomCode);
        
        // Check if all players have voted
        checkAllVotesSubmitted(roomCode);
    }
    
    // Check if all alive players have submitted actions
    private void checkAllActionsSubmitted(String roomCode) {
        List<Player> alivePlayers = rooms.get(roomCode).stream()
                                        .filter(Player::isAlive)
                                        .collect(Collectors.toList());
        
        Map<String, String> actions = playerActions.getOrDefault(roomCode, new HashMap<>());
        
        boolean allActionsSubmitted = true;
        for (Player player : alivePlayers) {
            // Only include players with actionable roles (MAFIA, DOCTOR, DETECTIVE)
            if (player.getRole().equals("MAFIA") || 
                player.getRole().equals("DOCTOR") || 
                player.getRole().equals("DETECTIVE")) {
                
                if (!actions.containsKey(player.getUsername())) {
                    allActionsSubmitted = false;
                    break;
                }
            }
        }
        
        if (allActionsSubmitted) {
            // Automatically end the phase if all players have acted
            togglePhase(roomCode);
        }
    }
    
    // Check if all alive players have voted
    private void checkAllVotesSubmitted(String roomCode) {
        List<Player> alivePlayers = rooms.get(roomCode).stream()
                                        .filter(Player::isAlive)
                                        .collect(Collectors.toList());
        
        Set<String> votedPlayers = playersDoneVoting.getOrDefault(roomCode, new HashSet<>());
        
        if (votedPlayers.size() >= alivePlayers.size()) {
            // Automatically end the phase if all alive players have voted
            togglePhase(roomCode);
        }
    }
    
    // Send vote updates to all clients
    private void sendVoteUpdate(String roomCode) {
        Map<String, Integer> voteCount = new HashMap<>();
        List<Player> players = rooms.get(roomCode);
        Map<String, Integer> currentVotes = votes.getOrDefault(roomCode, new HashMap<>());
        
        // Count votes for each player
        for (Integer targetId : currentVotes.values()) {
            for (Player player : players) {
                if (player.getId().equals(targetId)) {
                    String targetName = player.getUsername();
                    voteCount.put(targetName, voteCount.getOrDefault(targetName, 0) + 1);
                    break;
                }
            }
        }
        
        messagingTemplate.convertAndSend("/game/" + roomCode + "/votes", voteCount);
    }
    
    // Send player updates to all clients
    private void sendPlayerUpdates(String roomCode) {
        messagingTemplate.convertAndSend("/game/" + roomCode + "/players", rooms.get(roomCode));
    }
    
    // Get all players in a room
    public List<Player> getPlayers(String roomCode) {
        return rooms.getOrDefault(roomCode, new ArrayList<>());
    }
    
    // Get the current game phase for a room
    public String getGamePhase(String roomCode) {
        return gamePhases.getOrDefault(roomCode, "LOBBY");
    }
    
    // Generate a 6-character alphanumeric room code
    private String generateRoomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }
    
    // Start the timer for the current phase
    private void startPhaseTimer(String roomCode) {
        int seconds = phaseTimers.getOrDefault(roomCode, 60);
        
        // Send initial time to clients
        messagingTemplate.convertAndSend("/game/" + roomCode + "/timer", seconds);
        
        // Use a separate thread for the timer
        new Thread(() -> {
            while (seconds > 0 && 
                   gamePhases.containsKey(roomCode) && 
                   !checkGameEnd(roomCode)) {
                try {
                    Thread.sleep(1000);
                    seconds--;
                    
                    // Update clients every 5 seconds or in the last 10 seconds
                    if (seconds % 5 == 0 || seconds <= 10) {
                        messagingTemplate.convertAndSend("/game/" + roomCode + "/timer", seconds);
                    }
                    
                    if (seconds == 0) {
                        // Auto-toggle phase when time runs out
                        togglePhase(roomCode);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }
    
    // Get last investigated player for a detective
    public String getLastInvestigationResult(String roomCode, String username) {
        return lastInvestigated.get(roomCode + "_" + username);
    }
    
    // Get last saved player for a doctor
    public String getLastSavedPlayer(String roomCode, String username) {
        return lastSaved.get(roomCode + "_" + username);
    }
}

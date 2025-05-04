package com.victadore.webmafia.mafia_web_of_lies.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.victadore.webmafia.mafia_web_of_lies.model.Player;
import com.victadore.webmafia.mafia_web_of_lies.repository.PlayerRepository;
import com.victadore.webmafia.mafia_web_of_lies.service.GameService;

@Controller
public class GameController {

    @Autowired
    private GameService gameService;
    
    @Autowired
    private PlayerRepository playerRepository;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    // REST endpoint to create a new game room
    @PostMapping("/api/room/create")
    @ResponseBody
    public Map<String, String> createRoom() {
        String roomCode = gameService.createRoom();
        Map<String, String> response = new HashMap<>();
        response.put("roomCode", roomCode);
        return response;
    }
    
    // REST endpoint to join a game room
    @PostMapping("/api/room/join")
    @ResponseBody
    public Player joinRoom(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String roomCode = request.get("roomCode");
        
        Player player = gameService.joinRoom(username, roomCode);
        
        // Notify all clients in the room that a player has joined
        messagingTemplate.convertAndSend("/game/" + roomCode + "/players", 
                playerRepository.findByRoomCode(roomCode));
        
        return player;
    }
    
    // REST endpoint to start the game and assign roles
    @PostMapping("/api/room/{roomCode}/start")
    @ResponseBody
    public Map<String, String> startGame(@PathVariable String roomCode) {
        try {
            gameService.assignRoles(roomCode);
            
            // Notify all clients that the game has started
            messagingTemplate.convertAndSend("/game/" + roomCode + "/phase", "NIGHT");
            
            // Send private role information to each player
            List<Player> players = playerRepository.findByRoomCode(roomCode);
            for (Player player : players) {
                messagingTemplate.convertAndSendToUser(
                        player.getUsername(),
                        "/game/" + roomCode + "/role",
                        player.getRole());
            }
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("phase", "NIGHT");
            return response;
            
        } catch (IllegalStateException e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return response;
        }
    }
    
    // WebSocket endpoint for receiving player votes during day phase
    @MessageMapping("/room/{roomCode}/vote")
    @SendTo("/game/{roomCode}/votes")
    public Map<String, Object> handleVote(@DestinationVariable String roomCode, 
                                        Map<String, String> vote) {
        String voter = vote.get("from");
        String target = vote.get("target");
        
        // In a full implementation, track votes and execute majority decision
        
        Map<String, Object> result = new HashMap<>();
        result.put("voter", voter);
        result.put("target", target);
        return result;
    }
    
    // WebSocket endpoint for night actions (Mafia kill)
    @MessageMapping("/room/{roomCode}/night/kill")
    public void handleMafiaKill(@DestinationVariable String roomCode, 
                             Map<String, String> action) {
        String target = action.get("target");
        gameService.performKillAction(roomCode, target);
        
        // Check win conditions after action
        checkGameState(roomCode);
    }
    
    // WebSocket endpoint for night actions (Doctor save)
    @MessageMapping("/room/{roomCode}/night/save")
    public void handleDoctorSave(@DestinationVariable String roomCode, 
                              Map<String, String> action) {
        String target = action.get("target");
        gameService.performSaveAction(roomCode, target);
    }
    
    // WebSocket endpoint for night actions (Detective check)
    @MessageMapping("/room/{roomCode}/night/check")
    public void handleDetectiveCheck(@DestinationVariable String roomCode, 
                                  Map<String, String> action) {
        String from = action.get("from");
        String target = action.get("target");
        String result = gameService.performCheckAction(roomCode, target);
        
        // Send result only to the detective who requested it
        messagingTemplate.convertAndSendToUser(
                from,
                "/game/" + roomCode + "/check-result",
                result);
    }
    
    // REST endpoint to toggle between day and night phases
    @PostMapping("/api/room/{roomCode}/toggle-phase")
    @ResponseBody
    public Map<String, String> togglePhase(@PathVariable String roomCode) {
        String newPhase = gameService.toggleGamePhase(roomCode);
        
        // Notify all clients about the phase change
        messagingTemplate.convertAndSend("/game/" + roomCode + "/phase", newPhase);
        
        Map<String, String> response = new HashMap<>();
        response.put("phase", newPhase);
        return response;
    }
    
    // Utility method to check game state after actions
    private void checkGameState(String roomCode) {
        // Update player list for all clients
        List<Player> players = playerRepository.findByRoomCode(roomCode);
        messagingTemplate.convertAndSend("/game/" + roomCode + "/players", players);
        
        // Check win conditions
        String winner = gameService.checkWinCondition(roomCode);
        if (winner != null) {
            Map<String, String> gameOver = new HashMap<>();
            gameOver.put("winner", winner);
            messagingTemplate.convertAndSend("/game/" + roomCode + "/game-over", gameOver);
        }
    }
    
    // REST endpoint to get all players in a room
    @GetMapping("/api/room/{roomCode}/players")
    @ResponseBody
    public List<Player> getPlayers(@PathVariable String roomCode) {
        return playerRepository.findByRoomCode(roomCode);
    }
}

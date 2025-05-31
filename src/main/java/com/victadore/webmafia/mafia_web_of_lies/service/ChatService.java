package com.victadore.webmafia.mafia_web_of_lies.service;

import com.victadore.webmafia.mafia_web_of_lies.dto.ChatMessageRequest;
import com.victadore.webmafia.mafia_web_of_lies.dto.ChatMessageResponse;
import com.victadore.webmafia.mafia_web_of_lies.model.ChatMessage;
import com.victadore.webmafia.mafia_web_of_lies.model.Game;
import com.victadore.webmafia.mafia_web_of_lies.model.Player;
import com.victadore.webmafia.mafia_web_of_lies.repository.ChatMessageRepository;
import com.victadore.webmafia.mafia_web_of_lies.repository.GameRepository;
import com.victadore.webmafia.mafia_web_of_lies.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatService {
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired
    private GameRepository gameRepository;
    
    @Autowired
    private PlayerRepository playerRepository;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private ValidationService validationService;
    
    /**
     * Send a chat message (public or private)
     */
    @Transactional
    public ChatMessageResponse sendMessage(String gameCode, String senderUsername, ChatMessageRequest request) {
        // Validate game exists
        Game game = gameRepository.findByGameCode(gameCode);
        if (game == null) {
            throw new IllegalArgumentException("Game not found");
        }
        
        // Validate player exists and is in the game
        Player sender = playerRepository.findByUsernameAndGameId(senderUsername, game.getId());
        if (sender == null) {
            throw new IllegalArgumentException("Player not found in game");
        }
        
        // Validate player is alive (dead players cannot chat)
        if (!sender.isAlive()) {
            throw new IllegalArgumentException("Dead players cannot send chat messages");
        }
        
        // Validate message content
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }
        
        if (request.getMessage().length() > 1000) {
            throw new IllegalArgumentException("Message is too long (max 1000 characters)");
        }
        
        // Validate chat permissions based on phase and role
        validateChatPermissions(game, sender, request);
        
        // Create and save chat message
        ChatMessage chatMessage = new ChatMessage(
            gameCode,
            senderUsername,
            request.getMessage().trim(),
            game.getCurrentDay(),
            game.getCurrentPhase(),
            request.getChatType(),
            request.getTargetRole()
        );
        
        chatMessage = chatMessageRepository.save(chatMessage);
        
        // Create response
        ChatMessageResponse response = new ChatMessageResponse(chatMessage, senderUsername);
        
        // Send via WebSocket to appropriate recipients
        broadcastMessage(gameCode, response);
        
        return response;
    }
    
    /**
     * Get chat messages for a player (based on their role and current phase)
     */
    public List<ChatMessageResponse> getMessagesForPlayer(String gameCode, String username) {
        // Get game and player info
        Game game = gameRepository.findByGameCode(gameCode);
        if (game == null) {
            throw new IllegalArgumentException("Game not found");
        }
        
        Player player = playerRepository.findByUsernameAndGameId(username, game.getId());
        if (player == null) {
            throw new IllegalArgumentException("Player not found in game");
        }
        
        List<ChatMessage> messages;
        
        if (game.getCurrentPhase() == 0) { // DAY phase
            // Show all public messages from current day
            messages = chatMessageRepository.findMessagesByGameDayPhaseAndType(
                gameCode, game.getCurrentDay(), 0, ChatMessage.ChatType.PUBLIC);
        } else { // NIGHT phase
            if ("MAFIA".equals(player.getRole().name())) {
                // Mafia can see their private chat
                messages = chatMessageRepository.findPrivateMessagesByGameDayPhaseAndRole(
                    gameCode, game.getCurrentDay(), 1, "MAFIA");
            } else {
                // Non-Mafia roles cannot chat during night
                messages = List.of();
            }
        }
        
        return messages.stream()
            .map(msg -> new ChatMessageResponse(msg, username))
            .collect(Collectors.toList());
    }
    
    /**
     * Get all public messages for a game (for admin/spectator view)
     */
    public List<ChatMessageResponse> getPublicMessages(String gameCode, String username) {
        List<ChatMessage> messages = chatMessageRepository.findPublicMessagesByGameCode(gameCode);
        return messages.stream()
            .map(msg -> new ChatMessageResponse(msg, username))
            .collect(Collectors.toList());
    }
    
    /**
     * Validate chat permissions based on game phase and player role
     */
    private void validateChatPermissions(Game game, Player player, ChatMessageRequest request) {
        if (game.getCurrentPhase() == 0) { // DAY phase
            // Only public chat allowed during day
            if (request.getChatType() != ChatMessage.ChatType.PUBLIC) {
                throw new IllegalArgumentException("Only public chat is allowed during day phase");
            }
        } else { // NIGHT phase
            // Only Mafia can chat during night, and only privately
            if (!"MAFIA".equals(player.getRole().name())) {
                throw new IllegalArgumentException("Only Mafia members can chat during night phase");
            }
            
            if (request.getChatType() != ChatMessage.ChatType.PRIVATE || 
                !"MAFIA".equals(request.getTargetRole())) {
                throw new IllegalArgumentException("Mafia can only use private chat during night phase");
            }
        }
    }
    
    /**
     * Broadcast message via WebSocket to appropriate recipients
     */
    private void broadcastMessage(String gameCode, ChatMessageResponse message) {
        if (message.getChatType() == ChatMessage.ChatType.PUBLIC) {
            // Send to all players in the game
            messagingTemplate.convertAndSend("/topic/game/" + gameCode + "/chat", message);
        } else if (message.getChatType() == ChatMessage.ChatType.PRIVATE) {
            // Send only to players with the target role
            messagingTemplate.convertAndSend("/topic/game/" + gameCode + "/chat/" + message.getTargetRole(), message);
        }
    }
    
    /**
     * Send a system message (for game events)
     */
    @Transactional
    public void sendSystemMessage(String gameCode, String message) {
        Game game = gameRepository.findByGameCode(gameCode);
        if (game == null) {
            throw new IllegalArgumentException("Game not found");
        }
        
        ChatMessage chatMessage = new ChatMessage(
            gameCode,
            "SYSTEM",
            message,
            game.getCurrentDay(),
            game.getCurrentPhase(),
            ChatMessage.ChatType.SYSTEM
        );
        
        chatMessage = chatMessageRepository.save(chatMessage);
        
        ChatMessageResponse response = new ChatMessageResponse(chatMessage, "");
        
        // Send system message to all players
        messagingTemplate.convertAndSend("/topic/game/" + gameCode + "/chat", response);
    }
    
    /**
     * Clear all chat messages for a game (when game ends or for cleanup)
     */
    @Transactional
    public void clearGameChat(String gameCode) {
        chatMessageRepository.deleteByGameCode(gameCode);
    }
} 
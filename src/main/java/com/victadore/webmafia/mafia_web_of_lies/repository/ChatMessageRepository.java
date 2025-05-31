package com.victadore.webmafia.mafia_web_of_lies.repository;

import com.victadore.webmafia.mafia_web_of_lies.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    /**
     * Find all public chat messages for a game (day phase messages)
     */
    @Query("SELECT c FROM ChatMessage c WHERE c.gameCode = :gameCode AND c.chatType = 'PUBLIC' ORDER BY c.timestamp ASC")
    List<ChatMessage> findPublicMessagesByGameCode(@Param("gameCode") String gameCode);
    
    /**
     * Find private chat messages for a specific role in a game (e.g., Mafia night chat)
     */
    @Query("SELECT c FROM ChatMessage c WHERE c.gameCode = :gameCode AND c.chatType = 'PRIVATE' AND c.targetRole = :role ORDER BY c.timestamp ASC")
    List<ChatMessage> findPrivateMessagesByGameCodeAndRole(@Param("gameCode") String gameCode, @Param("role") String role);
    
    /**
     * Find all messages for a game (for admin/debugging purposes)
     */
    @Query("SELECT c FROM ChatMessage c WHERE c.gameCode = :gameCode ORDER BY c.timestamp ASC")
    List<ChatMessage> findAllMessagesByGameCode(@Param("gameCode") String gameCode);
    
    /**
     * Find messages for current day and phase
     */
    @Query("SELECT c FROM ChatMessage c WHERE c.gameCode = :gameCode AND c.day = :day AND c.phase = :phase AND c.chatType = :chatType ORDER BY c.timestamp ASC")
    List<ChatMessage> findMessagesByGameDayPhaseAndType(@Param("gameCode") String gameCode, 
                                                       @Param("day") Integer day, 
                                                       @Param("phase") Integer phase, 
                                                       @Param("chatType") ChatMessage.ChatType chatType);
    
    /**
     * Find private messages for current day, phase and role
     */
    @Query("SELECT c FROM ChatMessage c WHERE c.gameCode = :gameCode AND c.day = :day AND c.phase = :phase AND c.chatType = 'PRIVATE' AND c.targetRole = :role ORDER BY c.timestamp ASC")
    List<ChatMessage> findPrivateMessagesByGameDayPhaseAndRole(@Param("gameCode") String gameCode, 
                                                              @Param("day") Integer day, 
                                                              @Param("phase") Integer phase, 
                                                              @Param("role") String role);
    
    /**
     * Count messages by game code (for statistics)
     */
    @Query("SELECT COUNT(c) FROM ChatMessage c WHERE c.gameCode = :gameCode")
    Long countMessagesByGameCode(@Param("gameCode") String gameCode);
    
    /**
     * Delete all messages for a game (cleanup when game ends)
     */
    void deleteByGameCode(String gameCode);
} 
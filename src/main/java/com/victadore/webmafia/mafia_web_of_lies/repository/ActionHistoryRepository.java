package com.victadore.webmafia.mafia_web_of_lies.repository;

import com.victadore.webmafia.mafia_web_of_lies.model.ActionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActionHistoryRepository extends JpaRepository<ActionHistory, Long> {
    
    /**
     * Find all actions for a specific game, ordered by timestamp
     */
    List<ActionHistory> findByGameIdOrderByTimestamp(Long gameId);
    
    /**
     * Find all actions by a specific actor in a game
     */
    List<ActionHistory> findByGameIdAndActorUsernameOrderByTimestamp(Long gameId, String actorUsername);
    
    /**
     * Find all actions of a specific type in a game
     */
    List<ActionHistory> findByGameIdAndActionTypeOrderByTimestamp(Long gameId, String actionType);
    
    /**
     * Find all actions in a specific game day and phase
     */
    List<ActionHistory> findByGameIdAndGameDayAndGamePhaseOrderByTimestamp(Long gameId, Integer gameDay, Integer gamePhase);
    
    /**
     * Find all actions targeting a specific player
     */
    List<ActionHistory> findByGameIdAndTargetUsernameOrderByTimestamp(Long gameId, String targetUsername);
    
    /**
     * Find actions within a time range
     */
    List<ActionHistory> findByGameIdAndTimestampBetweenOrderByTimestamp(Long gameId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Count total actions in a game
     */
    long countByGameId(Long gameId);
    
    /**
     * Count actions by a specific actor
     */
    long countByGameIdAndActorUsername(Long gameId, String actorUsername);
    
    /**
     * Count successful actions
     */
    long countByGameIdAndSuccessful(Long gameId, Boolean successful);
    
    /**
     * Find the most recent action in a game
     */
    ActionHistory findFirstByGameIdOrderByTimestampDesc(Long gameId);
    
    /**
     * Find all vote actions in a specific day phase
     */
    @Query("SELECT ah FROM ActionHistory ah WHERE ah.game.id = :gameId " +
           "AND ah.actionType IN ('VOTE', 'SKIP_VOTE') " +
           "AND ah.gameDay = :gameDay AND ah.gamePhase = 0 " +
           "ORDER BY ah.timestamp")
    List<ActionHistory> findVoteActionsInDay(@Param("gameId") Long gameId, @Param("gameDay") Integer gameDay);
    
    /**
     * Find all night actions in a specific night phase
     */
    @Query("SELECT ah FROM ActionHistory ah WHERE ah.game.id = :gameId " +
           "AND ah.actionType IN ('KILL', 'SAVE', 'INVESTIGATE') " +
           "AND ah.gameDay = :gameDay AND ah.gamePhase = 1 " +
           "ORDER BY ah.timestamp")
    List<ActionHistory> findNightActionsInNight(@Param("gameId") Long gameId, @Param("gameDay") Integer gameDay);
    
    /**
     * Get action statistics for a game
     */
    @Query("SELECT ah.actionType, COUNT(ah) FROM ActionHistory ah " +
           "WHERE ah.game.id = :gameId GROUP BY ah.actionType")
    List<Object[]> getActionStatistics(@Param("gameId") Long gameId);
    
    /**
     * Find actions that resulted in player elimination
     */
    @Query("SELECT ah FROM ActionHistory ah WHERE ah.game.id = :gameId " +
           "AND ah.actionType IN ('ELIMINATE', 'KILL') " +
           "AND ah.successful = true " +
           "ORDER BY ah.timestamp")
    List<ActionHistory> findEliminationActions(@Param("gameId") Long gameId);
    
    /**
     * Find all actions performed by alive players only
     */
    @Query("SELECT ah FROM ActionHistory ah WHERE ah.game.id = :gameId " +
           "AND ah.actorUsername IN (SELECT p.username FROM Player p WHERE p.game.id = :gameId AND p.isAlive = true) " +
           "ORDER BY ah.timestamp")
    List<ActionHistory> findActionsByAlivePlayers(@Param("gameId") Long gameId);
} 
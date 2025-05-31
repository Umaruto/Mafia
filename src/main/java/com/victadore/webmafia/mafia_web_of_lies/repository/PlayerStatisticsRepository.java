package com.victadore.webmafia.mafia_web_of_lies.repository;

import com.victadore.webmafia.mafia_web_of_lies.model.PlayerStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerStatisticsRepository extends JpaRepository<PlayerStatistics, Long> {
    
    /**
     * Find player statistics by username
     */
    Optional<PlayerStatistics> findByUsername(String username);
    
    /**
     * Check if player statistics exist for a username
     */
    boolean existsByUsername(String username);
    
    /**
     * Find top players by win rate (minimum games required)
     */
    @Query("SELECT ps FROM PlayerStatistics ps WHERE ps.totalGames >= :minGames " +
           "ORDER BY (ps.gamesWon * 1.0 / ps.totalGames) DESC")
    List<PlayerStatistics> findTopPlayersByWinRate(@Param("minGames") int minGames);
    
    /**
     * Find top players by total games played
     */
    @Query("SELECT ps FROM PlayerStatistics ps ORDER BY ps.totalGames DESC")
    List<PlayerStatistics> findTopPlayersByGamesPlayed();
    
    /**
     * Find top players by survival rate
     */
    @Query("SELECT ps FROM PlayerStatistics ps WHERE ps.totalGames >= :minGames " +
           "ORDER BY (ps.timesSurvived * 1.0 / ps.totalGames) DESC")
    List<PlayerStatistics> findTopPlayersBySurvivalRate(@Param("minGames") int minGames);
    
    /**
     * Find top players by voting accuracy
     */
    @Query("SELECT ps FROM PlayerStatistics ps WHERE ps.totalVotesCast >= :minVotes " +
           "ORDER BY (ps.correctMafiaVotes * 1.0 / ps.totalVotesCast) DESC")
    List<PlayerStatistics> findTopPlayersByVotingAccuracy(@Param("minVotes") int minVotes);
    
    /**
     * Find top Mafia players by win rate
     */
    @Query("SELECT ps FROM PlayerStatistics ps WHERE ps.mafiaGames >= :minGames " +
           "ORDER BY (ps.mafiaWins * 1.0 / ps.mafiaGames) DESC")
    List<PlayerStatistics> findTopMafiaPlayers(@Param("minGames") int minGames);
    
    /**
     * Find top Detective players by success rate
     */
    @Query("SELECT ps FROM PlayerStatistics ps WHERE ps.investigationsPerformed >= :minInvestigations " +
           "ORDER BY (ps.mafiaFound * 1.0 / ps.investigationsPerformed) DESC")
    List<PlayerStatistics> findTopDetectivePlayers(@Param("minInvestigations") int minInvestigations);
    
    /**
     * Find top Doctor players by save success rate
     */
    @Query("SELECT ps FROM PlayerStatistics ps WHERE ps.savesAttempted >= :minSaves " +
           "ORDER BY (ps.successfulSaves * 1.0 / ps.savesAttempted) DESC")
    List<PlayerStatistics> findTopDoctorPlayers(@Param("minSaves") int minSaves);
    
    /**
     * Find players with most MVP awards
     */
    @Query("SELECT ps FROM PlayerStatistics ps ORDER BY ps.mvpAwards DESC")
    List<PlayerStatistics> findTopMVPPlayers();
    
    /**
     * Find players with most perfect games
     */
    @Query("SELECT ps FROM PlayerStatistics ps ORDER BY ps.perfectGames DESC")
    List<PlayerStatistics> findTopPerfectGamePlayers();
    
    /**
     * Find recently active players
     */
    @Query("SELECT ps FROM PlayerStatistics ps WHERE ps.lastGameDate >= :since ORDER BY ps.lastGameDate DESC")
    List<PlayerStatistics> findRecentlyActivePlayers(@Param("since") LocalDateTime since);
    
    /**
     * Find players by experience level
     */
    @Query("SELECT ps FROM PlayerStatistics ps WHERE ps.totalGames >= :minGames AND ps.totalGames < :maxGames")
    List<PlayerStatistics> findPlayersByExperienceRange(@Param("minGames") int minGames, @Param("maxGames") int maxGames);
    
    /**
     * Get global statistics
     */
    @Query("SELECT " +
           "COUNT(ps), " +
           "COALESCE(SUM(ps.totalGames), 0), " +
           "COALESCE(AVG(CASE WHEN ps.totalGames > 0 THEN ps.gamesWon * 1.0 / ps.totalGames ELSE 0 END), 0), " +
           "COALESCE(AVG(CASE WHEN ps.totalGames > 0 THEN ps.timesSurvived * 1.0 / ps.totalGames ELSE 0 END), 0), " +
           "COALESCE(MAX(ps.totalGames), 0), " +
           "COALESCE(MAX(ps.gamesWon), 0) " +
           "FROM PlayerStatistics ps")
    Object[] getGlobalStatistics();
    
    /**
     * Count players by experience level
     */
    @Query("SELECT " +
           "SUM(CASE WHEN ps.totalGames < 5 THEN 1 ELSE 0 END) as newcomers, " +
           "SUM(CASE WHEN ps.totalGames >= 5 AND ps.totalGames < 15 THEN 1 ELSE 0 END) as beginners, " +
           "SUM(CASE WHEN ps.totalGames >= 15 AND ps.totalGames < 30 THEN 1 ELSE 0 END) as intermediate, " +
           "SUM(CASE WHEN ps.totalGames >= 30 AND ps.totalGames < 50 THEN 1 ELSE 0 END) as advanced, " +
           "SUM(CASE WHEN ps.totalGames >= 50 AND ps.totalGames < 100 THEN 1 ELSE 0 END) as expert, " +
           "SUM(CASE WHEN ps.totalGames >= 100 THEN 1 ELSE 0 END) as master " +
           "FROM PlayerStatistics ps")
    Object[] getExperienceLevelDistribution();
    
    /**
     * Find players with specific achievements
     */
    @Query("SELECT ps FROM PlayerStatistics ps WHERE " +
           "(ps.perfectGames > 0 OR ps.mvpAwards > 0) " +
           "ORDER BY (ps.perfectGames + ps.mvpAwards) DESC")
    List<PlayerStatistics> findPlayersWithAchievements();
    
    /**
     * Search players by username pattern
     */
    @Query("SELECT ps FROM PlayerStatistics ps WHERE ps.username LIKE %:pattern% ORDER BY ps.totalGames DESC")
    List<PlayerStatistics> searchPlayersByUsername(@Param("pattern") String pattern);
    
    /**
     * Find players who haven't played recently
     */
    @Query("SELECT ps FROM PlayerStatistics ps WHERE ps.lastGameDate < :cutoffDate ORDER BY ps.lastGameDate ASC")
    List<PlayerStatistics> findInactivePlayers(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Get role preference statistics
     */
    @Query("SELECT " +
           "SUM(ps.mafiaGames) as totalMafiaGames, " +
           "SUM(ps.detectiveGames) as totalDetectiveGames, " +
           "SUM(ps.doctorGames) as totalDoctorGames, " +
           "SUM(ps.citizenGames) as totalCitizenGames " +
           "FROM PlayerStatistics ps")
    Object[] getRoleDistributionStatistics();
} 
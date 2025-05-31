package com.victadore.webmafia.mafia_web_of_lies.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "player_statistics", indexes = {
    @Index(name = "idx_player_stats_username", columnList = "username"),
    @Index(name = "idx_player_stats_updated", columnList = "last_updated")
})
public class PlayerStatistics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Username is required")
    @Size(min = 2, max = 20, message = "Username must be between 2 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_\\-]+$", message = "Username can only contain letters, numbers, underscores, and hyphens")
    @Column(nullable = false, unique = true, length = 20)
    private String username;
    
    // Game Statistics
    @NotNull(message = "Total games is required")
    @Column(nullable = false)
    private Integer totalGames = 0;
    
    @NotNull(message = "Games won is required")
    @Column(nullable = false)
    private Integer gamesWon = 0;
    
    @NotNull(message = "Games lost is required")
    @Column(nullable = false)
    private Integer gamesLost = 0;
    
    // Role-specific Statistics
    @NotNull(message = "Mafia games is required")
    @Column(nullable = false)
    private Integer mafiaGames = 0;
    
    @NotNull(message = "Mafia wins is required")
    @Column(nullable = false)
    private Integer mafiaWins = 0;
    
    @NotNull(message = "Detective games is required")
    @Column(nullable = false)
    private Integer detectiveGames = 0;
    
    @NotNull(message = "Detective wins is required")
    @Column(nullable = false)
    private Integer detectiveWins = 0;
    
    @NotNull(message = "Doctor games is required")
    @Column(nullable = false)
    private Integer doctorGames = 0;
    
    @NotNull(message = "Doctor wins is required")
    @Column(nullable = false)
    private Integer doctorWins = 0;
    
    @NotNull(message = "Citizen games is required")
    @Column(nullable = false)
    private Integer citizenGames = 0;
    
    @NotNull(message = "Citizen wins is required")
    @Column(nullable = false)
    private Integer citizenWins = 0;
    
    // Survival Statistics
    @NotNull(message = "Times survived is required")
    @Column(nullable = false)
    private Integer timesSurvived = 0;
    
    @NotNull(message = "Times eliminated is required")
    @Column(nullable = false)
    private Integer timesEliminated = 0;
    
    @NotNull(message = "Times eliminated by voting is required")
    @Column(nullable = false)
    private Integer timesEliminatedByVoting = 0;
    
    @NotNull(message = "Times eliminated by Mafia is required")
    @Column(nullable = false)
    private Integer timesEliminatedByMafia = 0;
    
    // Action Statistics
    @NotNull(message = "Total votes cast is required")
    @Column(nullable = false)
    private Integer totalVotesCast = 0;
    
    @NotNull(message = "Correct Mafia votes is required")
    @Column(nullable = false)
    private Integer correctMafiaVotes = 0;
    
    @NotNull(message = "Skip votes is required")
    @Column(nullable = false)
    private Integer skipVotes = 0;
    
    @NotNull(message = "Night actions performed is required")
    @Column(nullable = false)
    private Integer nightActionsPerformed = 0;
    
    @NotNull(message = "Successful night actions is required")
    @Column(nullable = false)
    private Integer successfulNightActions = 0;
    
    // Detective-specific Statistics
    @NotNull(message = "Investigations performed is required")
    @Column(nullable = false)
    private Integer investigationsPerformed = 0;
    
    @NotNull(message = "Mafia found is required")
    @Column(nullable = false)
    private Integer mafiaFound = 0;
    
    // Doctor-specific Statistics
    @NotNull(message = "Saves attempted is required")
    @Column(nullable = false)
    private Integer savesAttempted = 0;
    
    @NotNull(message = "Successful saves is required")
    @Column(nullable = false)
    private Integer successfulSaves = 0;
    
    // Mafia-specific Statistics
    @NotNull(message = "Mafia kills attempted is required")
    @Column(nullable = false)
    private Integer mafiaKillsAttempted = 0;
    
    @NotNull(message = "Successful Mafia kills is required")
    @Column(nullable = false)
    private Integer successfulMafiaKills = 0;
    
    // Achievement Counters
    @NotNull(message = "Perfect games is required")
    @Column(nullable = false)
    private Integer perfectGames = 0; // Games where player performed optimally
    
    @NotNull(message = "MVP awards is required")
    @Column(nullable = false)
    private Integer mvpAwards = 0; // Most Valuable Player awards
    
    // Timestamps
    @NotNull(message = "First game date is required")
    @Column(nullable = false)
    private LocalDateTime firstGameDate;
    
    @NotNull(message = "Last game date is required")
    @Column(nullable = false)
    private LocalDateTime lastGameDate;
    
    @NotNull(message = "Last updated is required")
    @Column(nullable = false)
    private LocalDateTime lastUpdated;
    
    // Calculated Properties (not stored in DB)
    @Transient
    public Double getWinRate() {
        return totalGames > 0 ? (double) gamesWon / totalGames * 100 : 0.0;
    }
    
    @Transient
    public Double getSurvivalRate() {
        return totalGames > 0 ? (double) timesSurvived / totalGames * 100 : 0.0;
    }
    
    @Transient
    public Double getVotingAccuracy() {
        return totalVotesCast > 0 ? (double) correctMafiaVotes / totalVotesCast * 100 : 0.0;
    }
    
    @Transient
    public Double getMafiaWinRate() {
        return mafiaGames > 0 ? (double) mafiaWins / mafiaGames * 100 : 0.0;
    }
    
    @Transient
    public Double getDetectiveWinRate() {
        return detectiveGames > 0 ? (double) detectiveWins / detectiveGames * 100 : 0.0;
    }
    
    @Transient
    public Double getDoctorWinRate() {
        return doctorGames > 0 ? (double) doctorWins / doctorGames * 100 : 0.0;
    }
    
    @Transient
    public Double getCitizenWinRate() {
        return citizenGames > 0 ? (double) citizenWins / citizenGames * 100 : 0.0;
    }
    
    @Transient
    public Double getDetectiveSuccessRate() {
        return investigationsPerformed > 0 ? (double) mafiaFound / investigationsPerformed * 100 : 0.0;
    }
    
    @Transient
    public Double getDoctorSuccessRate() {
        return savesAttempted > 0 ? (double) successfulSaves / savesAttempted * 100 : 0.0;
    }
    
    @Transient
    public Double getMafiaKillSuccessRate() {
        return mafiaKillsAttempted > 0 ? (double) successfulMafiaKills / mafiaKillsAttempted * 100 : 0.0;
    }
    
    @Transient
    public String getExperienceLevel() {
        if (totalGames < 5) return "Newcomer";
        if (totalGames < 15) return "Beginner";
        if (totalGames < 30) return "Intermediate";
        if (totalGames < 50) return "Advanced";
        if (totalGames < 100) return "Expert";
        return "Master";
    }
    
    @Transient
    public String getFavoriteRole() {
        int maxGames = Math.max(Math.max(mafiaGames, detectiveGames), 
                               Math.max(doctorGames, citizenGames));
        
        if (maxGames == 0) return "None";
        if (maxGames == mafiaGames) return "Mafia";
        if (maxGames == detectiveGames) return "Detective";
        if (maxGames == doctorGames) return "Doctor";
        return "Citizen";
    }
    
    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.lastUpdated = LocalDateTime.now();
        if (this.firstGameDate == null) {
            this.firstGameDate = LocalDateTime.now();
        }
    }
} 
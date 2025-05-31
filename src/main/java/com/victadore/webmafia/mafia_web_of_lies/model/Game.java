package com.victadore.webmafia.mafia_web_of_lies.model;

import java.util.List;
import java.util.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;

@Entity
@Data
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Game code is required")
    @Size(min = 6, max = 6, message = "Game code must be exactly 6 characters")
    @Pattern(regexp = "^[A-Z0-9]{6}$", message = "Game code must contain only uppercase letters and numbers")
    @Column(nullable = false, unique = true, length = 6)
    private String gameCode;

    @NotNull(message = "Active status is required")
    @Column(nullable = false)
    private boolean isActive;

    @NotNull(message = "Game state is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameState gameState;
    
    @NotNull(message = "Players list is required")
    @Size(min = 0, max = 15, message = "Game can have at most 15 players")
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL)
    private List<Player> players;

    @Min(value = 0, message = "Current day cannot be negative")
    @Max(value = 100, message = "Current day cannot exceed 100")
    @Column(nullable = false)
    private int currentDay;

    @Min(value = 0, message = "Current phase must be 0 (day) or 1 (night)")
    @Max(value = 1, message = "Current phase must be 0 (day) or 1 (night)")
    @Column(nullable = false)
    private int currentPhase;

    @ElementCollection
    private Map<Long, Integer> votes = new HashMap<>();

    @ElementCollection
    private Set<Long> playersWhoVoted = new HashSet<>();

    @ElementCollection
    private Set<Long> playersWhoActedAtNight = new HashSet<>();

    // Individual vote tracking: voter ID -> target ID (null for skip)
    @ElementCollection
    private Map<Long, Long> individualVotes = new HashMap<>();

    // Mafia vote tracking: mafia member ID -> target player ID
    @ElementCollection
    private Map<Long, Long> mafiaVotes = new HashMap<>();

    // Night action targets
    private Long mafiaTarget;  // The player targeted by Mafia for elimination
    private Long doctorTarget; // The player targeted by Doctor for saving

    @Min(value = 4, message = "Minimum players must be at least 4")
    @Max(value = 15, message = "Minimum players cannot exceed 15")
    @Column(nullable = false)
    private int minPlayers;   // Minimum number of players required to start
    
    @Min(value = 4, message = "Maximum players must be at least 4")
    @Max(value = 15, message = "Maximum players cannot exceed 15")
    @Column(nullable = false)
    private int maxPlayers;   // Maximum number of players allowed
    
    @NotBlank(message = "Creator username is required")
    @Size(min = 2, max = 20, message = "Creator username must be between 2 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_\\-]+$", message = "Creator username can only contain letters, numbers, underscores, and hyphens")
    @Column(nullable = false, length = 20)
    private String createdBy; // Username of the player who created the game
    
    @Size(max = 20, message = "Winner name cannot exceed 20 characters")
    private String winner;
    
    // Custom validation method
    @AssertTrue(message = "Maximum players must be greater than or equal to minimum players")
    private boolean isMaxPlayersValid() {
        return maxPlayers >= minPlayers;
    }
}

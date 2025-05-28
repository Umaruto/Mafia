package com.victadore.webmafia.mafia_web_of_lies.model;

import java.util.List;
import java.util.*;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String gameCode;

    private boolean isActive;

    @Enumerated(EnumType.STRING)
    private GameState gameState;
    
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL)
    private List<Player> players;

    private int currentDay;

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

    private int minPlayers;   // Minimum number of players required to start
    private int maxPlayers;   // Maximum number of players allowed
    private String createdBy; // Username of the player who created the game
    private String winner; 
}

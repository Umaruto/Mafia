package com.victadore.webmafia.mafia_web_of_lies.model;

import java.util.List;

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

    private int minPlayers;   // Minimum number of players required to start
    private int maxPlayers;   // Maximum number of players allowed
    private String createdBy; // Username of the player who created the game
    private String winner; 
}

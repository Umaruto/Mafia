package com.victadore.webmafia.mafia_web_of_lies.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"username", "game_id"}, name = "uk_player_username_game")
})
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private boolean isAlive = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean isInvestigated = false;

    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;
}

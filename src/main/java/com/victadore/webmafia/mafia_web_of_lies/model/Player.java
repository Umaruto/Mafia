package com.victadore.webmafia.mafia_web_of_lies.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "Username is required")
    @Size(min = 2, max = 20, message = "Username must be between 2 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_\\-]+$", message = "Username can only contain letters, numbers, underscores, and hyphens")
    @Column(nullable = false, length = 20)
    private String username;

    @NotNull(message = "Alive status is required")
    @Column(nullable = false)
    private boolean isAlive = true;

    @NotNull(message = "Role is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @NotNull(message = "Investigation status is required")
    @Column(nullable = false)
    private boolean isInvestigated = false;

    @NotNull(message = "Game association is required")
    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;
}

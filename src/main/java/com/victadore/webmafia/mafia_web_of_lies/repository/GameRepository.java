package com.victadore.webmafia.mafia_web_of_lies.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.victadore.webmafia.mafia_web_of_lies.model.Game;
import com.victadore.webmafia.mafia_web_of_lies.model.GameState;
import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    
    Game findByGameCode(String gameCode);

    boolean existsByGameCode(String gameCode);

    List<Game> findByIsActiveTrue();

    List<Game> findByGameState(GameState state);

    List<Game> findByCreatedBy(String createdBy);
}

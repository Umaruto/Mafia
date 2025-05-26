package com.victadore.webmafia.mafia_web_of_lies.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import com.victadore.webmafia.mafia_web_of_lies.model.Player;
import com.victadore.webmafia.mafia_web_of_lies.model.Role;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    
    List<Player> findByGameId(Long gameId);

    Player findByUsernameAndGameId(String username, Long gameId);

    List<Player> findByGameIdAndIsAliveTrue(Long gameId);

    List<Player> findByGameIdAndRole(Long gameId, Role role);

    long countByGameId(Long gameId);

    long countByGameIdAndIsAliveTrue(Long gameId);
    
}


package com.victadore.webmafia.mafia_web_of_lies.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.victadore.webmafia.mafia_web_of_lies.model.Player;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    // Find all players in a specific game room
    List<Player> findByRoomCode(String roomCode);
    
    // Find a specific player by username and room code
    Player findByUsernameAndRoomCode(String username, String roomCode);
    
    // Find all living players in a room
    List<Player> findByRoomCodeAndAliveTrue(String roomCode);
    
    // Find players by role in a specific room
    List<Player> findByRoomCodeAndRole(String roomCode, String role);
}

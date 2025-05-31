-- Manual statistics update for existing games
-- This script will create basic statistics for players who have played games

-- First, let's create some sample statistics for the players in the finished games
-- Based on the games we found: FFA3DD (CITIZENS won), 019CB6 (MAFIA won), 46D708 (MAFIA won)

-- Insert basic statistics for Umar (the game creator)
INSERT INTO player_statistics (username, total_games, games_won, games_lost, times_survived, times_eliminated, 
                              mafia_games, detective_games, doctor_games, citizen_games,
                              mafia_wins, detective_wins, doctor_wins, citizen_wins,
                              first_game_date, last_game_date, last_updated)
VALUES ('Umar', 3, 1, 2, 2, 1, 1, 1, 1, 0, 0, 0, 1, 0, 
        '2025-05-30 15:49:06', '2025-05-31 17:10:37', NOW())
ON DUPLICATE KEY UPDATE
    total_games = 3,
    games_won = 1,
    games_lost = 2,
    times_survived = 2,
    times_eliminated = 1,
    last_game_date = '2025-05-31 17:10:37',
    last_updated = NOW();

-- Insert statistics for other common test usernames
INSERT INTO player_statistics (username, total_games, games_won, games_lost, times_survived, times_eliminated,
                              mafia_games, detective_games, doctor_games, citizen_games,
                              mafia_wins, detective_wins, doctor_wins, citizen_wins,
                              first_game_date, last_game_date, last_updated)
VALUES 
    ('Isko', 3, 2, 1, 1, 2, 1, 0, 1, 1, 1, 0, 0, 1, '2025-05-30 15:49:06', '2025-05-31 17:10:37', NOW()),
    ('Alice', 2, 1, 1, 1, 1, 0, 1, 0, 1, 0, 1, 0, 0, '2025-05-30 15:49:06', '2025-05-31 16:37:03', NOW()),
    ('Bob', 2, 1, 1, 2, 0, 1, 0, 0, 1, 1, 0, 0, 0, '2025-05-30 15:49:06', '2025-05-31 16:37:03', NOW()),
    ('Charlie', 1, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, '2025-05-31 16:34:58', '2025-05-31 16:37:03', NOW()),
    ('Diana', 1, 1, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, '2025-05-31 16:51:23', '2025-05-31 17:10:37', NOW())
ON DUPLICATE KEY UPDATE
    total_games = VALUES(total_games),
    games_won = VALUES(games_won),
    games_lost = VALUES(games_lost),
    times_survived = VALUES(times_survived),
    times_eliminated = VALUES(times_eliminated),
    mafia_games = VALUES(mafia_games),
    detective_games = VALUES(detective_games),
    doctor_games = VALUES(doctor_games),
    citizen_games = VALUES(citizen_games),
    mafia_wins = VALUES(mafia_wins),
    detective_wins = VALUES(detective_wins),
    doctor_wins = VALUES(doctor_wins),
    citizen_wins = VALUES(citizen_wins),
    last_game_date = VALUES(last_game_date),
    last_updated = NOW(); 
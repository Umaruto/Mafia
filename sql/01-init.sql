-- Initialize Mafia Web of Lies Database
-- This script runs automatically when the MySQL container starts

USE mafiadb;

-- Set timezone
SET time_zone = '+00:00';

-- Create indexes for better performance on frequently queried columns
-- These will be created by Hibernate, but we can optimize them

-- Example queries that might benefit from indexes:
-- SELECT * FROM games WHERE game_code = ?
-- SELECT * FROM players WHERE game_id = ? AND username = ?
-- SELECT * FROM chat_messages WHERE game_code = ? AND day = ? AND phase = ?
-- SELECT * FROM votes WHERE game_id = ? AND day = ?

-- The tables will be created automatically by Hibernate with ddl-auto=update
-- But we can add some initial configuration here if needed

-- Set default charset for better emoji support in chat
ALTER DATABASE mafiadb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; 
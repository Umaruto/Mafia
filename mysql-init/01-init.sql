-- Initialize Mafia Game Database
CREATE DATABASE IF NOT EXISTS mafiadb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE mafiadb;

-- Grant privileges to mafia_user
GRANT ALL PRIVILEGES ON mafiadb.* TO 'mafia_user'@'%';
FLUSH PRIVILEGES;

-- Set timezone
SET time_zone = '+00:00'; 
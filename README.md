# Mafia Web of Lies

A multiplayer online Mafia game built with Spring Boot, featuring real-time gameplay, chat system, player statistics, and game history.

## Features

- **Real-time Multiplayer Gameplay**: WebSocket-based real-time game mechanics
- **Role-based Game System**: Mafia, Villagers, and special roles
- **Chat System**: Public chat during day phases, private Mafia chat during night phases
- **Player Statistics**: Comprehensive tracking of player performance and achievements
- **Game History**: Browse and replay past games with detailed timelines
- **Enhanced Animations**: Smooth transitions and visual effects
- **Sound System**: Immersive audio experience
- **Responsive Design**: Works on desktop and mobile devices

## Technology Stack

- **Backend**: Spring Boot 3.2.5, Java 21
- **Database**: MySQL 8.0
- **Frontend**: Thymeleaf, Bootstrap 5, JavaScript
- **Real-time Communication**: WebSocket
- **Build Tool**: Maven

## Prerequisites

- Java 21 or higher
- Docker and Docker Compose (for database)
- Maven 3.6+ (or use the included Maven wrapper)

## Quick Start

### 1. Start the Database

```bash
# Start MySQL and phpMyAdmin using Docker Compose
docker-compose up -d

# Verify containers are running
docker-compose ps
```

This will start:
- MySQL database on port 3306
- phpMyAdmin web interface on port 8081

### 2. Build and Run the Application

```bash
# Clean and build the project
./mvnw clean compile

# Run the application
./mvnw spring-boot:run
```

The application will be available at: http://localhost:8080

### 3. Access the Application

- **Main Application**: http://localhost:8080
- **Player Statistics**: http://localhost:8080/statistics
- **Game History**: http://localhost:8080/history
- **Database Admin (phpMyAdmin)**: http://localhost:8081

## Database Configuration

The application uses MySQL with the following default configuration:

- **Database**: mafiadb
- **Username**: mafia_user
- **Password**: mafia_password
- **Host**: localhost:3306

You can modify these settings in `src/main/resources/application.properties`.

## Game Rules

### Roles
- **Villagers**: Win by eliminating all Mafia members
- **Mafia**: Win by eliminating all Villagers or achieving majority

### Game Phases
1. **Day Phase**: All players discuss and vote to eliminate someone
2. **Night Phase**: Mafia members secretly choose a victim

### Chat System
- **Day Phase**: Public chat visible to all players
- **Night Phase**: Private chat for Mafia members only
- **Dead Players**: Cannot send messages but can observe

## Development

### Project Structure

```
src/main/java/com/victadore/webmafia/mafia_web_of_lies/
├── controller/          # REST controllers and web endpoints
├── model/              # JPA entities
├── repository/         # Data access layer
├── service/            # Business logic
├── websocket/          # WebSocket configuration and handlers
└── MafiaWebOfLiesApplication.java

src/main/resources/
├── static/             # CSS, JavaScript, images
├── templates/          # Thymeleaf templates
└── application.properties
```

### Building for Production

```bash
# Build JAR file
./mvnw clean package

# Run the JAR
java -jar target/mafia-web-of-lies-0.0.1-SNAPSHOT.jar
```

### Database Management

```bash
# Stop database
docker-compose down

# Reset database (removes all data)
docker-compose down -v
docker-compose up -d

# View logs
docker-compose logs mysql
```

## API Endpoints

### Game Management
- `POST /api/games/create` - Create a new game
- `POST /api/games/join` - Join an existing game
- `GET /api/games/{gameCode}/waiting-room` - Waiting room page
- `GET /api/games/{gameCode}/game` - Game page

### Statistics
- `GET /statistics` - Player statistics dashboard
- `GET /api/statistics/global` - Global statistics API
- `GET /api/statistics/leaderboard/wins` - Win rate leaderboard
- `GET /api/statistics/leaderboard/games` - Games played leaderboard

### Game History
- `GET /history` - Game history browser
- `GET /api/history/games` - List finished games
- `GET /api/history/games/{gameId}` - Get game details

## Troubleshooting

### Common Issues

1. **Database Connection Error**
   ```bash
   # Ensure MySQL is running
   docker-compose ps
   
   # Check MySQL logs
   docker-compose logs mysql
   ```

2. **Port Already in Use**
   ```bash
   # Check what's using port 8080
   netstat -ano | findstr :8080
   
   # Kill the process or change port in application.properties
   ```

3. **Build Errors**
   ```bash
   # Clean and rebuild
   ./mvnw clean compile
   
   # Update dependencies
   ./mvnw dependency:resolve
   ```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

This project is licensed under the MIT License.
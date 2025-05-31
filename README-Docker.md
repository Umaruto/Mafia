# Mafia Web of Lies - Docker Setup Guide

## Prerequisites
- Docker and Docker Compose installed
- Java 21 JDK
- Maven 3.6+

## Quick Start with Docker

### 1. Start the Database
```bash
# Start MySQL and phpMyAdmin
docker-compose up -d

# Check if containers are running
docker-compose ps
```

### 2. Wait for MySQL to Initialize
The first startup takes a few minutes. Check logs:
```bash
docker-compose logs mysql
```
Wait until you see "MySQL init process done. Ready for start up."

### 3. Run the Application
```bash
# Using MySQL profile
mvn spring-boot:run -Dspring-boot.run.profiles=mysql

# Or build and run JAR
mvn clean package
java -jar target/mafia-web-of-lies-0.0.1-SNAPSHOT.jar --spring.profiles.active=mysql
```

## Database Access

### Application Database
- **URL**: `localhost:3306`
- **Database**: `mafiadb`
- **Username**: `mafiauser`
- **Password**: `mafiapass`

### phpMyAdmin (Web Interface)
- **URL**: http://localhost:8081
- **Username**: `mafiauser`
- **Password**: `mafiapass`

### Root Access (Admin)
- **Username**: `root`
- **Password**: `rootpassword`

## Development vs Production

### Development (H2 Database)
```bash
# Use H2 for quick development
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

### Production (MySQL Database)
```bash
# Use MySQL for production-like environment
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

## Docker Commands

### Start Services
```bash
docker-compose up -d          # Start in background
docker-compose up             # Start with logs
```

### Stop Services
```bash
docker-compose down           # Stop containers
docker-compose down -v        # Stop and remove volumes (deletes data)
```

### Manage Data
```bash
# Backup database
docker exec mafia-mysql mysqldump -u mafiauser -pmafiapass mafiadb > backup.sql

# Restore database
docker exec -i mafia-mysql mysql -u mafiauser -pmafiapass mafiadb < backup.sql

# Access MySQL shell
docker exec -it mafia-mysql mysql -u mafiauser -pmafiapass mafiadb
```

### Logs and Monitoring
```bash
docker-compose logs mysql     # MySQL logs
docker-compose logs -f        # Follow all logs
docker stats                  # Container resource usage
```

## Troubleshooting

### Port Conflicts
If ports 3306 or 8081 are already in use:
```yaml
# Edit docker-compose.yml
ports:
  - "3307:3306"  # Use different host port
  - "8082:80"    # Use different host port
```

### Connection Issues
1. Ensure containers are running: `docker-compose ps`
2. Check MySQL is ready: `docker-compose logs mysql`
3. Verify network: `docker network ls`

### Reset Everything
```bash
docker-compose down -v
docker-compose up -d
# Wait for initialization, then restart application
```

## Configuration Files

- `docker-compose.yml` - Docker services configuration
- `application-mysql.properties` - MySQL Spring configuration
- `application-h2.properties` - H2 development configuration
- `sql/01-init.sql` - Database initialization script

## Notes

- MySQL data persists in Docker volume `mysql_data`
- First startup initializes the database schema automatically
- Chat messages support full UTF-8 (emojis, international characters)
- Connection pooling configured for optimal performance 
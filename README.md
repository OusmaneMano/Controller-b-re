# CONTROLLER Backend - API

SaaS Platform for Business Data Management

## Prerequisites

- Java 17+
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL 16+ (or use Docker Compose)

## Quick Start

### 1. Clone/Extract Project
```bash
cd controller-backend
```

### 2. Start Database
```bash
docker-compose up -d
```

### 3. Build Project
```bash
mvn clean install
```

### 4. Run Application
```bash
mvn spring-boot:run
```

Application runs on: `http://localhost:8080`

### 5. Test Health
```bash
curl http://localhost:8080/api/health
```

## Project Structure

```
controller-backend/
├── src/main/java/com/controller/
│   ├── entity/           # Database models
│   ├── repository/       # Data access
│   ├── service/          # Business logic
│   ├── controller/       # API endpoints
│   ├── security/         # JWT & Auth
│   ├── dto/              # Request/Response
│   ├── exception/        # Error handling
│   └── config/           # Configuration
├── src/main/resources/
│   └── application.properties
├── pom.xml               # Maven dependencies
├── Dockerfile            # Docker image
├── docker-compose.yml    # PostgreSQL setup
└── README.md            # This file
```

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/controller_db
spring.datasource.username=postgres
spring.datasource.password=postgres

# JWT Secret (change in production!)
jwt.secret=your_jwt_secret_key_min_32_chars

# CORS Origins
cors.allowed-origins=http://localhost:3000,http://localhost:4200
```

## Database Setup

### Option 1: Docker Compose (Recommended)
```bash
docker-compose up -d
```

### Option 2: Local PostgreSQL
```bash
# Create database
createdb controller_db

# Update application.properties with your credentials
```

## Building

```bash
# Clean build
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Build Docker image
docker build -t controller-backend:latest .
```

## Running

```bash
# Development mode
mvn spring-boot:run

# Production mode
java -jar target/controller-backend-1.0.0.jar

# Docker
docker run -d \
  -e DATABASE_URL=jdbc:postgresql://postgres:5432/controller_db \
  -e DATABASE_USER=postgres \
  -e DATABASE_PASSWORD=postgres \
  -e JWT_SECRET=your_secret \
  -p 8080:8080 \
  controller-backend:latest
```

## API Endpoints

### Health Check
```bash
GET http://localhost:8080/api/health
```

### Authentication (Coming Soon)
```bash
POST /api/auth/manager/signup
POST /api/auth/manager/login
POST /api/auth/employee/signup
POST /api/auth/employee/verify-credentials
```

## Troubleshooting

### Port 8080 Already in Use
```bash
# Change port in application.properties
server.port=8081
```

### Database Connection Error
```bash
# Check Docker is running
docker-compose ps

# View logs
docker logs controller_postgres
```

### Maven Build Fails
```bash
# Clear cache and rebuild
mvn clean install -U
```

## Technologies

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: PostgreSQL 16
- **Security**: Spring Security + JWT
- **Build**: Maven 3.9
- **Containerization**: Docker & Docker Compose

## License

Proprietary - CONTROLLER Platform

## Support

For issues and questions, contact: ousmanemanot@gmail.com

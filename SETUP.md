# SETUP & INSTALLATION GUIDE

## Prerequisites

Before running the backend, ensure you have the following installed:

1. **Java 21 JDK**
   - Download: https://www.oracle.com/java/technologies/downloads/#java21
   - Verify: `java -version`

2. **Maven 3.9+**
   - Download: https://maven.apache.org/download.cgi
   - Verify: `mvn -version`

3. **Docker & Docker Compose**
   - Download: https://www.docker.com/products/docker-desktop
   - Verify: `docker --version` and `docker-compose --version`

4. **Git** (optional)
   - Download: https://git-scm.com/download

---

## Step 1: Start PostgreSQL Database

### Option A: Using Docker Compose (Recommended)

```bash
cd finsimx-backend
docker-compose up -d postgres
```

This will:

- Download PostgreSQL 16 image
- Create a container named `finsimx-postgres`
- Expose port 5432
- Create a database called `finsimx`
- Default credentials: username=postgres, password=postgres

### Option B: Manual PostgreSQL Setup

If you have PostgreSQL installed locally:

1. Create a database: `CREATE DATABASE finsimx;`
2. Ensure it's running on localhost:5432

### Verify PostgreSQL is Running

```bash
# Should show postgres process running
docker ps | grep postgres

# Or connect directly
psql -h localhost -U postgres -d finsimx
```

---

## Step 2: Build the Project

```bash
cd finsimx-backend
mvn clean install
```

This will:

- Download all dependencies
- Compile Java code
- Run any unit tests
- Create target/finsimx-backend-1.0.0.jar

---

## Step 3: Configure Database Connection

The application automatically migrates the database schema using Flyway.

**Current Configuration** (in `application.yml`):

```yaml
datasource:
  url: jdbc:postgresql://localhost:5432/finsimx
  username: postgres
  password: postgres
```

If your PostgreSQL credentials are different, update `application.yml` before running.

---

## Step 4: Run the Application

### Option A: Using Maven (Development)

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### Option B: Using Java Directly

```bash
java -jar target/finsimx-backend-1.0.0.jar
```

### Option C: Using Startup Script

**On Windows:**

```bash
start.bat
```

**On Linux/Mac:**

```bash
chmod +x start.sh
./start.sh
```

---

## Step 5: Verify Everything is Running

### Check Health Endpoint

```bash
curl http://localhost:8080/api/health
```

Expected Response:

```json
{
  "status": "UP",
  "application": "FinSimX Trading Engine",
  "version": "1.0.0"
}
```

### Check Database Connection

```bash
# Connect to PostgreSQL
psql -h localhost -U postgres -d finsimx

# List tables
\dt

# Should show: users, orders, trades
```

---

## Troubleshooting

### PostgreSQL Connection Failed

```
Error: Connection to localhost:5432 refused
```

**Solution:**

1. Ensure Docker is running: `docker ps`
2. Check if container is running: `docker ps | grep postgres`
3. Restart container: `docker-compose down && docker-compose up -d postgres`
4. Check logs: `docker logs finsimx-postgres`

### Port 5432 Already in Use

```
Error: Address already in use
```

**Solution:**

1. Find process using port: `lsof -i :5432` (Mac/Linux) or `netstat -ano | findstr 5432` (Windows)
2. Kill the process or change the port in `docker-compose.yml`

### Maven Build Fails

```
Error: [ERROR] Failed to execute goal org.springframework.boot:spring-boot-maven-plugin:3.2.1:repackage
```

**Solution:**

1. Clear Maven cache: `mvn clean`
2. Download dependencies: `mvn dependency:resolve`
3. Rebuild: `mvn clean install`

### Java Version Issue

```
Error: Unsupported class version
```

**Solution:**

- Ensure Java 21 is installed: `java -version`
- Set JAVA_HOME: `export JAVA_HOME=/path/to/java21`

---

## Database Schema Overview

After running the application, the following tables are automatically created:

### USERS

```sql
SELECT * FROM users LIMIT 5;
```

### ORDERS

```sql
SELECT * FROM orders LIMIT 5;
```

### TRADES

```sql
SELECT * FROM trades LIMIT 5;
```

---

## Development Tips

### Hot Reload (Automatic Restart on File Changes)

Spring Boot Dev Tools are included. Any Java file change will trigger a restart:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### Database Console (optional)

To access the database via a web interface:

```bash
# Install pgAdmin (optional)
docker run --name pgadmin -p 5050:80 -e PGADMIN_DEFAULT_EMAIL=admin@admin.com -e PGADMIN_DEFAULT_PASSWORD=admin dpage/pgadmin4
```

Then visit: http://localhost:5050

### Logging

View application logs:

```bash
# Real-time logs
docker logs -f finsimx-postgres

# Or save logs to file
docker logs finsimx-postgres > postgres.log
```

---

## Next Steps

Once the backend is running:

1. **Step 2**: User Management API (Registration/Login)
2. **Step 3**: Wallet Service
3. **Step 4**: Order Placement API
4. **Step 5**: Matching Engine
5. **Step 6**: Trade Execution
6. **Step 7**: WebSocket Streaming
7. **Step 8**: React Frontend

---

## Stopping Services

To stop everything:

```bash
# Stop the Spring Boot application (Ctrl+C in terminal)

# Stop PostgreSQL container
docker-compose down

# Remove all data (clean slate)
docker-compose down -v
```

---

## Next Steps

Once your backend is successfully running and healthy, proceed to **Step 2: User Authentication & REST API** where we'll implement:

- User registration
- User login with JWT
- Password hashing
- REST endpoints for user management

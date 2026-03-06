# FinSimX Backend

## Project Structure

```
finsimx-backend/
├── pom.xml
├── docker-compose.yml
├── src/
│   └── main/
│       ├── java/com/finsimx/
│       │   ├── entity/              # JPA Entities
│       │   ├── repository/          # Data Access Layer
│       │   ├── service/             # Business Logic Layer
│       │   ├── controller/          # REST Controllers
│       │   ├── config/              # Configuration Classes
│       │   ├── dto/                 # Data Transfer Objects
│       │   └── FinSimXApplication.java
│       └── resources/
│           ├── application.yml
│           └── db/migration/        # Flyway SQL migrations
```

## Database Schema

### Users Table

- `id` - Auto-generated primary key
- `username` - Unique username
- `password` - Hashed password
- `email` - Unique email
- `balance` - Account balance (default 100,000)

### Orders Table

- `id` - Order ID
- `user_id` - User placing order
- `asset` - Trading pair (e.g., AAPL, EURUSD)
- `type` - BUY or SELL
- `price` - Limit price
- `quantity` - Order quantity
- `filled_quantity` - Partially filled amount
- `status` - OPEN, PARTIAL, FILLED, CANCELLED
- `created_at`, `updated_at` - Timestamps

### Trades Table

- `id` - Trade ID
- `buyer_id` - Buyer user ID
- `seller_id` - Seller user ID
- `asset` - Trading pair
- `price` - Execution price
- `quantity` - Executed quantity
- `created_at` - Trade timestamp

## Dependencies

- **Spring Boot 3.2.1** - Framework
- **Spring Data JPA** - ORM
- **Spring Security** - Authentication
- **Spring WebSocket** - Real-time messaging
- **PostgreSQL 16** - Database
- **Flyway** - Database migrations
- **JWT** - Token-based auth
- **Lombok** - Boilerplate reduction

## Prerequisites

- Java 21 JDK
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL 16 (via Docker)

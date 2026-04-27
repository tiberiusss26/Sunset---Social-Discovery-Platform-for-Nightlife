# NightOut 🌙

> A social discovery platform for nightlife. Browse tonight's bars and clubs,
> see where your friends are going, RSVP, rate venues, and never miss a great night out.

---

## Table of Contents
1. [Architecture](#architecture)
2. [Entity Model](#entity-model)
3. [Setup Instructions](#setup-instructions)
4. [Running the App](#running-the-app)
5. [API Documentation](#api-documentation)
6. [Testing](#testing)
7. [Security](#security)
8. [Caching](#caching)
9. [Monitoring](#monitoring)
10. [Project Structure](#project-structure)
11. [Team Contributions](#team-contributions)

---

## Architecture

NightOut is built as a **monolith in Phase 1** and migrated to **microservices in Phase 2**.

### Phase 1 — Monolith
Single Spring Boot application with all 7 entities, full CRUD, Spring Security (JWT),
Redis caching, pagination, SLF4J logging, and a React frontend.

### Tech stack
| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.2, Spring Security, Spring Data JPA |
| Database | PostgreSQL (dev), H2 (test) |
| Cache | Redis |
| Auth | JWT (JJWT 0.12) |
| Testing | JUnit 5, Mockito, MockMvc |
| Logging | SLF4J + Logback |
| Monitoring | Spring Actuator + Prometheus + Grafana |
| Containerisation | Docker + Docker Compose |
| Frontend | React + Tailwind CSS |

---

## Entity Model

```
User ──ManyToMany──▶ Role
User ──ManyToMany──▶ User       (self-referencing follows)
User ──OneToMany──▶  Rsvp
User ──OneToMany──▶  VenueRating
User ──OneToMany──▶  Venue      (owned venues)
Venue ──OneToOne──▶  Address    ← the OneToOne requirement
Venue ──OneToMany──▶ Night
Venue ──OneToMany──▶ VenueRating
Night ──OneToMany──▶ Rsvp
Night ──ManyToMany──▶ Tag
```

Relationships covered:
- ✅ `@OneToOne` — Venue → Address
- ✅ `@OneToMany` / `@ManyToOne` — Venue → Night, User → Rsvp, User → VenueRating
- ✅ `@ManyToMany` — User ↔ Role, Night ↔ Tag, User ↔ User (follows)

---

## Setup Instructions

### Prerequisites
- Java 17+
- Maven 3.9+
- Docker & Docker Compose
- Node.js 18+ (for React frontend)

### Option A — Full Docker stack (recommended)

```bash
# Clone the repository
git clone https://github.com/your-team/nightout.git
cd nightout

# Start everything (PostgreSQL + Redis + App + Prometheus + Grafana)
docker compose up --build

# App is available at:
# http://localhost:8080      — Spring Boot API
# http://localhost:3000      — Grafana (admin/admin)
# http://localhost:9090      — Prometheus
```

### Option B — Run locally without Docker

```bash
# 1. Start PostgreSQL and Redis (you need them installed locally)
psql -U postgres -c "CREATE DATABASE nightout_dev;"
redis-server

# 2. Run the Spring Boot app
./gradlew bootRun --args="--spring.profiles.active=dev"

# 3. App runs at http://localhost:8080
```

### Default credentials (seeded on dev startup)

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@nightout.com | Admin1234! |
| Venue Owner | owner@clubnova.com | Owner1234! |
| Regular User | alice@example.com | User1234! |

---

## Running the App

```bash
# Development (PostgreSQL + Redis required)
./gradlew bootRun --args="--spring.profiles.active=dev"

# Run all tests (H2 in-memory, no external dependencies)
./gradlew test

# Run tests with coverage report
./gradlew test jacocoTestReport
# Open target/site/jacoco/index.html in your browser

# Package as JAR
./gradlew build -x test
java -jar build/libs/nightout.jar
```

---

## API Documentation

### Authentication

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | ❌ | Register new user |
| POST | `/api/auth/login` | ❌ | Login, returns JWT |

All protected endpoints require: `Authorization: Bearer <token>`

### Venues

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/venues` | ❌ | Personalised ranked venue feed |
| GET | `/api/venues/search?q=` | ❌ | Search venues by name |
| GET | `/api/venues/{id}` | ❌ | Venue details |
| POST | `/api/venues` | VENUE_OWNER | Create venue |
| PUT | `/api/venues/{id}` | VENUE_OWNER | Update venue |
| DELETE | `/api/venues/{id}` | VENUE_OWNER | Delete venue |
| GET | `/api/venues/{id}/ratings` | ❌ | Venue ratings |
| POST | `/api/venues/{id}/ratings` | USER | Rate a venue |

Pagination params: `?page=0&size=12&sort=averageRating,desc`

### Nights

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/nights/tonight` | ❌ | All active nights tonight |
| GET | `/api/nights/friends-feed` | USER | Nights your friends are attending |
| GET | `/api/nights/my-plans` | USER | Your upcoming RSVPs |
| GET | `/api/nights/{id}` | ❌ | Night details |
| GET | `/api/venues/{id}/nights` | ❌ | All nights for a venue |
| POST | `/api/venues/{id}/nights` | VENUE_OWNER | Post a new night |
| PUT | `/api/nights/{id}` | VENUE_OWNER | Update a night |
| DELETE | `/api/nights/{id}` | VENUE_OWNER | Delete a night |

### RSVPs

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/nights/{id}/rsvps` | USER | RSVP for a night |
| DELETE | `/api/nights/{id}/rsvps` | USER | Cancel RSVP |
| GET | `/api/users/me/rsvps` | USER | My RSVP history |

### Users

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/users/me` | USER | My profile |
| PUT | `/api/users/me` | USER | Update my profile |
| GET | `/api/users/{id}` | USER | Another user's profile |
| POST | `/api/users/{id}/follow` | USER | Follow a user |
| DELETE | `/api/users/{id}/follow` | USER | Unfollow a user |
| GET | `/api/users/search?q=` | USER | Search users |

---

## Testing

```bash
# Run all tests
./gradlew test

# Run only unit tests
mvn test -Dgroups="unit"

# Run only integration tests
mvn test -Dgroups="integration"

# Coverage report (requires jacoco plugin)
mvn verify
```

### Test coverage targets
- Service layer unit tests: **≥ 70% line coverage**
- Integration test scenarios: **≥ 3 end-to-end flows**

---

## Security

- **Authentication**: JWT (HS256, 24h expiry)
- **Password hashing**: BCrypt strength 12
- **Roles**: `ROLE_USER`, `ROLE_VENUE_OWNER`, `ROLE_ADMIN`
- **CSRF**: Disabled (stateless JWT API)
- **Session**: Stateless (`STATELESS` session creation policy)

---

## Caching

Redis caches the `rankedVenues` endpoint with a 30-minute TTL.
Cache is evicted automatically when venues, ratings, or RSVPs change.

```
Cache key format: {userId}_{date}_{pageNumber}
TTL: rankedVenues=30min, nightDetails=5min, userProfiles=10min
```

---

## Monitoring

Access the monitoring stack after `docker compose up`:

| Service | URL | Credentials |
|---------|-----|-------------|
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | — |
| Actuator health | http://localhost:8080/actuator/health | — |
| Actuator metrics | http://localhost:8080/actuator/prometheus | — |

---

## Project Structure

```
nightout/
├── src/main/java/com/nightout/
│   ├── NightOutApplication.java      ← Entry point
│   ├── config/
│   │   ├── RedisConfig.java          ← Cache configuration
│   │   └── DataInitializer.java      ← Dev seed data
│   ├── domain/                       ← JPA Entities
│   │   ├── BaseEntity.java
│   │   ├── User.java
│   │   ├── Role.java
│   │   ├── Venue.java
│   │   ├── Address.java
│   │   ├── Night.java
│   │   ├── Rsvp.java
│   │   ├── VenueRating.java
│   │   └── Tag.java
│   ├── repository/                   ← Spring Data JPA interfaces
│   ├── service/                      ← Business logic
│   │   ├── AuthAndUserService.java
│   │   ├── VenueService.java
│   │   └── NightAndRsvpService.java
│   ├── controller/                   ← HTTP endpoints
│   │   ├── AuthAndUserController.java
│   │   └── VenueNightRsvpController.java
│   ├── dto/                          ← Request/Response objects
│   ├── exception/                    ← Custom exceptions + GlobalExceptionHandler
│   └── security/                     ← JWT + Spring Security config
├── src/main/resources/
│   ├── application.yml               ← Base config
│   ├── application-dev.yml           ← PostgreSQL config
│   ├── application-test.yml          ← H2 config
│   └── logback-spring.xml            ← Logging config
├── src/test/java/com/nightout/
│   ├── service/ServiceTests.java     ← Unit tests (Mockito)
│   └── controller/IntegrationTests.java ← Integration tests (MockMvc)
├── monitoring/
│   └── prometheus.yml                ← Prometheus scrape config
├── docker-compose.yml                ← Full local stack
├── Dockerfile                        ← Multi-stage build
└── README.md
```

---
# NightOut 🌙

> A social nightlife discovery platform. Verified venues post their nightly programs.
> Users browse tonight's scene, RSVP, reserve tables, rate venues, and see where
> their friends are going — all in one place.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Tech Stack](#2-tech-stack)
3. [Entity Model & Relationships](#3-entity-model--relationships)
4. [Project Structure](#4-project-structure)
5. [Setup & Running](#5-setup--running)
6. [Default Accounts](#6-default-accounts)
7. [API Reference](#7-api-reference)
8. [Security](#8-security)
9. [Caching](#9-caching)
10. [Logging](#10-logging)
11. [Testing](#11-testing)
12. [Monitoring](#12-monitoring)
13. [Microservices Architecture](#13-microservices-architecture)
14. [Deployment](#14-deployment)
15. [Team Contributions](#15-team-contributions)

---

## 1. Project Overview

NightOut is built in two phases:

**Phase 1 — Monolith** (`nightout-monolith/`)
A single Spring Boot application covering all mandatory requirements: 7 entities,
full CRUD, Spring Security with JWT, Redis caching, pagination, logging, and tests.

**Phase 2 — Microservices** (`nightout-microservices/`)
The monolith is decomposed into 4 independent services coordinated by Eureka,
Spring Cloud Gateway, a Config Server, RabbitMQ, Resilience4j, and a Saga pattern
for distributed RSVP transactions.

---

## 2. Tech Stack

| Concern | Technology |
|---|---|
| Framework | Spring Boot 3.2 |
| Security | Spring Security + JWT (JJWT 0.12) |
| Persistence | Spring Data JPA + Hibernate |
| Primary DB (dev) | PostgreSQL 16 |
| Test DB | H2 in-memory |
| Cache | Redis 7 |
| NoSQL (notifications) | MongoDB 7 |
| Messaging | RabbitMQ 3.13 |
| Service registry | Netflix Eureka |
| API gateway | Spring Cloud Gateway |
| Config management | Spring Cloud Config Server |
| Fault tolerance | Resilience4j (circuit breaker + retry) |
| Inter-service calls | OpenFeign + Spring Cloud LoadBalancer |
| Metrics | Spring Actuator + Prometheus + Grafana |
| Logging | SLF4J + Logback |
| Build | Gradle 8.7 (Kotlin DSL) |
| Containers | Docker + Docker Compose |

---

## 3. Entity Model & Relationships

```
User ──@ManyToMany──▶ Role
User ──@ManyToMany──▶ User         (self-referencing follow graph)
User ──@OneToMany──▶  Rsvp
User ──@OneToMany──▶  VenueRating
User ──@OneToMany──▶  Venue        (owned venues)
Venue ──@OneToOne──▶  Address      ← satisfies @OneToOne requirement
Venue ──@OneToMany──▶ Night
Venue ──@OneToMany──▶ VenueRating
Night ──@OneToMany──▶ Rsvp
Night ──@ManyToMany──▶ Tag
```

All required relationship types from the brief are covered naturally:

| Type | Example |
|---|---|
| `@OneToOne` | `Venue` → `Address` |
| `@OneToMany` / `@ManyToOne` | `Venue` → `Night`, `User` → `Rsvp`, `User` → `VenueRating` |
| `@ManyToMany` | `User` ↔ `Role`, `Night` ↔ `Tag`, `User` ↔ `User` (follows) |

---

## 4. Project Structure

```
nightout-monolith/
├── build.gradle.kts                  ← Gradle build (replaces pom.xml)
├── settings.gradle.kts
├── gradlew                           ← Gradle wrapper (run without installing Gradle)
├── gradle/wrapper/
│   └── gradle-wrapper.properties
├── Dockerfile                        ← Multi-stage build
├── docker-compose.yml                ← Full local stack
├── .gitignore
├── README.md
├── monitoring/
│   ├── prometheus.yml
│   └── grafana/provisioning/
│       ├── datasources/prometheus.yml
│       └── dashboards/
│           ├── dashboards.yml
│           └── nightout-dashboard.json
└── src/
    ├── main/
    │   ├── java/com/nightout/
    │   │   ├── NightOutApplication.java
    │   │   ├── config/
    │   │   │   ├── DataInitializer.java   ← Seeds dev DB on startup
    │   │   │   └── RedisConfig.java       ← Cache TTL configuration
    │   │   ├── domain/                    ← JPA entities (one file each)
    │   │   │   ├── BaseEntity.java
    │   │   │   ├── User.java
    │   │   │   ├── Role.java
    │   │   │   ├── Venue.java
    │   │   │   ├── Address.java
    │   │   │   ├── Night.java
    │   │   │   ├── Rsvp.java
    │   │   │   ├── VenueRating.java
    │   │   │   └── Tag.java
    │   │   ├── repository/               ← Spring Data JPA (one file per entity)
    │   │   │   ├── UserRepository.java
    │   │   │   ├── RoleRepository.java
    │   │   │   ├── VenueRepository.java
    │   │   │   ├── NightRepository.java
    │   │   │   ├── RsvpRepository.java
    │   │   │   ├── VenueRatingRepository.java
    │   │   │   └── TagRepository.java
    │   │   ├── dto/                      ← Request/response objects (one file each)
    │   │   │   ├── RegisterRequest.java
    │   │   │   ├── LoginRequest.java
    │   │   │   ├── AuthResponse.java
    │   │   │   ├── UserSummary.java
    │   │   │   ├── UserProfileResponse.java
    │   │   │   ├── UpdateProfileRequest.java
    │   │   │   ├── AddressRequest.java
    │   │   │   ├── AddressResponse.java
    │   │   │   ├── CreateVenueRequest.java
    │   │   │   ├── VenueSummary.java
    │   │   │   ├── VenueResponse.java
    │   │   │   ├── CreateNightRequest.java
    │   │   │   ├── NightSummary.java
    │   │   │   ├── NightResponse.java
    │   │   │   ├── CreateRsvpRequest.java
    │   │   │   ├── RsvpResponse.java
    │   │   │   ├── CreateRatingRequest.java
    │   │   │   ├── RatingResponse.java
    │   │   │   └── PageResponse.java
    │   │   ├── exception/
    │   │   │   ├── ResourceNotFoundException.java
    │   │   │   ├── DuplicateResourceException.java
    │   │   │   ├── BusinessRuleException.java
    │   │   │   ├── UnauthorizedException.java
    │   │   │   ├── ApiError.java
    │   │   │   └── GlobalExceptionHandler.java
    │   │   ├── security/
    │   │   │   ├── JwtTokenProvider.java
    │   │   │   ├── JwtAuthFilter.java
    │   │   │   ├── CustomUserDetailsService.java
    │   │   │   └── SecurityConfig.java
    │   │   ├── service/
    │   │   │   ├── AuthService.java
    │   │   │   ├── UserService.java
    │   │   │   ├── VenueService.java
    │   │   │   ├── NightService.java
    │   │   │   ├── RsvpService.java
    │   │   │   └── NotificationService.java
    │   │   └── controller/
    │   │       ├── AuthController.java
    │   │       ├── UserController.java
    │   │       ├── VenueController.java
    │   │       ├── NightController.java
    │   │       ├── RsvpController.java
    │   │       └── AdminController.java
    │   └── resources/
    │       ├── application.yml           ← Base config (all profiles)
    │       ├── application-dev.yml       ← PostgreSQL
    │       ├── application-test.yml      ← H2 in-memory
    │       └── logback-spring.xml        ← Async file logging
    └── test/
        └── java/com/nightout/
            ├── service/ServiceTests.java      ← Unit tests (Mockito)
            └── controller/IntegrationTests.java ← End-to-end (MockMvc + H2)
```

---

## 5. Setup & Running

### Prerequisites

- Java 17+
- Docker & Docker Compose
- (Optional) PostgreSQL and Redis if running without Docker

### Option A — Docker (recommended, zero setup)

```bash
git clone https://github.com/your-team/nightout.git
cd nightout/nightout-monolith

# Start app + PostgreSQL + Redis + Prometheus + Grafana
docker compose up --build

# API:     http://localhost:8080
# Grafana: http://localhost:3000  (admin / admin)
```

### Option B — Gradle directly

```bash
# Requires PostgreSQL on localhost:5432 and Redis on localhost:6379
cd nightout-monolith

./gradlew bootRun --args="--spring.profiles.active=dev"
```

### Useful Gradle commands

```bash
./gradlew test                  # Run all tests (uses H2, no external DB needed)
./gradlew test jacocoTestReport # Tests + coverage report
./gradlew build -x test         # Build JAR without running tests
./gradlew dependencies          # Print dependency tree
```

Coverage report: `build/reports/jacoco/test/html/index.html`

---

## 6. Default Accounts

Seeded automatically on first `dev` profile startup by `DataInitializer`.

| Role | Email | Password |
|---|---|---|
| Admin | admin@nightout.com | Admin1234! |
| Venue Owner (Club Nova) | owner@clubnova.com | Owner1234! |
| Venue Owner (Skybar) | owner@skybar.com | Owner1234! |
| Regular User | alice@example.com | User1234! |
| Regular User | bob@example.com | User1234! |

---

## 7. API Reference

All protected endpoints require: `Authorization: Bearer <token>`

Pagination parameters (where supported): `?page=0&size=12&sort=field,asc`

### Auth — public

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register → returns JWT |
| `POST` | `/api/auth/login` | Login → returns JWT |

### Users — authenticated

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/users/me` | My profile |
| `PUT` | `/api/users/me` | Update my profile |
| `GET` | `/api/users/{id}` | Another user's public profile |
| `POST` | `/api/users/{id}/follow` | Follow a user |
| `DELETE` | `/api/users/{id}/follow` | Unfollow a user |
| `GET` | `/api/users/search?q=` | Search users by username |

### Venues — GET public, write requires VENUE_OWNER or ADMIN

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/venues` | Personalised ranked feed |
| `GET` | `/api/venues/search?q=` | Search venues by name |
| `GET` | `/api/venues/{id}` | Venue details |
| `POST` | `/api/venues` | Create venue |
| `PUT` | `/api/venues/{id}` | Update venue (owner or admin) |
| `DELETE` | `/api/venues/{id}` | Delete venue (owner or admin) |
| `GET` | `/api/venues/{id}/ratings` | Paginated ratings |
| `POST` | `/api/venues/{id}/ratings` | Rate a venue (authenticated) |

### Nights — GET public, write requires VENUE_OWNER or ADMIN

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/nights/tonight` | All active nights tonight |
| `GET` | `/api/nights/friends-feed` | Nights your friends attend (auth required) |
| `GET` | `/api/nights/my-plans` | Your upcoming RSVPs (auth required) |
| `GET` | `/api/nights/{id}` | Night details |
| `GET` | `/api/venues/{id}/nights` | All nights for a venue |
| `POST` | `/api/venues/{id}/nights` | Post a new night |
| `PUT` | `/api/nights/{id}` | Update a night |
| `DELETE` | `/api/nights/{id}` | Delete a night |

### RSVPs — authenticated

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/nights/{id}/rsvps` | RSVP or update existing RSVP |
| `DELETE` | `/api/nights/{id}/rsvps` | Cancel RSVP |
| `GET` | `/api/users/me/rsvps` | My RSVP history |

### Admin — ADMIN role only

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/admin/stats` | Platform stats (users, venues, nights, RSVPs) |
| `GET` | `/api/admin/venues` | All venues including unverified |
| `PATCH` | `/api/admin/venues/{id}/verify` | Verify or unverify a venue |
| `GET` | `/api/admin/users` | All users |
| `PATCH` | `/api/admin/users/{id}/enabled` | Enable or disable a user account |

---

## 8. Security

- **Authentication**: JWT (HMAC-SHA256), 24-hour expiry
- **Password hashing**: BCrypt strength 12
- **Roles**: `ROLE_USER`, `ROLE_VENUE_OWNER`, `ROLE_ADMIN`
- **CSRF**: Disabled — stateless JWT API
- **Session**: `STATELESS` — no server-side session
- **Ownership checks**: service layer verifies resource ownership beyond role checks

---

## 9. Caching

Redis caches the personalised venue ranking query which is the most expensive
operation in the app (joins venues, nights, RSVPs, and applies a scoring formula).

| Cache name | TTL | Evicted when |
|---|---|---|
| `rankedVenues` | 30 min | Venue created/updated/deleted or rating added |
| `nightDetails` | 5 min | Night updated or RSVP changes |
| `userProfiles` | 10 min | Profile updated |

Cache key format: `{userId}_{date}_{pageNumber}`

---

## 10. Logging

Configured in `logback-spring.xml`:

- **Console**: all levels, coloured output
- **`logs/nightout.log`**: all levels, rotated daily, 30-day retention
- **`logs/nightout-errors.log`**: ERROR only, 90-day retention, full stack traces
- **Async appender**: log writes are buffered in a 512-event queue and written
  on a background thread so logging never blocks HTTP request threads

Log levels:

| Logger | Level |
|---|---|
| `com.nightout` | DEBUG (dev) / INFO (prod) |
| `org.hibernate.SQL` | DEBUG |
| `org.springframework.security` | DEBUG |
| Root | INFO |

---

## 11. Testing

```bash
./gradlew test                  # All tests
./gradlew test jacocoTestReport # Tests + HTML coverage report
```

### Unit tests (`ServiceTests.java`)

Tests the service layer in complete isolation using Mockito mocks.
No database, no Spring context, no HTTP — pure business logic.

Coverage: **≥ 70% line coverage on the service package** (enforced by JaCoCo).

| Test class | What it covers |
|---|---|
| `VenueServiceTest` | createVenue, getVenueById, deleteVenue (owner vs admin vs intruder), addRating |
| `NightServiceTest` | createNight (owner check, tag find-or-create), deleteNight |
| `RsvpServiceTest` | RSVP upsert, past-night rejection, notification trigger, no-notification for INTERESTED |

### Integration tests (`IntegrationTests.java`)

Full stack: real Spring context, H2 database, MockMvc HTTP simulation.

| Scenario | What it proves |
|---|---|
| Register → login → access `/api/users/me` | Auth flow end-to-end |
| Venue owner creates venue → posts night → night appears in venue list | CRUD chain |
| User RSVPs → appears in `/api/nights/my-plans` | RSVP flow |
| Regular user gets 403 on `POST /api/venues` | Role enforcement |
| Unauthenticated RSVP gets 401 | Security filter |
| Duplicate email registration gets 409 | Validation |
| Invalid email format gets 400 with field errors | Bean Validation |

---

## 12. Monitoring

After `docker compose up`:

| Service | URL | Credentials |
|---|---|---|
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | — |
| Actuator health | http://localhost:8080/actuator/health | — |
| Actuator metrics | http://localhost:8080/actuator/prometheus | — |

The Grafana dashboard (`NightOut — Application Metrics`) is provisioned automatically
and shows: HTTP requests/sec, P99 latency, JVM heap usage, and requests by endpoint.

---

## 13. Microservices Architecture

The microservices phase lives in `nightout-microservices/` and decomposes the
monolith into 4 independent services plus 3 infrastructure components.

```
Client
  │
  ▼
API Gateway (port 8080)
  │  JWT validation at the edge
  │  Rate limiting via Redis token bucket
  │  lb:// routing via Eureka
  │
  ├──▶ user-service   (8081)  Auth, JWT issuance, social graph
  ├──▶ venue-service  (8082)  Venues, ratings, personalised ranking
  ├──▶ night-service  (8083)  Nights, RSVPs, friends feed, Saga
  └──▶ notification-service (8084)  Async email, MongoDB history

Infrastructure:
  Eureka Server   (8761)   Service registry — all services register here
  Config Server   (8888)   Centralised config served from classpath/config/
  RabbitMQ        (5672)   RSVP confirmed events → notification-service
  Redis           (6379)   Gateway rate limiter + venue-service cache
  PostgreSQL      (5432)   3 separate databases (one per stateful service)
  MongoDB         (27017)  Notification history documents
  Prometheus      (9090)   Scrapes /actuator/prometheus on all 4 services
  Grafana         (3000)   Dashboards
```

### How each requirement maps to code

| Requirement | Points | Implementation |
|---|---|---|
| Config centralizată | 4% | `config-server` — all service config in one place |
| Service discovery | 6% | `eureka-server` — services register and discover by name |
| Load balancing | 5% | `lb://` URIs in gateway routes + `docker compose up --scale night-service=2` |
| API Gateway | 4% | `api-gateway` — routing, rate limiting, JWT filter |
| Monitorizare | 5% | Actuator + Prometheus scraping all services + Grafana |
| Securitate distribuită | 4% | JWT validated at gateway, forwarded as `X-User-Id` header |
| Resilience / Fault Tolerance | 5% | `@CircuitBreaker` + `@Retry` on all Feign calls, graceful fallbacks |
| Design Patterns | 3% | Choreography Saga in `night-service` for RSVP distributed transaction |
| NoSQL & Caching | 4% | MongoDB for notification history, Redis for venue ranking cache |

### RSVP Saga flow

```
User POSTs /api/nights/{id}/rsvps
  │
  ▼
night-service: save RSVP (sagaState=PENDING) ← local DB transaction
  │
  ▼
night-service: publish RsvpConfirmedEvent → RabbitMQ exchange → nightout.notifications queue
  │
  ▼
notification-service: consume event → send email → save NotificationRecord to MongoDB
  │                                                                │
  ├── success → publish RsvpNotifiedEvent                         │
  │    └── night-service: set sagaState=COMPLETED                 │
  │                                                               │
  └── failure → publish RsvpNotificationFailedEvent               │
       └── night-service: set sagaState=COMPENSATED,             │
                          set rsvp status=CANCELLED  ◄───────────┘
```

### Running microservices

```bash
cd nightout-microservices

# Start everything (11 containers)
docker compose up --build

# Demonstrate load balancing: run 2 instances of night-service
docker compose up --scale night-service=2

# Eureka dashboard: http://localhost:8761
# RabbitMQ UI:      http://localhost:15672  (guest/guest)
```

---

## 14. Deployment

### Local (Docker Compose)

```bash
# Monolith
cd nightout-monolith && docker compose up --build

# Microservices
cd nightout-microservices && docker compose up --build
```

### Cloud (Railway / Render)

1. Push to GitHub
2. Connect repo to Railway or Render
3. Set environment variables:
    - `SPRING_PROFILES_ACTIVE=dev`
    - `SPRING_DATASOURCE_URL=jdbc:postgresql://...`
    - `SPRING_DATA_REDIS_HOST=...`
    - `APP_JWT_SECRET=<minimum 32 char secret>`
4. Deploy — the Dockerfile is picked up automatically

---

## 15. Team Contributions

| Member | Responsibilities |
|---|---|
| [Name 1] | Domain model, all repositories, VenueService, VenueController, Redis caching |
| [Name 2] | Spring Security, JWT, AuthService, UserService, AuthController, UserController |
| [Name 3] | NightService, RsvpService, NightController, tests, Docker, monitoring |

> Update this table with your actual names and split of work before submission.
> The professor will check git commit history — make sure each member has
> meaningful commits across the codebase.

---

*Built for the Web Applications with Microservices course.*
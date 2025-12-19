# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**GgUd** - A meeting coordination and settlement system backend built with Spring Boot. The application helps users arrange meetings by calculating midpoints between participants, recommending places via AI, and tracking real-time locations.

### Tech Stack
- **Framework**: Spring Boot 3.5.7 (Java 17)
- **Database**: PostgreSQL 15+
- **Cache**: Redis 7
- **Build Tool**: Gradle
- **Container**: Docker Compose

## Common Commands

### Development
```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run specific test class
./gradlew test --tests ClassName

# Run application locally (without Docker)
./gradlew bootRun

# Clean build artifacts
./gradlew clean
```

### Docker Environment
```bash
# Start all services (PostgreSQL, Redis, App)
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop all services
docker-compose down

# Rebuild and restart
docker-compose up -d --build

# View database logs
docker-compose logs -f db
```

### Database Access
```bash
# Connect to PostgreSQL container
docker exec -it ggud-postgres psql -U postgres -d ggud_db

# Connect to Redis
docker exec -it ggud-redis redis-cli
```

## Architecture

### Package Structure
```
dev.promise4.GgUd/
├── entity/              # JPA entities (database models)
├── repository/          # Data access layer (JPA repositories)
├── service/             # Business logic layer
├── controller/          # REST API controllers
└── GgUdApplication.java # Main Spring Boot application
```

### Core Domain Model

The application follows a meeting-centric architecture:

**Users (`UsersEntity`)**
- Kakao OAuth2 authentication
- Soft delete support (`deletedAt`)
- Notification token for push notifications

**Meetings (`MeetingsEntity`)**
- Lifecycle states: `PLANNING` → `CONFIRMED` → `COMPLETED` → `CANCELLED`
- Midpoint calculation and caching (`midpointLatitude`, `midpointLongitude`)
- Selected place information from Naver Maps API
- Bidirectional relationship with participants

**Meeting Participants (`MeetingParticipantsEntity`)**
- Invitation status tracking: `PENDING` → `ACCEPTED`/`DECLINED`
- Departure location storage for midpoint calculation
- Composite unique constraint: `(meeting_id, user_id)`

**AI Place Recommendations (`AiPlaceRecommendationsEntity`)**
- Temporary storage of AI server recommendations
- Ranking and scoring system
- Review summaries from AI analysis
- User selection tracking

**Expense Records (`ExpenseRecordsEntity`)**
- Simple per-person expense tracking
- No complex settlement logic (handled in frontend)

### Key Design Patterns

**Entity Auditing**
- All entities use `@EntityListeners(AuditingEntityListener.class)`
- Automatic `createdAt` and `updatedAt` timestamps
- Main application enables with `@EnableJpaAuditing`

**Soft Delete**
- Users and Meetings support `deletedAt` for soft deletion
- Preserves data history and referential integrity

**Cascade Operations**
- Meeting → Participants: `CascadeType.ALL` with `orphanRemoval = true`
- Meeting deletion cascades to participants, recommendations, expenses

**Lazy Loading**
- All entity relationships use `FetchType.LAZY`
- Prevents N+1 query problems

### External System Integration

**AI Recommendation Server**
- Sends midpoint coordinates to AI server
- Receives place recommendations with rankings
- Stores results in `ai_place_recommendations` table
- Review summaries cached to avoid re-requests

**Naver Maps API**
- Place information lookup by `place_id`
- Address and coordinate data
- Used for final place selection

**Kakao Services** (planned)
- OAuth2 authentication
- KakaoTalk invitation links
- Push notifications

## Database Schema

### Important Constraints
- `users.kakao_id` - UNIQUE, indexed for login performance
- `meeting_participants.(meeting_id, user_id)` - UNIQUE to prevent duplicates
- All foreign keys use `ON DELETE CASCADE` for automatic cleanup

### Indexes
- `idx_users_kakao_id`, `idx_users_email` - Login and search
- `idx_meetings_creator_id`, `idx_meetings_status`, `idx_meetings_datetime` - Query optimization
- `idx_participants_meeting_id`, `idx_participants_user_id` - Relationship queries

### Precision Settings
- Coordinates: `DECIMAL(10, 8)` for latitude, `DECIMAL(11, 8)` for longitude
- Money: `DECIMAL(12, 2)` for expense amounts
- Scores: `DECIMAL(5, 2)` for AI scores, `DECIMAL(3, 2)` for ratings

## Configuration

### Environment Variables (Docker)
Set in `compose.yaml`:
- `SPRING_DATASOURCE_URL` - PostgreSQL connection string (jdbc:postgresql://db:5432/ggud_db)
- `SPRING_DATASOURCE_USERNAME/PASSWORD` - DB credentials
- `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` - Schema validation (use Flyway for migrations)
- `SPRING_DATA_REDIS_HOST/PORT` - Redis configuration
- `SPRING_CACHE_TYPE=redis` - Caching backend

### Application Properties
Located at `src/main/resources/application.properties`:
- Currently minimal (only `spring.application.name=GgUd`)
- Most configuration comes from environment variables

## Development Practices

### Entity Development
- Always extend auditing annotations (`@CreatedDate`, `@LastModifiedDate`)
- Use Lombok annotations: `@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
- Add database indexes via `@Table(indexes = {...})`
- Use appropriate precision for numeric types

### Repository Layer
- Extend `JpaRepository<Entity, ID>`
- Follow naming conventions: `findByFieldName`, `existsByFieldName`
- Use `@Query` for complex queries
- Consider query performance with proper indexing

### Service Layer
- Business logic belongs here, not in controllers or repositories
- Transaction management with `@Transactional`
- Handle soft deletes (set `deletedAt` instead of actual delete)

### API Design
- RESTful conventions
- Proper HTTP status codes
- Consider pagination for list endpoints
- Validate input with Bean Validation annotations

## Data Flow

### Meeting Creation Flow
1. User creates meeting via API
2. `MeetingsEntity` created with `status=PLANNING`
3. Creator automatically added to `MeetingParticipantsEntity`
4. Invitation code/link generated for sharing

### Midpoint Calculation Flow
1. All participants submit departure locations
2. Backend calculates average coordinates (midpoint)
3. Sends midpoint to AI server for recommendations
4. AI server returns ranked places with reviews
5. Results stored in `ai_place_recommendations`
6. Participants vote on preferred location
7. Host confirms final selection
8. Meeting status → `CONFIRMED`

### Real-time Location Sharing
1. Meeting status → `IN_PROGRESS` (5 minutes before start)
2. Participants' apps send location updates
3. Stored in `real_time_locations` table
4. Other participants can view on map
5. Automatic cleanup after meeting ends

## Important Notes

- **Soft Delete**: When deleting users or meetings, set `deletedAt` timestamp instead of actual deletion
- **Cascade Delete**: Meeting deletion automatically removes participants, recommendations, and expenses
- **Midpoint Caching**: Once calculated, midpoint is cached in meeting entity to avoid recalculation
- **AI Integration**: AI recommendations are temporary - cleared after meeting confirmation
- **Location Privacy**: Real-time locations only shared during active meeting period

## Reference Documentation

See also:
- `database-schema.md` - Complete database schema with SQL DDL and JPA examples
- `ggud.md` - Detailed workflow guide for API development phases
- `README.md` - Project overview
- `compose.yaml` - Docker service configuration

# GgUd - Meeting Coordination Backend

Spring Boot backend for calculating meeting midpoints and AI place recommendations.

## Quick Start
```bash
# Start all services
docker-compose up -d

# Run application
./gradlew bootRun
```

## Documentation
- **Project Overview**: [docs/project_brief.md](docs/project_brief.md) - Complete project summary
- **Getting Started**: [docs/runbooks/local-dev.md](docs/runbooks/local-dev.md) - Local development setup
- **Current Status**: [docs/_memory/current_state.md](docs/_memory/current_state.md) - Development progress
- **API Docs**: [docs/api/backend-api.md](docs/api/backend-api.md) - REST API specifications
- **Architecture Decisions**: [docs/decisions/adr-index.md](docs/decisions/adr-index.md) - Design decisions
- **Implementation Phases**: [docs/phases/phase-0-overview.md](docs/phases/phase-0-overview.md) - Development roadmap

## Tech Stack
Spring Boot 3.5.7 | PostgreSQL 15 | Redis 7 | Docker Compose

## Core Features
- Meeting midpoint calculation from participant locations
- AI-powered place recommendations
- Real-time location sharing
- Kakao OAuth2 authentication
- Simple expense tracking

## License
[Your License]

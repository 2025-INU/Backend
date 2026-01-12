
# Current Development State

**Last Updated**: 2025-01-12
**Current Phase**: Phase 7 Planning - AWS Deployment & CI/CD
**Active Branch**: main
**Test Coverage**: 22 test files

## Phase 1 Progress: Project Setup ✅
- [x] Project directory structure creation
- [x] Docker Compose environment configuration
- [x] Spring Boot project initialization
- [x] Common components implementation (BaseTimeEntity, GlobalExceptionHandler)

**Key Files**:
- src/main/java/dev/promise4/ggud/common/entity/BaseTimeEntity.java:1-29
- compose.yaml (PostgreSQL 15 + Redis 7)

## Phase 2: Authentication System ✅
- [x] User entity and Repository
- [x] Kakao OAuth2 service
- [x] JWT token management (Access + Refresh)
- [x] Authentication API controller

**Key Files**:
- src/main/java/dev/promise4/ggud/entity/User.java:1-62
- src/main/java/dev/promise4/ggud/security/jwt/JwtTokenProvider.java:1-162
- RefreshToken entity implemented

## Phase 3: Core Meeting Features ✅
- [x] Promise and Participant entities
- [x] Meeting creation API
- [x] Meeting participation API
- [x] Departure location input API
- [x] Meeting query API

**Key Files**:
- src/main/java/dev/promise4/ggud/entity/Promise.java:1-153
- Promise status lifecycle: CREATED → RECRUITING → WAITING_LOCATIONS → SELECTING_MIDPOINT → CONFIRMED → IN_PROGRESS → COMPLETED/CANCELLED
- Participant entity with invitation status tracking

## Phase 4: Midpoint Recommendation ✅
- [x] Subway station data loading
- [x] Midpoint calculation service
- [x] Midpoint recommendation API
- [x] Host-only confirmation API implemented
- [x] Voting system removed (aligned with ADR-0004)

**Key Files**:
- src/main/java/dev/promise4/ggud/service/MidpointService.java (추천 + 확정 통합)
- src/main/java/dev/promise4/ggud/controller/MidpointController.java (호스트 전용 확정)
- src/main/java/dev/promise4/ggud/controller/dto/ConfirmMidpointRequest.java

## Phase 5: Real-time Features ✅
- [x] WebSocket configuration
- [x] WebSocket security configuration
- [x] Real-time location sharing service
- [x] Meeting status real-time notifications

**Key Files**:
- src/main/java/dev/promise4/ggud/config/WebSocketConfig.java
- src/main/java/dev/promise4/ggud/config/WebSocketSecurityConfig.java
- src/main/java/dev/promise4/ggud/config/WebSocketEventListener.java

## Phase 6: Kakao API Integration ✅
- [x] Kakao API client setup
- [x] Kakao Message API (invitation links)
- [x] Kakao Directions API
- [x] KakaoMap integration data

**Key Files**:
- src/main/java/dev/promise4/ggud/service/KakaoMessageService.java

## Phase 7: AWS Deployment & CI/CD (Planning)
- [ ] AWS infrastructure setup (EC2, RDS considerations)
- [ ] Production environment configuration
- [ ] GitHub Actions CI/CD pipeline
- [ ] Monitoring and logging setup (CloudWatch)
- [ ] Security hardening (Fail2Ban, SSL/TLS)
- [ ] Deployment scripts and rollback procedures

**Planning Document**:
- docs/phases/phase-7-deployment.md

**Target Environment**:
- AWS EC2 t2.medium (Ubuntu 22.04)
- PostgreSQL 15 + Redis 7
- Nginx reverse proxy
- GitHub Actions for automated deployment

## Next Steps
1. AWS account setup and IAM configuration
2. EC2 instance provisioning
3. Production configuration files (application-prod.yml)
4. GitHub Actions workflow creation (.github/workflows/)
5. Complete unit test coverage before production deployment
6. Security audit for JWT and OAuth2 implementation

## Blockers
None

## Notes
- Docker Compose configured with PostgreSQL 15 and Redis 7
- Using Spring Boot 3.5.7 with Java 17
- Database schema validation mode set to `validate` (will use Flyway for migrations)

## Quick Links
- **Project Overview**: [../project_brief.md](../project_brief.md)
- **Current Phase Details**: [../phases/phase-7-deployment.md](../phases/phase-7-deployment.md)
- **Phase Roadmap**: [../phases/phase-0-overview.md](../phases/phase-0-overview.md)
- **API Progress**: [../api/backend-api.md](../api/backend-api.md)
- **Deployment Guide**: [../phases/phase-7-deployment.md](../phases/phase-7-deployment.md)

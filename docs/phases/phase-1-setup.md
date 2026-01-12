# Phase 1: 프로젝트 초기 설정

## 목표 (Goals)
- Docker Compose 환경 구성 완료
- Spring Boot 프로젝트 구조 생성
- 공통 컴포넌트 구현 (ApiResponse, BaseTimeEntity, 예외 처리)
- 데이터베이스 스키마 생성

## Step 1.1: 프로젝트 구조 탐색 및 계획

**Claude에게 지시:**
```
프로젝트 구조를 먼저 계획해줘. Spring Boot 3.x + PostgreSQL + Docker Compose 환경으로
약속 관리 앱 백엔드를 만들 거야. 아직 코드는 작성하지 말고, 다음 항목들에 대해 계획만 세워줘:

1. 프로젝트 디렉토리 구조
2. 필요한 Gradle 의존성 목록
3. Docker Compose 서비스 구성
4. application.yml 설정 항목

think hard about this plan
```

## Step 1.2: Docker Compose 환경 구성

**Claude에게 지시:**
```
docker-compose.yml 파일을 작성해줘. 다음 서비스들이 필요해:

1. app: Spring Boot 애플리케이션 (포트 8080)
2. db: PostgreSQL 15 (포트 5432)
3. 볼륨 설정으로 데이터 영속성 보장
4. 환경 변수는 .env 파일에서 로드

Dockerfile도 함께 작성해줘. 멀티스테이지 빌드로 이미지 크기를 최적화해.
```

**예상 결과물:**
- `docker-compose.yml`
- `Dockerfile`
- `.env.example`

**참고 문서**: [../runbooks/local-dev.md](../runbooks/local-dev.md)

## Step 1.3: Spring Boot 프로젝트 초기화

**Claude에게 지시:**
```
Spring Boot 프로젝트 기본 구조를 생성해줘:

1. build.gradle에 필요한 의존성 추가:
   - Spring Web
   - Spring Data JPA
   - Spring Security
   - Spring WebSocket
   - PostgreSQL Driver
   - Lombok
   - Springdoc OpenAPI
   - JWT (jjwt)

2. application.yml 기본 설정:
   - 데이터베이스 연결 설정
   - JPA 설정 (ddl-auto: validate)
   - 로깅 설정
   - 서버 포트 설정

3. 기본 패키지 구조 생성

docker-compose up으로 실행 가능한지 확인해줘.
```

## Step 1.4: 공통 컴포넌트 구현

**Claude에게 지시:**
```
다음 공통 컴포넌트들을 구현해줘:

1. ApiResponse<T> - 통일된 API 응답 형식
2. BaseTimeEntity - 생성/수정 시간 자동 관리
3. GlobalExceptionHandler - 전역 예외 처리
4. 커스텀 예외 클래스들 (NotFoundException, BadRequestException, UnauthorizedException)

각 컴포넌트에 대한 테스트도 작성해줘.
```

## Validation Checklist
- [ ] Docker Compose 서비스가 정상적으로 시작됨
- [ ] 애플리케이션 health check 통과
- [ ] 데이터베이스 연결 성공
- [ ] 공통 컴포넌트 단위 테스트 통과
- [ ] `./gradlew build` 성공
- [ ] `./gradlew test` 모두 통과

## 다음 Phase
[phase-2-auth.md](phase-2-auth.md) - 인증 시스템 구현

## 관련 문서
- **프로젝트 개요**: [../project_brief.md](../project_brief.md)
- **로컬 개발 가이드**: [../runbooks/local-dev.md](../runbooks/local-dev.md)
- **현재 진행 상황**: [../_memory/current_state.md](../_memory/current_state.md)

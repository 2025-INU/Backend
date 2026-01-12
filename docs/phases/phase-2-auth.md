# Phase 2: 인증 시스템 구현

## 목표 (Goals)
- Kakao OAuth2 인증 시스템 구축
- JWT 토큰 기반 인증/인가 구현
- User 엔티티 및 Repository 생성
- 인증 관련 REST API 구현

## Step 2.1: 카카오 OAuth2 설정 계획

**Claude에게 지시:**
```
카카오 OAuth2 로그인 구현 계획을 세워줘. 아직 코드는 작성하지 말고:

1. 카카오 로그인 플로우 다이어그램
2. 필요한 엔드포인트 목록
3. JWT 토큰 관리 전략
4. 사용자 엔티티 설계

think about the security implications
```

## Step 2.2: User 엔티티 및 Repository 구현

**Claude에게 지시:**
```
User 도메인을 구현해줘:

1. User 엔티티:
   - id (Long, PK)
   - kakaoId (String, unique)
   - nickname (String)
   - profileImageUrl (String, nullable)
   - email (String, nullable)
   - role (enum: USER, ADMIN)
   - BaseTimeEntity 상속

2. UserRepository (JpaRepository 상속)
   - findByKakaoId 메서드

3. 테스트 작성 (TDD 방식으로 먼저 테스트 작성)

테스트가 실패하는지 먼저 확인하고, 그 다음 구현해줘.
```

**참고**: [ADR-0001](../decisions/adr-0001-soft-delete.md) - User는 soft delete 지원

## Step 2.3: 카카오 OAuth2 인증 서비스 구현

**Claude에게 지시:**
```
카카오 OAuth2 인증을 위한 서비스를 구현해줘:

1. KakaoOAuthService:
   - getKakaoLoginUrl(): 카카오 로그인 URL 생성
   - processKakaoLogin(code): 인가 코드로 사용자 정보 조회 및 저장
   - 카카오 API 호출 (RestTemplate 또는 WebClient 사용)

2. 필요한 DTO:
   - KakaoTokenResponse
   - KakaoUserInfo

3. 설정 클래스:
   - KakaoOAuthProperties (@ConfigurationProperties)

테스트는 MockServer 또는 WireMock을 사용해서 카카오 API를 모킹해줘.
```

## Step 2.4: JWT 토큰 관리 구현

**Claude에게 지시:**
```
JWT 토큰 관리 시스템을 구현해줘:

1. JwtTokenProvider:
   - createAccessToken(userId): 액세스 토큰 생성 (유효기간: 1시간)
   - createRefreshToken(userId): 리프레시 토큰 생성 (유효기간: 7일)
   - validateToken(token): 토큰 유효성 검증
   - getUserIdFromToken(token): 토큰에서 사용자 ID 추출

2. RefreshToken 엔티티 (Redis 대신 PostgreSQL 사용):
   - id, userId, token, expiryDate

3. JwtAuthenticationFilter:
   - OncePerRequestFilter 상속
   - Authorization 헤더에서 토큰 추출 및 검증

테스트 커버리지 90% 이상 목표로 작성해줘.
```

## Step 2.5: 인증 API 컨트롤러 구현

**Claude에게 지시:**
```
인증 관련 REST API를 구현해줘:

AuthController:
- GET /api/v1/auth/kakao/login-url: 카카오 로그인 URL 반환
- POST /api/v1/auth/kakao/callback: 카카오 로그인 콜백 처리
- POST /api/v1/auth/refresh: 토큰 갱신
- POST /api/v1/auth/logout: 로그아웃

각 엔드포인트에 대한 통합 테스트도 작성해줘.
Swagger 문서화도 추가해줘.
```

**API 상세**: [../api/backend-api.md#authentication-apis](../api/backend-api.md)

## Validation Checklist
- [ ] User 엔티티 및 Repository 테스트 통과
- [ ] 카카오 로그인 URL 생성 성공
- [ ] 카카오 콜백 처리 및 사용자 정보 저장 성공
- [ ] JWT 토큰 생성 및 검증 성공
- [ ] 리프레시 토큰 갱신 성공
- [ ] 인증 API 통합 테스트 통과
- [ ] Swagger UI에서 API 문서 확인 가능
- [ ] 테스트 커버리지 90% 이상

## 다음 Phase
[phase-3-core-features.md](phase-3-core-features.md) - 약속 핵심 기능 구현

## 관련 문서
- **API 명세**: [../api/backend-api.md#authentication-apis](../api/backend-api.md)
- **프로젝트 개요**: [../project_brief.md](../project_brief.md)
- **현재 진행 상황**: [../_memory/current_state.md](../_memory/current_state.md)

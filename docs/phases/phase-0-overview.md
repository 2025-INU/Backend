# 구현 단계 개요

## 단계 요약

### Phase 1: 프로젝트 설정 (2-3일)
**초점**: 인프라 및 기반

Docker Compose 환경, Spring Boot 초기화, 공통 컴포넌트 (ApiResponse, BaseTimeEntity, GlobalExceptionHandler), 데이터베이스 스키마 설정

**결과물**: 실행 가능한 Docker 환경, 빌드 가능한 Spring Boot 프로젝트, 공통 유틸리티

**참조**: [phase-1-setup.md](phase-1-setup.md)

---

### Phase 2: 인증 시스템 (3-4일)
**초점**: 사용자 관리 및 보안

카카오 OAuth2 연동, JWT 토큰 관리, User 엔티티 및 리포지토리, 인증 API 엔드포인트

**결과물**: 작동하는 로그인 플로우, 토큰 갱신, 사용자 관리

**참조**: [phase-2-auth.md](phase-2-auth.md)

---

### Phase 3: 핵심 약속 기능 (5-7일)
**초점**: 약속 라이프사이클 관리

약속 생성, 참여자 관리, 초대 시스템, 출발지 제출, 약속 조회

**결과물**: 약속 전체 CRUD, 초대 플로우, 참여자 추적

**참조**: [phase-3-core-features.md](phase-3-core-features.md)

---

### Phase 4: 중간지점 추천 시스템 (3-4일)
**초점**: 위치 인텔리전스

지하철역 데이터 로딩, 중간지점 계산 (하버사인 공식), 장소 추천을 위한 AI 서버 연동, 호스트 확정 API

**결과물**: 작동하는 중간지점 계산, AI 추천 연동, 장소 확정

**참조**: [phase-4-midpoint.md](phase-4-midpoint.md)

---

### Phase 5: 실시간 기능 (4-5일)
**초점**: 실시간 커뮤니케이션

WebSocket + STOMP 설정, 실시간 위치 공유, 약속 상태 알림, 참여자 접속 상태 추적

**결과물**: 실시간 위치 업데이트, 상태 변경 알림, WebSocket 인프라

**참조**: [phase-5-realtime.md](phase-5-realtime.md)

---

### Phase 6: 카카오 API 연동 (3-4일)
**초점**: 외부 서비스 연동

카카오 메시지 API (초대 링크), 카카오 길찾기 API (내비게이션), 프론트엔드용 카카오맵 데이터 포맷팅

**결과물**: 카카오톡 초대, 경로 안내, 지도 연동 엔드포인트

**참조**: [phase-6-kakao.md](phase-6-kakao.md)

---

### Phase 7: AWS 배포 및 CI/CD (5-7일)
**초점**: 프로덕션 배포 및 자동화

AWS EC2 인스턴스 배포, GitHub Actions CI/CD 파이프라인, 모니터링 및 로깅, 보안 강화

**결과물**: 프로덕션 환경 구축, 자동화된 배포 파이프라인, 모니터링 시스템

**참조**: [phase-7-deployment.md](phase-7-deployment.md)

---

## 단계 의존성

```
Phase 1 (설정)
    ↓
Phase 2 (인증) ← 모든 후속 단계에 필요
    ↓
Phase 3 (핵심 기능) ← 약속 작업의 기반
    ↓
Phase 4 (중간지점) ← Phase 3에 의존 (약속, 참여자)
    ↓
Phase 5 (실시간) ← Phase 3에 의존 (약속 상태)
    ↓
Phase 6 (카카오 API) ← Phase 4-5와 부분적으로 병렬 가능
    ↓
Phase 7 (배포) ← 모든 기능 완성 후 진행
```

**주요 경로**: 1 → 2 → 3 → 4 → 7
**병렬 옵션**: Phase 3 완료 후 Phase 5와 6을 병렬로 개발 가능

## 예상 일정

- **전체 기간**: 25-34일 (5-7주)
- **MVP (Phases 1-4)**: 13-18일 (2.5-3.5주)
- **전체 기능 (Phases 1-6)**: 20-27일 (4-5.5주)
- **프로덕션 배포 포함 (Phases 1-7)**: 25-34일 (5-7주)

## 현재 상태

현재 단계 진행상황 및 상세 작업 추적은 [../_memory/current_state.md](../_memory/current_state.md)를 참조하세요.

## 관련 문서

- **프로젝트 개요**: [../project_brief.md](../project_brief.md)
- **API 명세서**: [../api/backend-api.md](../api/backend-api.md)
- **아키텍처 결정사항**: [../decisions/adr-index.md](../decisions/adr-index.md)

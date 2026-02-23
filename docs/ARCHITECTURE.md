# Trobl Backend — 상세 아키텍처

---

## 목차

1. [전체 레이어 구조](#1-전체-레이어-구조)
2. [도메인 설계](#2-도메인-설계)
3. [인증 & 보안](#3-인증--보안)
4. [캐싱 전략](#4-캐싱-전략)
5. [알림 시스템](#5-알림-시스템)
6. [파일 스토리지](#6-파일-스토리지)
7. [환경 분리 전략](#7-환경-분리-전략)
8. [설계 의도 & 고려사항](#8-설계-의도--고려사항)

---

## 1. 전체 레이어 구조

### 계층형 아키텍처 (Layered Architecture)

```
┌───────────────────────────────────┐
│          Presentation Layer        │
│   (Controller, DTO)               │
│   - REST API 엔드포인트 노출       │
│   - 요청/응답 변환                 │
├───────────────────────────────────┤
│           Service Layer            │
│   (Service, ServiceImpl)          │
│   - 비즈니스 로직 처리             │
│   - 트랜잭션 관리                  │
│   - 외부 서비스 연동               │
├───────────────────────────────────┤
│         Repository Layer           │
│   (JPA Repository, Redis)         │
│   - 데이터 접근 추상화             │
│   - 커스텀 JPQL/Native Query       │
├───────────────────────────────────┤
│           Domain Layer             │
│   (Entity, Enum, Exception)       │
│   - 핵심 비즈니스 모델             │
│   - 도메인 규칙                    │
└───────────────────────────────────┘
```

### 공통 응답 형식

모든 API는 `Message` 객체로 통일된 응답 형식을 반환합니다.

```json
{
  "success": true,
  "data": { ... },
  "message": null
}
```

---

## 2. 도메인 설계

### 핵심 엔티티 관계

```
User (사용자)
  ├── RefreshToken (1:1) — JWT 갱신 토큰
  ├── Partner (1:1) — 커플 연결
  ├── Posting (1:N) — 게시물
  │     ├── Comment (1:N)
  │     ├── PostLike (1:N)
  │     ├── PostView (1:N)
  │     ├── PostBookmark (1:N)
  │     ├── TagMapping (1:N)
  │     └── Poll (1:1)
  │           └── PollOption (1:N)
  ├── Notification (1:N) — 알림
  ├── PushAlarmToken (1:N) — FCM 디바이스 토큰
  └── Report (1:N) — 신고
```

### User Entity 주요 필드

```java
// 기본 정보
String username;        // 로그인 ID (이메일)
String password;        // BCrypt 암호화 비밀번호
String nickname;        // 표시 이름
String address;         // 암호화된 주소 정보

// OAuth 연동
String oAuthProvider;   // "google" | "kakao" | "apple"
String oAuthId;         // OAuth 제공자 ID

// 파트너 (커플) 기능
boolean married;
Long partnerId;
LocalDate weddingAnniversaryDate;

// 보안
int failedLoginAttempts;
boolean accountLocked;
LocalDateTime lockExpiredAt;

// 알림 설정
Set<NotificationType> blockedNotificationTypes;
```

### Posting Entity — 4가지 타입

| PostingType | 설명 | 특이사항 |
|-------------|------|----------|
| `GENERAL` | 일반 게시물 | 일반적인 텍스트 게시물 |
| `POLL` | 투표 게시물 | Poll 엔티티가 1:1로 연결 |
| `FAIR_VIEW` | 공정뷰 | 파트너 양쪽이 모두 작성해야 공개됨 (`confirmed` 필드) |
| `ANNOUNCEMENT` | 공지사항 | 관리자만 작성 가능 |

### FairView 동작 흐름

```
[파트너 A] 게시물 작성 (confirmed=false)
      │
      ▼
[파트너 B] 같은 주제로 게시물 작성
      │
      ▼
[파트너 A] PUT /postings/{postId}/confirm 요청
      │
      ▼
confirmed=true → 모든 사용자에게 공개
```

---

## 3. 인증 & 보안

### JWT 토큰 전략

```
로그인 성공
    │
    ├──▶ Access Token (단기, 헤더로 전달)
    └──▶ Refresh Token (장기, DB 저장)

API 요청
    │
    ▼
JwtAuthenticationFilter
    │
    ├── 토큰 유효 → SecurityContext에 사용자 정보 저장
    └── 토큰 만료 → 401 Unauthorized

Access Token 만료 시
    │
    ▼
POST /auth/refresh (Refresh Token 전달)
    │
    ├── 유효 → 새 Access Token 발급
    └── 무효 → 재로그인 필요
```

### OAuth2 소셜 로그인 흐름

```
Client → GET /oauth/{provider}/login
              │
              ▼
         OAuth Provider (Google/Kakao/Apple)
              │ Authorization Code
              ▼
         OAuthController
              │ 사용자 정보 조회
              ▼
         신규 사용자? ──Yes──▶ 자동 회원가입
              │
             No
              │
              ▼
         JWT 발급 → Client
```

### Spring Security 설정

```
공개 엔드포인트 (인증 불필요)
  GET  /**              — 모든 조회
  /**  /auth/**         — 로그인/회원가입
  /**  /oauth/**        — 소셜 로그인
  /**  /announcements/**— 공지사항
  GET  /ads             — 광고 목록

인증 필요
  POST, PUT, DELETE, PATCH /**
```

### 보안 레이어

| 계층 | 구현 |
|------|------|
| 전송 암호화 | HTTPS (외부 Reverse Proxy) |
| 인증 | JWT Bearer Token |
| 권한 | `@EnableMethodSecurity` + `@PreAuthorize` |
| 비밀번호 | BCryptPasswordEncoder |
| 민감 데이터 | AES-256 대칭 암호화 |
| MFA | TOTP (Time-based One-Time Password) |
| 로그인 실패 제한 | 연속 실패 시 계정 잠금 |

---

## 4. 캐싱 전략

### Redis 캐시 적용 대상

| 캐시 키 | 내용 | 무효화 시점 |
|---------|------|-------------|
| `top-posts` | Top 10 인기 게시물 | 게시물 생성/수정/삭제/좋아요/신고 |
| `post:{id}` | 개별 게시물 상세 | 게시물 수정/삭제/좋아요/공유/조회 |
| `user-thumbnail:{id}` | 사용자 프로필 이미지 URL | 프로필 이미지 변경 시 |
| `main-layout` | 메인 화면 레이아웃 | 관리자 레이아웃 변경 시 |

### 캐시 무효화 전략

```java
// 게시물 수정 시 관련 캐시 일괄 삭제
postingService.evictAllTopPosts();
cacheService.invalidatePostRelatedCache(postId);
```

- **Write-Through** 대신 **Cache Aside** 패턴 사용
- 게시물 변경 이벤트 발생 시 즉시 캐시 삭제 후 다음 요청에서 DB 재조회

---

## 5. 알림 시스템

### 알림 발송 흐름

```
이벤트 발생 (좋아요, 댓글, 파트너 이벤트 등)
    │
    ▼
NotificationService.send*()
    │
    ├── DB에 Notification 레코드 저장
    │
    └── PushNotificationService
              │
              ▼
         Firebase FCM
              │
              ▼
         사용자 디바이스 (iOS/Android/Web)
```

### 알림 배치 처리

- 대량 알림(예: 공지사항)은 `NotificationBatchService`로 비동기 처리
- `@Async` + `AsyncConfig` (ThreadPoolTaskExecutor) 활용

### 사용자별 알림 설정

```java
// User 엔티티에서 차단된 알림 타입 관리
Set<NotificationType> blockedNotificationTypes;

// 알림 발송 전 사용자 설정 확인
if (!user.getBlockedNotificationTypes().contains(notificationType)) {
    // 알림 발송
}
```

### 알림 타입 (NotificationType 열거형)

- `LIKE` — 좋아요
- `COMMENT` — 댓글
- `PARTNER_REQUEST` — 파트너 요청
- `PARTNER_ACCEPT` — 파트너 수락
- `ANNOUNCEMENT` — 공지사항
- (기타 서비스 확장에 따른 타입 추가)

---

## 6. 파일 스토리지

### Google Cloud Storage 연동

```
Client → POST /users/profile-image (multipart)
              │
              ▼
         GoogleCloudStorageService
              │
              ├── 파일 업로드 → GCS Bucket
              └── Public URL 생성 → DB 저장

URL 접근
  CDN / GCS Public URL → 클라이언트 직접 접근
```

### dev / prod 환경 분리

```properties
# dev 환경
gcs.bucket-name=trobl-dev-bucket

# prod 환경
gcs.bucket-name=trobl-prod-bucket
```

- 개발/운영 GCS 버킷을 별도로 분리하여 운영 데이터 보호

---

## 7. 환경 분리 전략

### Gradle 빌드 시 환경별 설정 복사

```groovy
tasks.register('copyProperties') {
    def env = project.hasProperty('env') ? project.property('env') : 'local'
    doLast {
        copy {
            from "src/main/resources/application-${env}.properties"
            into layout.buildDirectory.dir('resources/main')
            rename { 'application.properties' }
        }
    }
}
```

빌드 시 `-Penv=prod` 파라미터로 환경을 지정하면 해당 설정 파일이 `application.properties`로 복사됩니다.

### 환경별 주요 차이

| 설정 | local | dev | prod |
|------|-------|-----|------|
| DB URL | localhost | dev DB | prod DB |
| Redis | localhost | dev Redis | prod Redis |
| GCS Bucket | dev bucket | dev bucket | prod bucket |
| OAuth Redirect | localhost:3000 | dev domain | prod domain |
| JPA DDL | update | update | validate |

---

## 8. 설계 의도 & 고려사항

### Service Interface 분리

주요 서비스는 Interface + Impl 패턴을 적용합니다.

```java
PostingService          // 인터페이스
PostingServiceImpl      // 구현체
```

- 테스트 시 Mock 교체 용이
- 구현체 변경 시 클라이언트 코드 변경 최소화

### N+1 문제 해결

초기 Eager 로딩으로 인한 N+1 문제를 Lazy 로딩 + JPQL fetch join으로 전환했습니다.

```java
// 변경 전: Eager → N+1 쿼리 발생
@OneToMany(fetch = FetchType.EAGER)

// 변경 후: Lazy + fetch join
@OneToMany(fetch = FetchType.LAZY)
// Repository에서 @Query 로 필요한 경우에만 fetch join
```

### 검색 성능 최적화

키워드 검색은 DB LIKE 쿼리 대신 성능 최적화된 쿼리를 적용하였으며, 필요 시 인덱스 활용을 고려했습니다.

### 다국어 지원

```
resources/
├── translations_ko_KR.properties   # 한국어
├── translations_en_US.properties   # 영어
└── translations_ja_JP.properties   # 일본어
```

알림 메시지, 에러 메시지 등을 다국어로 제공합니다.

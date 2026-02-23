# Trobl Backend — 개발 과정 & 트러블슈팅

---

## 목차

1. [개발 타임라인](#1-개발-타임라인)
2. [트러블슈팅 기록](#2-트러블슈팅-기록)
3. [주요 기술적 결정](#3-주요-기술적-결정)
4. [커밋 히스토리 요약](#4-커밋-히스토리-요약)

---

## 1. 개발 타임라인

### Phase 1 — 프로젝트 기반 구축 (2025.05.10 ~ 05.19)

**목표:** 인증 시스템과 핵심 엔티티 설계

| 날짜 | 작업 |
|------|------|
| 05.10 | 프로젝트 초기화 (Spring Boot 3.4.4, Gradle) |
| 05.11 | 프로젝트 설정 완료 |
| 05.12 | User 엔티티 설계, JWT 토큰 구현, Refresh Token 관리, 회원가입/로그인 API |
| 05.17 | 게시물(Posting) API, 댓글(Comment) API, 푸시 알림(FCM) 연동, Redis 캐시 도입, 알림 시스템 |
| 05.19 | Docker 설정, AOP 기반 권한 처리 |

**주요 결정:**
- JWT Access Token + Refresh Token 이중 구조로 보안성과 편의성 균형
- JPA Hibernate 기반 ORM으로 빠른 개발 속도 확보

---

### Phase 2 — 핵심 기능 확장 (2025.05.22 ~ 06.09)

**목표:** 게시물 타입 다양화, 소셜 기능 추가

| 날짜 | 주요 기능 |
|------|-----------|
| 05.22 | FairView 게시물 타입 추가 |
| 05.23 | 댓글 버그 수정, 페이지네이션, 북마크 API |
| 05.24 | 투표 수 누락 버그 수정, 북마크 기능 완성 |
| 05.26 | 사용자 프로필, 닉네임 수정, 태그 추가, 투표한 게시물 목록 |
| 06.02 | Redis 기반 Top 10 게시물 캐시 |
| 06.04 | FairView 상세 API, Confirm 기능 |
| 06.08 | **N+1 쿼리 문제 해결** (Eager → Lazy 로딩) |
| 06.09 | CORS 설정 완료, 검색 최적화 |

---

### Phase 3 — OAuth2 & 외부 연동 (2025.06.13 ~ 07.12)

**목표:** 소셜 로그인, 파일 스토리지, 신고 시스템

| 날짜 | 주요 기능 |
|------|-----------|
| 06.13~15 | 각종 버그 수정 (좋아요, 조회수, 댓글) |
| 06.25 | 광고(Advertisement) 시스템, 회원탈퇴 기능 |
| 06.27 | Google, Apple OAuth2 연동, 복수 선택 투표 추가 |
| 07.03 | CDN(GCS) 연결 |
| 07.04 | Kakao OAuth2 연동, 사용자 정보 확장 |
| 07.07~08 | 신고(Report) 시스템 (게시물, 사용자) |
| 07.11~12 | OAuth 동작 오류 수정, 성능 코드 개선 |

---

### Phase 4 — 알림 & 파트너 시스템 (2025.07.18 ~ 07.31)

**목표:** 알림 시스템 완성, 파트너 기능, 관리자 기능 기반

| 날짜 | 주요 기능 |
|------|-----------|
| 07.18 | 태그, 닉네임, 사용자명 버그 수정 |
| 07.20 | 구조 리팩토링 |
| 07.23 | 알림(Notification) 시스템 완성 |
| 07.24 | 스케줄러 추가, 파트너 삭제 기능 |
| 07.25 | FairView 목록 API, 배치 테스트, 닉네임 제한 해제 |
| 07.26 | CDN 서비스 분리, FCM 토큰 중복 저장 방지, 검색 성능 개선 |
| 07.28 | GCS dev/prod 환경 분리, 공지사항 API |
| 07.27 | 관리자(Admin) 기능 기반 구축 |
| 07.30 | 프로필 이미지 캐시 처리, 결혼 기념일 오류 수정 |
| 07.31 | 프로필 이미지 관리자 연동, 캐시 무효화 개선 |

---

### Phase 5 — 관리자 & 콘텐츠 관리 (2025.08.01 ~ 08.30)

**목표:** 관리자 도구 완성, 욕설 필터, 그룹/레이아웃

| 날짜 | 주요 기능 |
|------|-----------|
| 08.01 | 전반적 버그 수정 |
| 08.03 | 게시물 내용 업데이트 버그 수정 |
| 08.04 | Redis 캐시 버그 수정 |
| 08.06~07 | **욕설 필터(Profanity)** 기능 추가, 신고 후 게시물 숨김 처리 |
| 08.09 | 관리자 댓글 관리, 공지사항 기능 |
| 08.10 | 댓글 신고 사용자 필터링 |
| 08.14 | 게시물 태그 분석 기능 |
| 08.15 | 관리자 태그 관리 & 게시물 목록 |
| 08.16 | 사용자 검색 버그 수정 |
| 08.23~30 | 그룹(Group) 기능, 레이아웃(Layout) 시스템 |

---

### Phase 6 — 대시보드 & 광고 (2025.09.11 ~ 10.15)

**목표:** 관리자 대시보드 완성, 광고 시스템, 태그 마이그레이션

| 날짜 | 주요 기능 |
|------|-----------|
| 09.11 | AI 사용량 조회 기능 |
| 09.15 | AI 사용량 트래킹 |
| 09.23 | 대시보드 차트 추가, API 쿼리 개선 |
| 09.29 | 태그 마이그레이션, 태그 예외 처리 수정 |
| 09.30 | 키워드 기반 태그 연동 |
| 10.03 | **광고(Ad) 시스템** 완성 |
| 10.05 | 관리자 배너 기능 |
| 10.11 | 배너 버그 수정 |
| 10.15 | 광고 버그 수정 |

---

## 2. 트러블슈팅 기록

### [TRS-001] N+1 쿼리 문제 — 2025.06.08

**문제 상황**

게시물 목록 조회 API에서 게시물 수가 늘어날수록 응답 시간이 선형으로 증가했습니다.
게시물 10개 조회 시 쿼리가 10×(연관 엔티티 수)만큼 발생하는 N+1 문제였습니다.

**원인**

```java
// 문제: Eager 로딩으로 즉시 모든 연관 엔티티 조회
@OneToMany(fetch = FetchType.EAGER)
private List<Comment> comments;

@OneToMany(fetch = FetchType.EAGER)
private List<PostLike> likes;
```

게시물 목록 1번 쿼리 + 각 게시물별 Comments N번 + Likes N번 = **2N+1 쿼리**

**해결**

```java
// Lazy 로딩으로 변경
@OneToMany(fetch = FetchType.LAZY)
private List<Comment> comments;

// 필요한 경우에만 JPQL fetch join으로 명시적 조회
@Query("SELECT p FROM Posting p LEFT JOIN FETCH p.likes WHERE p.id = :id")
Optional<Posting> findByIdWithLikes(@Param("id") Long id);
```

**결과:** 목록 조회 쿼리 수 N+1 → 1~2회로 감소

---

### [TRS-002] JWT 토큰 파싱 에러 처리 미흡 — 2025.07.26

**문제 상황**

토큰이 만료되거나 잘못된 형식일 때 서버에서 500 Internal Server Error가 반환되었습니다.
클라이언트는 어떤 오류인지 알 수 없어 디버깅이 어려웠습니다.

**원인**

JWT 파싱 예외(`JwtException`)가 글로벌 예외 핸들러에서 처리되지 않고 스택 트레이스가 그대로 반환됨.

**해결**

```java
// CustomAuthenticationEntryPoint에서 401 응답 처리
@Override
public void commence(HttpServletRequest request, HttpServletResponse response,
        AuthenticationException authException) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write("{\"success\":false,\"message\":\"TOKEN_PARSE_ERROR\"}");
}
```

**결과:** 토큰 오류 시 명확한 401 + 에러 코드 반환

---

### [TRS-003] FCM 토큰 중복 저장 — 2025.07.26

**문제 상황**

같은 디바이스에서 앱을 재설치하거나 재로그인할 때 FCM 토큰이 중복 저장되어, 동일 사용자에게 알림이 중복 발송되었습니다.

**원인**

FCM 토큰 저장 시 기존 토큰 존재 여부를 확인하지 않고 무조건 INSERT.

**해결**

```java
// 저장 전 기존 토큰 조회 후 중복 체크
public void saveFcmToken(Long userId, String token) {
    boolean exists = pushAlarmTokenRepository
        .existsByUserIdAndToken(userId, token);
    if (!exists) {
        pushAlarmTokenRepository.save(new PushAlarmToken(userId, token));
    }
}
```

**결과:** 알림 중복 발송 문제 해소

---

### [TRS-004] 프로필 이미지 캐시 미스로 인한 GCS 과호출 — 2025.07.30

**문제 상황**

프로필 이미지 URL이 매 요청마다 GCS에서 조회되어 불필요한 외부 API 호출이 발생했습니다.
특히 인기 게시물의 작성자 프로필이 집중적으로 요청될 때 GCS 비용이 증가할 우려가 있었습니다.

**해결**

Redis에 `user-thumbnail:{userId}` 키로 프로필 이미지 URL 캐시 적용.
이미지 변경 시 해당 캐시 키를 명시적으로 삭제(evict).

```java
@CacheEvict(value = "user-thumbnail", key = "#userId")
public void updateProfileImage(Long userId, String imageUrl) {
    // 이미지 업데이트 로직
}
```

**결과:** GCS 호출 빈도 대폭 감소, 응답 속도 개선

---

### [TRS-005] FairView Confirm 버그 — 2025.07.25

**문제 상황**

FairView 게시물 수정(PUT) 시 `confirmed` 상태가 의도치 않게 변경되는 버그가 발생했습니다.

**원인**

게시물 수정 로직에서 `confirmed` 필드를 항상 `false`로 초기화하고 있었음.

**해결**

수정 요청 DTO에서 `confirmed` 필드를 제외하고, 별도의 `/confirm` 엔드포인트를 통해서만 상태를 변경하도록 분리.

```
PATCH /postings/{postId}/confirm  → confirmed 상태만 변경
PUT   /postings/{postId}          → 내용만 수정 (confirmed 유지)
```

---

### [TRS-006] GCS URL 공개 접근 불가 — 2025.07.29

**문제 상황**

업로드된 파일 URL로 접근 시 403 Forbidden이 반환됐습니다.

**원인**

GCS 버킷의 기본 액세스 설정이 Private(비공개)이었고, 업로드 시 Public Read 권한을 설정하지 않았습니다.

**해결**

파일 업로드 시 명시적으로 Public Read ACL 설정 추가:

```java
BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
    .setAcl(List.of(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER))) // Public 설정
    .setContentType(contentType)
    .build();
storage.create(blobInfo, bytes);
```

---

### [TRS-007] 게시물 삭제 후 캐시 미삭제 — 2025.07.31

**문제 상황**

게시물을 삭제해도 Redis 캐시에 남아있어 삭제된 게시물이 Top 10 목록에 계속 노출되었습니다.

**해결**

게시물 삭제 시 관련 캐시를 일괄 무효화하는 `invalidatePostRelatedCache()` 메서드 추가:

```java
@DeleteMapping("/{postId}")
public ResponseEntity<Message> deletePost(...) {
    boolean response = postingService.deletePost(user.getId(), postId);
    contentUpdateService.deleteItem(postId, ItemType.POST);
    cacheService.invalidatePostRelatedCache(postId);  // 캐시 무효화 추가
    return ResponseEntity.ok(Message.success(response));
}
```

---

### [TRS-008] OAuth 소셜 로그인 동작 안 됨 — 2025.07.12

**문제 상황**

Google, Kakao OAuth 로그인 후 콜백 처리에서 사용자 정보를 가져오지 못하는 오류가 발생했습니다.

**원인**

각 OAuth 제공자마다 응답 JSON 구조가 달랐는데, 하드코딩된 필드명으로 파싱하다 보니 일부 제공자에서 실패.

**해결**

OAuth 제공자별로 별도의 파싱 로직을 구분:

```java
// Google: "sub" 필드로 ID 추출
// Kakao: "id" 필드로 ID 추출
// Apple: JWT 디코딩 후 "sub" 필드 추출

switch (provider) {
    case "google" -> googleOAuthService.processLogin(oAuthInfo);
    case "kakao"  -> kakaoOAuthService.processLogin(oAuthInfo);
    case "apple"  -> appleOAuthService.processLogin(oAuthInfo);
}
```

---

### [TRS-009] 검색 성능 저하 — 2025.07.26

**문제 상황**

게시물 키워드 검색 시 전체 테이블을 스캔하여 데이터가 늘어날수록 응답 시간이 급격히 증가했습니다.

**해결**

- `LIKE` 양방향 와일드카드 (`%keyword%`) 대신 제목 우선 검색으로 쿼리 최적화
- 신고된 게시물 필터링을 서브쿼리 대신 조인 방식으로 변경
- 페이지네이션 사이즈 기본값 조정

---

### [TRS-010] 배너 수정 버그 — 2025.10.11

**문제 상황**

관리자 배너 수정 API에서 배너 이미지가 업데이트되지 않는 버그가 발생했습니다.

**원인**

배너 엔티티 업데이트 메서드에서 이미지 URL 필드를 setter로 갱신하지 않고 생성자 로직을 재사용한 것이 원인이었습니다.

**해결**

배너 엔티티에 명시적인 `updateImage(String url)` 메서드를 추가하여 수정 시 해당 메서드 호출로 통일.

---

## 3. 주요 기술적 결정

### 결정 1: JWT Stateless vs. Session 기반

**고려사항:** 모바일(iOS, Android) + 웹 클라이언트를 동시에 지원해야 하므로 서버 세션 유지가 부담됨.

**결정:** JWT Stateless 방식 채택
- Access Token: 짧은 만료 시간, 메모리에서 관리
- Refresh Token: DB 저장으로 강제 만료/로그아웃 가능

---

### 결정 2: Redis 캐시 도입 시점

**고려사항:** 초기부터 Redis를 도입하면 복잡도가 증가하지만, 나중에 추가하면 캐시 전략을 설계하기 어려움.

**결정:** 프로젝트 중반(06.02)에 Top 10 게시물 캐시를 시작으로 도입.
주요 인기 콘텐츠 위주로 선택적 캐싱을 적용하여 복잡도 최소화.

---

### 결정 3: FairView 독자적 콘텐츠 타입

**고려사항:** 커플 서비스의 차별화 포인트가 필요했고, 파트너 양쪽의 의견을 공정하게 보여주는 UX가 필요했음.

**결정:** `PostingType.FAIR_VIEW` 타입과 `confirmed` 필드를 도입해 파트너 양쪽이 각자 작성하고 확인 후 공개되는 플로우 설계.

---

### 결정 4: 환경별 설정 분리 전략

**고려사항:** dev/prod 설정 혼용으로 인한 데이터 오염 방지, CI/CD 파이프라인에서의 환경 변수 관리.

**결정:** Gradle `-Penv` 파라미터로 빌드 시점에 환경별 설정을 분리. Docker 컨테이너 실행 시에는 환경 변수로 민감 정보 주입.

---

## 4. 커밋 히스토리 요약

```
2025.05  ████████░░░░░░░░░░░░  기반 구축 (인증, 게시물, 댓글, 알림)
2025.06  ████████████░░░░░░░░  기능 확장 (FairView, 북마크, OAuth, 광고)
2025.07  ████████████████░░░░  외부 연동 (GCS, FCM, Kakao, Apple OAuth)
2025.08  ████████████████████  관리자 도구 (욕설 필터, 신고, 레이아웃)
2025.09  ████████████████░░░░  대시보드 (차트, AI 사용량, 태그 마이그레이션)
2025.10  ████████░░░░░░░░░░░░  광고 & 배너 완성, 버그 수정
```

### 커밋 유형별 분포

| 유형 | 비율 | 내용 |
|------|------|------|
| `[feature]` | ~45% | 새 기능 추가 |
| `[bugfix]` | ~50% | 버그 수정 |
| `[refactor]` | ~3% | 코드 구조 개선 |
| `[init]` | ~2% | 초기 설정 |

> 버그 수정 비율이 높은 것은 빠른 MVP 개발 후 사용자 피드백 기반으로 안정화하는 개발 방식을 선택했기 때문입니다.

### 주요 기능별 첫 커밋 날짜

| 기능 | 최초 도입 |
|------|-----------|
| JWT 인증 | 2025.05.12 |
| 게시물/댓글 | 2025.05.17 |
| Redis 캐시 | 2025.05.17 |
| Firebase FCM | 2025.05.17 |
| 북마크 | 2025.05.23 |
| Top 10 캐시 | 2025.06.02 |
| FairView | 2025.06.04 |
| 광고 시스템 | 2025.06.25 |
| Google/Apple OAuth | 2025.06.27 |
| Kakao OAuth | 2025.07.04 |
| GCS 파일 스토리지 | 2025.07.04 |
| 신고 시스템 | 2025.07.07 |
| 알림 시스템 | 2025.07.23 |
| 파트너 연동 | 2025.07.24 |
| 관리자 도구 | 2025.07.27 |
| 욕설 필터 | 2025.08.06 |
| 레이아웃 시스템 | 2025.08.23 |
| 대시보드 차트 | 2025.09.23 |
| 태그 마이그레이션 | 2025.09.29 |
| 배너 시스템 | 2025.10.05 |

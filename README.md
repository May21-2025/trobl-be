# Trobl — 커플 소셜 커뮤니티 플랫폼 Backend

> 커플을 위한 투표·토론·커뮤니티 서비스 **Trobl**의 Spring Boot 기반 백엔드 API 서버입니다.

---

## 목차

- [프로젝트 소개](#프로젝트-소개)
- [기술 스택](#기술-스택)
- [시스템 아키텍처](#시스템-아키텍처)
- [패키지 구조](#패키지-구조)
- [주요 기능](#주요-기능)
- [API 개요](#api-개요)
- [문서](#문서)

---

## 프로젝트 소개

**Trobl**은 커플이 일상의 다양한 주제에 대해 의견을 나누고, 투표하며, 소통하는 소셜 커뮤니티 플랫폼입니다.
단순한 게시판을 넘어 **FairView(공정한 시각)**, **Poll(투표)**, **Partner 연동** 등의 독자적인 기능을 통해 커플만의 특별한 경험을 제공합니다.

| 항목 | 내용                                     |
|------|----------------------------------------|
| **개발 기간** | 2025.05 ~ 2025.10 (약 6개월)              |
| **개발 인원** | 사이드 팀 프로젝트                             |
| **서버 포트** | 8080                                   |
| **GitHub** | https://github.com/May21-2025/trobl-be |

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.4.4 |
| **Build** | Gradle |
| **Database** | PostgreSQL + Spring Data JPA (Hibernate) |
| **Cache** | Redis (Spring Data Redis) |
| **Security** | Spring Security, JWT (jjwt 0.12.6), OAuth2 Client |
| **Push Notification** | Firebase Admin SDK 9.4.3 (FCM) |
| **Storage** | Google Cloud Storage (Spring Cloud GCP 6.1.1) |
| **MFA** | TOTP (dev.samstevens.totp 1.7.1) |
| **Profanity Filter** | Apache Commons Text 1.13.1 |
| **Monitoring** | Spring Boot Actuator |
| **Containerization** | Docker (eclipse-temurin:21-jre-jammy) |

---

## 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│                        Client                           │
│              (Web / iOS / Android)                      │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTPS
┌──────────────────────▼──────────────────────────────────┐
│              Spring Boot API Server (8080)               │
│                                                         │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │  Controller  │  │   Service    │  │  Repository   │  │
│  │  (REST API) │─▶│ (비즈니스 로직) │─▶│  (JPA/Redis) │  │
│  └─────────────┘  └──────────────┘  └───────────────┘  │
│                                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │              Spring Security Layer               │   │
│  │   JWT Filter → Authentication → Authorization   │   │
│  └─────────────────────────────────────────────────┘   │
└──────┬──────────────┬───────────────┬───────────────────┘
       │              │               │
┌──────▼───┐  ┌───────▼────┐  ┌──────▼──────────┐
│PostgreSQL│  │   Redis    │  │  External APIs  │
│  (Main   │  │  (Cache)   │  │ Firebase / GCS  │
│   DB)    │  │            │  │ Google / Kakao  │
└──────────┘  └────────────┘  │ Apple OAuth     │
                               └─────────────────┘
```

---

## 패키지 구조

```
src/main/java/com/may21/trobl/
├── _global/                    # 전역 공통 모듈
│   ├── aop/                    # AOP (로깅, 권한 등)
│   ├── config/                 # 설정 클래스 (Security, Redis, Firebase 등)
│   ├── enums/                  # 열거형 (PostingType, NotificationType 등)
│   ├── exception/              # 커스텀 예외 & 글로벌 예외 핸들러
│   ├── security/               # JWT 유틸리티, AES 암호화
│   └── utility/                # 공통 유틸
│
├── admin/                      # 관리자 기능
├── advertisement/              # 광고 관리
├── auth/                       # 인증 (JWT 발급/검증, 로그인/회원가입)
├── bookmark/                   # 북마크
├── comment/                    # 댓글
├── notification/               # 알림 (FCM 배치, 설정)
├── oAuth/                      # OAuth2 (Google, Kakao, Apple)
├── partner/                    # 파트너(커플) 연동
├── poll/                       # 투표
├── post/                       # 게시물 (핵심 도메인)
├── pushAlarm/                  # FCM 토큰 관리
├── recordLimit/                # 요청 횟수 제한
├── redis/                      # Redis 캐시 서비스
├── report/                     # 신고
├── scheduler/                  # 스케줄러 (주기적 작업)
├── storage/                    # Google Cloud Storage 연동
├── tag/                        # 태그
└── user/                       # 사용자 관리
```

> 상세 아키텍처는 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)를 참고하세요.

---

## 주요 기능

### 인증 & 사용자 관리
- **JWT 기반 인증** — Access Token + Refresh Token 이중 토큰 전략
- **OAuth2 소셜 로그인** — Google, Kakao, Apple 지원
- **TOTP MFA** — 2단계 인증으로 계정 보안 강화
- **AES 암호화** — 민감한 사용자 정보 암호화 저장
- **파트너(커플) 연동** — 두 사용자를 파트너로 연결, 기념일 관리

### 콘텐츠
- **게시물 (Posting)** — 4가지 타입 지원
  - `GENERAL` : 일반 게시물
  - `POLL` : 투표 게시물
  - `FAIR_VIEW` : 파트너가 각자 의견을 작성 후 공개되는 공정뷰
  - `ANNOUNCEMENT` : 공지사항
- **댓글 (Comment)** — 게시물 당 댓글 CRUD
- **투표 (Poll)** — 단일/복수 선택, 퀵 폴 기능
- **좋아요 / 북마크 / 공유** — 소셜 반응 추적

### 알림
- **Firebase FCM** — 좋아요, 댓글, 파트너 이벤트 등에 대한 푸시 알림
- **알림 배치 처리** — 비동기 배치로 대량 알림 처리
- **알림 수신 설정** — 사용자별 알림 타입 ON/OFF

### 검색 & 탐색
- **키워드 검색** — 게시물 제목·내용 풀텍스트 검색
- **태그 시스템** — 게시물 태그 관리 & 마이그레이션
- **Top 10 게시물** — Redis 캐시 기반 인기 게시물 목록
- **메인 레이아웃** — 관리자가 설정하는 홈 화면 구성

### 관리자
- **대시보드** — 사용자·게시물·AI 사용량 차트 및 통계
- **배너 관리** — 홈 화면 배너 CRUD
- **광고 관리** — 광고 등록, 클릭 수 집계
- **공지사항 관리** — 전체 공지 발행
- **욕설 필터** — Apache Commons Text 기반 부적절 단어 차단
- **태그 마이그레이션** — 기존 태그 데이터 일괄 정리

### 인프라 & 성능
- **Redis 캐싱** — 인기 게시물, 프로필 이미지, 레이아웃 캐시
- **Google Cloud Storage** — 프로필 이미지·첨부파일 저장
- **다국어 지원** — 한국어, 영어, 일본어
- **신고 시스템** — 게시물·댓글·사용자 신고 및 숨김 처리

---

## API 개요

| 도메인 | Base URL | 주요 기능 |
|--------|----------|-----------|
| 인증 | `/auth` | 회원가입, 로그인, 토큰 갱신 |
| OAuth | `/oauth` | Google / Kakao / Apple 소셜 로그인 |
| 게시물 | `/postings` | CRUD, 좋아요, 북마크, 공유, 검색 |
| 댓글 | `/comments` | CRUD |
| 투표 | `/polls` | 투표 참여, 결과 조회 |
| FairView | `/postings/fair-view` | 공정뷰 목록, 작성 확인 |
| 사용자 | `/users` | 프로필, 내 게시물, 알림 설정 |
| 파트너 | `/partner` | 파트너 연결/해제 |
| 태그 | `/tags` | 태그 검색, 관리 |
| 알림 | `/notifications` | 알림 목록, 읽음 처리 |
| 광고 | `/ads` | 광고 조회, 클릭 집계 |
| 관리자 | `/admin` | 사용자·게시물 관리, 배너, 공지 |
| 대시보드 | `/admin/dashboard` | 통계 차트 |
| 공지사항 | `/announcements` | 공지 목록 (인증 불필요) |

---

## 문서

| 문서 | 설명 |
|------|------|
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | 상세 아키텍처, 엔티티 설계, 보안 흐름 |
| [TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) | 개발 과정, 주요 트러블슈팅 & 해결 과정 |

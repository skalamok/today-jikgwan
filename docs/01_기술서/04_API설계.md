# API 설계

| 항목 | 내용 |
|---|---|
| 프로젝트명 | 오늘의직관 — KBO 직관 기록 서비스 |
| 작성자 | 울산 U133 이채목 |
| 최종 수정일 | 2026-08-19 |
| 원본 | `docs/03_API명세/openapi.yaml` (OpenAPI 3.0.3) |
| 버전 | 원본을 따른다. 개정 이력은 `openapi.yaml` 의 info.description 에 있다 |

아래 목록은 `openapi.yaml` 에서 생성한다. 전체 요청 · 응답 스키마는 원본에 있고,
Swagger Editor 에 붙여넣으면 화면으로 확인할 수 있다.

---

## 공통 규칙

| 항목 | 내용 |
|---|---|
| 기준 경로 | `/api/v1` |
| 인증 | `Authorization: Bearer {accessToken}` (JWT). 공개 엔드포인트만 예외 |
| 형식 | 요청 · 응답 모두 JSON (UTF-8). 사진 업로드만 `multipart/form-data` |
| 오류 | `{ code, message }` 형태로 통일한다. `code` 는 화면이 분기에 쓰는 값이고 `message` 는 사람이 읽는 문구다 |
| 목록 | `page` · `size` 로 나눠 주고 응답에 `totalElements` 를 담는다 |

## 주요 요청 · 응답 예시

값 자체가 설계 판단인 세 곳만 싣는다. 나머지는 원본을 본다.

### 1) 직관 기록 작성 — 같은 경기를 두 번 기록할 때

`POST /attendance-logs` (REQ-F-201)

```
요청
{ "gameId": 512, "cheerTeamId": 2, "stadiumZoneId": 14,
  "zoneRating": 4, "memo": "3연승 직관", "visibility": "PRIVATE" }

409 Conflict
{ "code": "DUPLICATE_ATTENDANCE_LOG",
  "message": "이미 이 경기 기록이 있어요.",
  "data": { "existingLogId": 1024 } }
```

애플리케이션 검사만 두면 같은 사람이 동시에 두 번 눌렀을 때 뚫린다.
`attendance_logs (user_id, game_id)` 유일 제약으로 막고 그 위반을 409 로 옮긴다.
`data.existingLogId` 를 함께 주어 화면이 기존 기록으로 보낼 수 있게 한다.

### 2) 내 전적 — 표본이 모자란 항목

`GET /stats/me/summary` · `GET /stats/me?dimension=STADIUM` (REQ-F-305)

```
GET /stats/me/summary  →  200 OK
{ "games": 8, "wins": 5, "draws": 0, "losses": 3, "winRate": 0.625,
  "smallSample": false, "neutralCount": 1,
  "currentStreak": -1, "longestWinStreak": 5, "totalCost": 237000 }

GET /stats/me?dimension=STADIUM  →  200 OK
[ { "key": "1", "label": "잠실야구장",
    "games": 5, "wins": 4, "draws": 0, "losses": 1,
    "winRate": 0.800, "smallSample": false },
  { "key": "7", "label": "대구삼성라이온즈파크",
    "games": 1, "wins": 0, "draws": 0, "losses": 1,
    "winRate": null, "smallSample": true } ]
```

통산은 1경기부터 승률을 준다. 구장 · 상대팀 · 요일처럼 나눠 놓은 집계는 5경기 미만이면
`winRate` 를 `null` 로 비우고 `smallSample: true` 를 세운다. 0 이나 0.667 을 주면 화면이
그것을 순위처럼 늘어놓게 된다. 값을 비워 화면이 판단하지 않게 하고, 왜 비었는지는
플래그로 알린다. `neutralCount` 는 중립 관람이라 승패 집계에서 빠진 경기 수다.

### 3) 메이트 신청 — 정원이 찬 순간

`POST /companion-posts/{postId}/applications` (REQ-F-503, 504)

```
201 Created
{ "seq": 3, "confirmedCount": 3, "capacity": 4 }

409 Conflict
{ "code": "COMPANION_POST_FULL", "message": "방금 정원이 찼어요." }
```

여러 명이 같은 순간에 눌러도 `seq` 가 겹치거나 비지 않는다. 모집글의 `version` 으로
낙관적 잠금을 걸어 충돌하면 다시 시도하고, 그 사이 정원이 차면 409 로 돌려보낸다.
마지막 방어선은 `confirmed_count <= capacity` CHECK 제약이다. 대기자 개념은 두지 않았다.

---

## Auth

인증 (REQ-F-001 ~ 004, REQ-F-009)

| 메서드 | 경로 | 설명 | 범위 | 인증 | 관련 요구사항 | 비고 |
|---|---|---|---|---|---|---|
| POST | `/auth/signup` | 이메일 회원가입 | MVP | 공개 | REQ-F-001 |  |
| POST | `/auth/login` | 이메일 로그인 | MVP | 공개 | REQ-F-002 |  |
| GET | `/auth/oauth/providers` | 사용 가능한 소셜 로그인 제공자 조회 | 확장 | 공개 | REQ-F-003 |  |
| GET | `/auth/oauth/{provider}/authorize-url` | 소셜 로그인 인가 URL 발급 | 확장 | 공개 | REQ-F-003 |  |
| POST | `/auth/oauth/{provider}/callback` | 소셜 로그인 콜백 (인가 코드 교환) | 확장 | 공개 | REQ-F-003 |  |
| POST | `/auth/verify-email` | 이메일 소유 확인 | MVP | 공개 | REQ-F-001 |  |
| POST | `/auth/password-reset` | 비밀번호 재설정 메일 발송 | 확장 | 공개 | REQ-F-004 | 미구현 |

## User

회원 정보 (REQ-F-005 ~ 008)

| 메서드 | 경로 | 설명 | 범위 | 인증 | 관련 요구사항 | 비고 |
|---|---|---|---|---|---|---|
| GET | `/users/me` | 내 정보 조회 | 확장 | 인증 | REQ-F-006 |  |
| PATCH | `/users/me` | 내 정보 수정 | MVP | 인증 | REQ-F-005, REQ-F-006 |  |
| GET | `/users/me/badges` | 내 배지 목록 | 확장 | 인증 | REQ-F-703 |  |
| GET | `/teams` | 구단 목록 | MVP | 공개 | REQ-F-005 |  |

## Game

경기 정보 (REQ-F-101 ~ 107, REQ-F-112 ~ 113)

| 메서드 | 경로 | 설명 | 범위 | 인증 | 관련 요구사항 | 비고 |
|---|---|---|---|---|---|---|
| GET | `/games` | 경기 일정 및 결과 조회 | MVP | 공개 | REQ-F-101, REQ-F-102 |  |
| GET | `/games/{gameId}` | 경기 상세 조회 | MVP | 공개 | REQ-F-103 |  |
| POST | `/games/suggest` | 촬영 일시 기반 경기 후보 조회 | 확장 | 인증 | REQ-F-204 |  |
| GET | `/games/{gameId}/weather` | 경기 구장 날씨 조회 | 확장 | 공개 | REQ-F-112 |  |
| POST | `/games/weather/sync` | 구장 날씨 수동 동기화 | 확장 | 인증 | REQ-F-112 |  |
| GET | `/standings` | 팀 순위 조회 | 확장 | 공개 | REQ-F-104 |  |

## Stadium

구장 및 구역별 만족도 (REQ-F-108 ~ 111, REQ-F-114)

| 메서드 | 경로 | 설명 | 범위 | 인증 | 관련 요구사항 | 비고 |
|---|---|---|---|---|---|---|
| GET | `/stadiums` | 구장 목록 조회 | MVP | 공개 | REQ-F-108 |  |
| GET | `/stadiums/{stadiumId}` | 구장 상세 조회 | MVP | 공개 | REQ-F-109, REQ-F-110 |  |
| GET | `/stadiums/{stadiumId}/zones/{zoneId}/reviews` | 구역별 후기 조회 | 확장 | 공개 | REQ-F-111 |  |

## AttendanceLog

직관 기록 (REQ-F-201 ~ 216)

| 메서드 | 경로 | 설명 | 범위 | 인증 | 관련 요구사항 | 비고 |
|---|---|---|---|---|---|---|
| GET | `/attendance-logs` | 내 직관 기록 목록 | MVP | 인증 | REQ-F-213 |  |
| POST | `/attendance-logs` | 직관 기록 작성 | MVP | 인증 | REQ-F-201, REQ-F-309 |  |
| GET | `/attendance-logs/{logId}` | 기록 상세 조회 | MVP | 인증 | REQ-F-214, REQ-NF-008 |  |
| PUT | `/attendance-logs/{logId}` | 기록 수정 | MVP | 인증 | REQ-F-212 |  |
| DELETE | `/attendance-logs/{logId}` | 기록 삭제 | MVP | 인증 | REQ-F-212 |  |
| POST | `/attendance-logs/{logId}/photos` | 사진 업로드 | MVP | 인증 | REQ-F-203, REQ-NF-007, REQ-NF-002 |  |
| DELETE | `/attendance-logs/{logId}/photos/{photoId}` | 사진 삭제 | 확장 | 인증 | REQ-F-209 |  |

## Stat

전적 집계 (REQ-F-301 ~ 310)

| 메서드 | 경로 | 설명 | 범위 | 인증 | 관련 요구사항 | 비고 |
|---|---|---|---|---|---|---|
| GET | `/stats/me/summary` | 내 전적 요약 | MVP | 인증 | REQ-F-301, REQ-F-305, REQ-F-306 |  |
| GET | `/stats/me` | 차원별 전적 조회 | MVP | 인증 | REQ-F-302, REQ-F-307 |  |

## Companion

직관 메이트 모집 (REQ-F-501 ~ 507)

| 메서드 | 경로 | 설명 | 범위 | 인증 | 관련 요구사항 | 비고 |
|---|---|---|---|---|---|---|
| GET | `/companion-posts` | 메이트 모집글 목록 | 확장 | 인증 | REQ-F-502 |  |
| POST | `/companion-posts` | 메이트 모집글 등록 | 확장 | 인증 | REQ-F-501 |  |
| GET | `/companion-posts/{postId}` | 메이트 모집글 상세 | 확장 | 인증 | REQ-F-502 |  |
| POST | `/companion-posts/{postId}/applications` | 메이트 참여 신청 (선착순 확정) | 확장 | 인증 | REQ-F-503, REQ-F-504, REQ-F-505 |  |
| DELETE | `/companion-posts/{postId}/applications` | 메이트 참여 취소 | 확장 | 인증 | REQ-F-506 |  |

## CompanionChat

메이트 소통 (REQ-F-510 ~ 513)

| 메서드 | 경로 | 설명 | 범위 | 인증 | 관련 요구사항 | 비고 |
|---|---|---|---|---|---|---|
| GET | `/companion-posts/{postId}/comments` | 모집글 문의 댓글 조회 | 확장 | 인증 | REQ-F-510 |  |
| POST | `/companion-posts/{postId}/comments` | 모집글 문의 댓글 작성 | 확장 | 인증 | REQ-F-510 |  |
| GET | `/companion-posts/{postId}/messages` | 메이트 대화 조회 | 확장 | 인증 | REQ-F-511 |  |
| POST | `/companion-posts/{postId}/messages` | 메이트 대화 전송 | 확장 | 인증 | REQ-F-511 |  |
| PUT | `/companion-posts/{postId}/messages/read` | 대화 읽음 처리 | 확장 | 인증 | REQ-F-511 |  |

## Safety

신고 · 차단 (REQ-F-508, REQ-F-509)

| 메서드 | 경로 | 설명 | 범위 | 인증 | 관련 요구사항 | 비고 |
|---|---|---|---|---|---|---|
| POST | `/reports` | 신고 | 확장 | 인증 | REQ-F-508, REQ-NF-023 |  |
| GET | `/users/me/blocks` | 차단 목록 조회 | 확장 | 인증 | REQ-F-509 |  |
| POST | `/users/me/blocks` | 사용자 차단 | 확장 | 인증 | REQ-F-509 |  |
| DELETE | `/users/me/blocks/{userId}` | 차단 해제 | 확장 | 인증 | REQ-F-509 |  |

## ViewingPlan

관람 계획 (REQ-F-401 ~ 404)

| 메서드 | 경로 | 설명 | 범위 | 인증 | 관련 요구사항 | 비고 |
|---|---|---|---|---|---|---|
| POST | `/viewing-plans` | 관람 계획 생성 | 확장 | 인증 | REQ-F-401, REQ-F-402 |  |
| POST | `/viewing-plans/{planId}/generate` | 후보 일정 자동 편성 | 확장 | 인증 | REQ-F-403 |  |

## Notification

알림 (REQ-F-706 ~ 707)

| 메서드 | 경로 | 설명 | 범위 | 인증 | 관련 요구사항 | 비고 |
|---|---|---|---|---|---|---|
| GET | `/notifications` | 알림 목록 | 확장 | 인증 | REQ-F-706 |  |
| PUT | `/notifications/read` | 알림 전체 읽음 처리 | 확장 | 인증 | REQ-F-707 |  |

## Admin

운영자 기능 (REQ-F-601 ~ 606)

| 메서드 | 경로 | 설명 | 범위 | 인증 | 관련 요구사항 | 비고 |
|---|---|---|---|---|---|---|
| POST | `/admin/games` | 경기 등록 | MVP | 인증 | REQ-F-601, REQ-NF-009 |  |
| POST | `/admin/games/sync` | 경기 데이터 동기화 | MVP | 인증 | REQ-F-107, REQ-NF-015 |  |
| GET | `/admin/games/unconfirmed` | 결과 미등록 경기 조회 | MVP | 인증 | REQ-F-606, REQ-NF-015 |  |
| POST | `/admin/games/{gameId}/revisions` | 경기 결과 정정 | MVP | 인증 | REQ-F-602, REQ-F-603, REQ-F-309 |  |
| GET | `/admin/games/{gameId}/revisions` | 경기 정정 이력 조회 | MVP | 인증 | REQ-F-603 |  |
| PATCH | `/admin/stadiums/{stadiumId}` | 구장 정보 수정 | MVP | 인증 | REQ-F-605 |  |
| GET | `/admin/stadiums/{stadiumId}/zones` | 좌석 구역 목록 (운영자) | MVP | 인증 | REQ-F-605 |  |
| POST | `/admin/stadiums/{stadiumId}/zones` | 좌석 구역 추가 | MVP | 인증 | REQ-F-605 |  |
| PATCH | `/admin/zones/{zoneId}` | 좌석 구역 수정 · 비활성화 | MVP | 인증 | REQ-F-605 |  |
| DELETE | `/admin/zones/{zoneId}` | 좌석 구역 삭제 | MVP | 인증 | REQ-F-605 |  |

---

총 45개 경로 · 57개 오퍼레이션. 인증이 필요한 요청은 `Authorization: Bearer {accessToken}` 헤더를 요구한다.

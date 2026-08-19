# 부록 B. API 엔드포인트 요약

| 항목 | 내용 |
|---|---|
| 프로젝트명 | 오늘의직관 — KBO 직관 기록 서비스 |
| 작성자 | 이채목 |
| 최종 수정일 | 2026-08-19 |
| 원본 | `docs/03_API명세/openapi.yaml` (OpenAPI 3.0.3) |
| 버전 | 원본을 따른다. 개정 이력은 `openapi.yaml` 의 info.description 에 있다 |

`openapi.yaml` 에서 자동 생성한 목록이다. 상세 요청 · 응답 스키마는 원본을 참조한다.
Swagger Editor 에 원본을 붙여넣으면 같은 내용을 화면으로 확인할 수 있다.

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
| GET | `/games/{gameId}` | 경기 상세 조회 | MVP | 공개 | REQ-F-103 | 미구현 |
| POST | `/games/suggest` | 촬영 일시 기반 경기 후보 조회 | 확장 | 인증 | REQ-F-204 | 미구현 |
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
| PATCH | `/attendance-logs/{logId}` | 기록 수정 | MVP | 인증 | REQ-F-212 | 미구현 |
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
| PUT | `/companion-posts/{postId}/messages/read` | 대화 읽음 처리 | 확장 | 인증 |  |  |

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
| GET | `/admin/games/unconfirmed` | 결과 미등록 경기 조회 | MVP | 인증 | REQ-F-606, REQ-NF-015 |  |
| POST | `/admin/games/{gameId}/revisions` | 경기 결과 정정 | MVP | 인증 | REQ-F-602, REQ-F-603, REQ-F-309 |  |
| GET | `/admin/games/{gameId}/revisions` | 경기 정정 이력 조회 | MVP | 인증 | REQ-F-603 |  |
| PATCH | `/admin/stadiums/{stadiumId}` | 구장 정보 수정 | MVP | 인증 | REQ-F-605 |  |
| GET | `/admin/stadiums/{stadiumId}/zones` | 좌석 구역 목록 (운영자) | MVP | 인증 | REQ-F-605 |  |
| POST | `/admin/stadiums/{stadiumId}/zones` | 좌석 구역 추가 | MVP | 인증 | REQ-F-605 |  |
| PATCH | `/admin/zones/{zoneId}` | 좌석 구역 수정 · 비활성화 | MVP | 인증 | REQ-F-605 |  |
| DELETE | `/admin/zones/{zoneId}` | 좌석 구역 삭제 | MVP | 인증 | REQ-F-605 |  |

---

총 43개 경로 · 55개 오퍼레이션. 인증이 필요한 요청은 `Authorization: Bearer {accessToken}` 헤더를 요구한다.

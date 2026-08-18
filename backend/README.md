# 오늘의직관 Backend (today-jikgwan)

Spring Boot 4.1 · Java 21 · PostgreSQL 18 · Flyway · JWT

## 실행

```bash
# 1. DB 기동 (프로젝트 루트에서)
docker-compose up -d

# 2. 애플리케이션 실행
./gradlew bootRun
```

- 애플리케이션: http://localhost:8080
- DB: localhost:5433 (todayjikgwan / todayjikgwan / todayjikgwan)

스키마와 시드 데이터는 Flyway가 부팅 시 자동 적용한다.

| 마이그레이션 | 내용 |
|---|---|
| `V1__init_schema.sql` | 테이블 24개 |
| `V2__seed_teams_stadiums.sql` | 구단 10 · 구장 9 · 구역 63 · 배지 6 |
| `V3__seed_games_2026.sql` | 2026 시즌 720경기 (합성 시드) |
| `V4__weather_and_channels.sql` | 구장 격자 좌표, 날씨 캐시, 구단 채널 컬럼 |

## 시즌 일정 재생성

```bash
python3 tools/generate_schedule.py
```

외부 API 무료 티어가 시즌 일정을 15건으로 제한하므로(`docs/03_API명세/외부API_실측결과.md`)
개발·시연용 일정을 KBO 실제 구조(10팀 · 팀당 144경기 · 월요일 휴식 · 하루 5경기)로 합성한다.
실제 경기 데이터가 아니며, 유료 구독 확보 시 `source = EXTERNAL` 로 대체한다.

## 구현된 API

| Method | Path | 요구사항 | 인증 |
|---|---|---|---|
| POST | `/api/v1/auth/signup` | REQ-F-001 | - |
| POST | `/api/v1/auth/login` | REQ-F-002 | - |
| GET | `/api/v1/games?date=` | REQ-F-101, 102 | - |
| POST | `/api/v1/attendance-logs` | REQ-F-201~211, 606 | 필요 |
| GET | `/api/v1/attendance-logs` | REQ-F-213 | 필요 |
| GET | `/api/v1/stats/me/summary` | REQ-F-301, 305, 306 | 필요 |
| GET | `/api/v1/stats/me?dimension=` | REQ-F-302~304 | 필요 |

## 설계 메모

### 소표본 표시 정책 (REQ-F-305)
표본이 `todayjikgwan.stat.small-sample-threshold`(기본 5) 미만이면 `winRate` 를 `null` 로 내리고
`smallSample=true` 를 함께 보낸다. 2경기 2승을 승률 1.000 으로 표시하면 오해를 유발하기 때문이다.

### 전적 재계산 (REQ-F-309)
`StatService.recalculate()` 는 **전체 재계산** 방식이다. 개인 관람 기록은 시즌당 수십 건 수준이라
증분 반영의 이점보다 정합성이 중요하고, 경기 결과 정정처럼 과거 데이터가 바뀌는 경우도
같은 경로로 처리할 수 있다. 데이터가 커지면 증분 방식으로 전환한다.

### 중립 관람 (REQ-F-202)
`is_neutral` 플래그를 두지 않고 `cheer_team_id` 를 nullable 로 둔다.
플래그와 팀 ID를 함께 두면 "중립인데 팀이 지정된" 모순 상태가 만들어질 수 있다.

### 응원팀 변경 (REQ-F-005)
관람 기록은 작성 시점의 `cheer_team_id` 를 보관한다.
프로필의 응원팀을 바꿔도 과거 기록의 승패 판정이 뒤집히지 않는다.

### 결과 미확정 경기 (REQ-F-606, 607)
결과가 확정되지 않은 경기에 기록을 작성하려면 스코어 제보가 필수다.
동일 스코어 제보가 `todayjikgwan.game-report.confirm-threshold`(기본 3)건 이상 일치하면 결과를 확정한다.

## 미구현

동행 모집(REQ-F-501~506), 관람 계획(REQ-F-401~404), 사진 업로드(REQ-F-203),
구장 상세(REQ-F-109~111), 배지(REQ-F-703), 운영자 기능(REQ-F-601~605)

# 개발 현황

> 작업 이어받을 때 이 문서부터 읽는다. 마지막 갱신 2026-08-19 밤

## 제출 일정

| 일자 | 제출물 | 상태 |
|---|---|---|
| 2026-08-19 17:59 | 프로젝트 기술서(.pdf) · DB ERD(.dbml) · API(.yaml) | **제출 완료** (약 17:47) |
| 2026-08-20 09:59 | 발표 자료(.pdf) | 준비 완료, `docs/_제출본/` |

제출 이후 무엇이 바뀌었는지는 `docs/제출_이후_변경.md` 에 있다.

## 실행

```bash
docker-compose up -d                                    # PostgreSQL 18 (5433)
cd backend && JIKGWAN_ADMIN_EMAILS=admin@example.com ./gradlew bootRun   # 8080
cd frontend && npm run dev                              # 5173
```

## 산출물 재생성 — 이 순서로

```bash
python3 tools/build_api_summary.py    # openapi.yaml → 기술서 부록 B
python3 tools/build_pdf.py            # 기술서 PDF
python3 tools/build_slides.py         # 발표 자료 PDF
python3 tools/check_consistency.py    # 정합성 15개 항목 (제출 전 필수)
python3 tools/build_wireframes.py     # 와이어프레임 HTML → PNG (dev 서버 필요)
```

`check_consistency.py` 가 전부 통과해야 제출한다. 눈으로 훑지 말 것.
`.dbml` 과 `.yaml` 은 전용 파서로 검증한다(`tools/validate_specs.js`).
파싱이 깨진 채 제출될 뻔한 적이 세 번 있었다.

## 현재 규모

기능 요구사항 79 · 비기능 24 · 화면 24(상세 7) · 테이블 27 · API 오퍼레이션 57
MVP 33건 / API 27개, 확장 46건 / API 30개

에픽 태스크 49개 — 완료 39 · 진행 3 · 예정 7. **MVP 태스크는 모두 완료다.**
남은 진행 셋은 전부 외부 키가 없어서다. 위 "외부 키를 받으면" 절을 본다.

## 최근 결정 (되돌리지 말 것)

| 결정 | 내용 |
|---|---|
| 경기 결과 확정 | **운영자만.** 사용자 제보 자동 확정을 제거했다 (V8). 스코어는 객관적 사실이라 사용자 입력으로 정할 이유가 없고, 틀리면 그 경기 기록자 전원의 전적이 틀어진다 |
| 승률 표시 | 통산은 1경기부터, 차원별(구장·상대팀·요일)은 5경기부터. 나란히 놓이면 순위처럼 읽히기 때문 |
| 요구사항 ID | `REQ-N` → `REQ-NF`. 마이그레이션 SQL 주석은 Flyway 체크섬 때문에 그대로 둔다 |
| 미사용 테이블 | `auth_tokens`, `viewing_plan_stadiums` 제거 (V9). `user_team_history` 는 REQ-F-005 근거라 유지 |
| 용어 | 동행 → **직관 메이트**. DB·API 는 `companion` 유지 |
| 도메인 | `todayjikgwan.site` (취득 예정). groupId `com.todayjikgwan` 는 유지 |

## 08-19 밤에 한 것

MVP 에 남아 있던 태스크를 모두 닫았다.

| 태스크 | 무엇이 없었나 |
|---|---|
| T-3.4 기록 수정 (REQ-F-212) | 명세와 화면은 있고 구현만 없었다. 구역 통계 이동과 전적 재계산까지 붙였다 |
| T-2.2 데이터 제공자 추상화 (REQ-F-107) | 발표에서 말하는 인터페이스가 없었다. 날씨와 같은 구조로 만들고 실제로 붙여 봤다 |
| T-1.2 응원팀 변경 이력 (REQ-F-005) | 주석은 "남긴다" 인데 남기지 않았다 |
| T-1.1 이메일 소유 확인 (REQ-F-001) | 메일 코드가 아예 없었다. 발송기를 추상화하고 기본은 로그로 둔다 |
| T-3.5 함께 간 사람 (REQ-F-209) | 엔티티만 있고 API 가 없었다 |

곁들여 찾은 것.

- `GET /games/{gameId}` 가 명세에만 있고 구현이 없었다. REQ-F-103 은 MVP 다
- 코드 주석에 없는 요구사항 번호(REQ-F-607 · 608)가 있었다. 607 은 V8 에서 없앤
  제보 확정 기능의 잔재라 죽은 코드까지 함께 지웠다
- 상세 응답에 `stadiumZoneId` · `cheerTeamId` 가 없어 수정 화면이 값을 채울 수 없었다

## 남은 기능

- 티켓 카드 공유 (REQ-F-704) — 기록 1건을 티켓 이미지로 만들어 내려받기
- 함께 간 사람별 · 날씨별 전적 (REQ-F-307 · 310) — 209 를 넣었으니 입력원은 생겼다
- 실시간 대화 수신 (REQ-F-512) — 지금은 새로고침으로 읽는다
- 구단 공식 채널 링크 (REQ-F-114) — `teams` 의 URL 칸이 비어 있다. 데이터만 넣으면 된다
- 모바일 웹 다듬기

## 외부 키를 받으면 여기부터 본다

키가 없어도 서비스는 돈다. 키를 쓰는 기능만 꺼진 채로 있고, 넣으면 바로 켜진다.
**소스에 값을 쓰지 않는다. 전부 환경변수다.**

### 소셜 로그인 (REQ-F-003 · T-1.3)

제공자 콘솔에서 앱을 만들고 **콜백 주소를 똑같이 등록해야 한다.** 이게 어긋나면
인가 단계에서 막힌다.

```
콜백 주소 : http://localhost:5173/oauth/{provider}
             {provider} 자리에 google · naver · kakao 를 그대로 넣는다
```

| 제공자 | 환경변수 | 콘솔 |
|---|---|---|
| 구글 | `JIKGWAN_GOOGLE_CLIENT_ID` · `JIKGWAN_GOOGLE_CLIENT_SECRET` | Google Cloud Console → OAuth 2.0 클라이언트 ID |
| 네이버 | `JIKGWAN_NAVER_CLIENT_ID` · `JIKGWAN_NAVER_CLIENT_SECRET` | 네이버 개발자센터 → 애플리케이션 등록 |
| 카카오 | `JIKGWAN_KAKAO_CLIENT_ID` · `JIKGWAN_KAKAO_CLIENT_SECRET` | 카카오 개발자 → 내 애플리케이션 (REST API 키) |

카카오는 이메일을 주려면 비즈앱 심사가 필요하다. 심사 전에는 이메일이 비어 오므로
계정 식별을 이메일이 아니라 (제공자, 제공자 사용자 ID) 조합으로 한다. 이미 그렇게 짜 두었다.

**넣는 법과 확인**

```bash
cd backend
JIKGWAN_KAKAO_CLIENT_ID=... JIKGWAN_KAKAO_CLIENT_SECRET=... ./gradlew bootRun

# ① 켜진 제공자만 목록에 나온다. 키를 안 넣은 것은 아예 빠진다
curl -s localhost:8080/api/v1/auth/oauth/providers

# ② 인가 URL 이 만들어지는지
curl -s "localhost:8080/api/v1/auth/oauth/kakao/authorize-url"

# ③ 브라우저에서 로그인 화면 → 그 제공자 버튼이 떠 있어야 한다
open http://localhost:5173/login
```

로그인까지 끝나면 `user_social_accounts` 에 행이 생긴다. 지금은 0행이다.

```sql
select provider, provider_user_id, email from user_social_accounts;
```

에픽 표(목표 정의서 7.4)의 T-1.3 을 완료로 바꾼다.

### 구장 날씨 (REQ-F-112 · 113 · T-2.6)

공공데이터포털에서 **기상청 단기예보 조회서비스**와 **기상특보 조회서비스** 두 개를
각각 신청해야 한다. 하나만 승인돼도 날씨는 나오지만 특보 안내는 비어 있다.

```bash
JIKGWAN_KMA_SERVICE_KEY=... ./gradlew bootRun

# 운영자로 로그인해 그날 경기의 예보를 받아 온다
curl -s -X POST -H "Authorization: Bearer $ADMIN_TOKEN" \
     "localhost:8080/api/v1/games/weather/sync?date=2026-08-20"

# 받아 왔는지
curl -s -H "Authorization: Bearer $TOKEN" localhost:8080/api/v1/games/632/weather
```

`game_weather` 에 행이 생기면 된 것이다. 지금은 0행이다.
서버가 하루 네 번(02·08·14·20시) 알아서 받아 오므로 이후에는 손댈 일이 없다.

### 경기 데이터 외부 연동 (REQ-F-107 · T-2.2)

인터페이스는 있고 기본은 꺼져 있다. 켜는 것만으로는 쓸 수 없다는 것을 이미 확인했다.

```bash
JIKGWAN_TSDB_ENABLED=true ./gradlew bootRun
curl -s -X POST -H "Authorization: Bearer $ADMIN_TOKEN" \
     "localhost:8080/api/v1/admin/games/sync?date=2025-08-19"
```

경기는 가져오지만 구단·구장이 `Hanwha Eagles` · `Daejeon Hanbat Baseball Stadium`
처럼 영문이라 전부 걸러진다. 쓰려면 이름 대응표를 만들어 계속 유지해야 한다.
`teams.external_ref` 와 `stadiums` 에 대응 값을 채우는 것이 가장 단순한 길이다.

## 주의사항

- **DB를 새로 만들면 기록 데이터가 사라진다.** 시드는 Flyway가 복원하지만 사용자 기록은 API로 다시 넣어야 한다
- 인증키(기상청·소셜)는 환경변수다. `application.yml` 에 값을 직접 쓰지 않는다
- 적용된 마이그레이션 SQL 은 수정하지 않는다. Flyway 체크섬이 깨져 기동이 실패한다
- `@Transactional` 은 같은 빈 안에서 자기 호출하면 걸리지 않는다
- 화면 ID 를 새로 붙일 때는 화면 설계서 10장을 먼저 본다
- 테이블을 지울 때 `Ref` 줄을 함께 지운다. 세 번 놓쳤다
- 마크다운 강조가 줄을 넘어가면 별표가 인쇄된다 → `build_pdf.py` 가 문단을 이어 붙이도록 고쳤다

## 테스트 계정

`admin@example.com`(운영자) · `fan@example.com`(기록 8건) · `planner@example.com` ·
`chost@example.com` · `cg1@example.com` · `cg2@example.com` — 비밀번호 모두 `todayball123!`

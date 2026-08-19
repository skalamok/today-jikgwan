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

기능 요구사항 79 · 비기능 24 · 화면 24(상세 7) · 테이블 27 · API 오퍼레이션 59
MVP 33건 / API 27개, 확장 46건 / API 32개

에픽 태스크 49개 — 완료 43 · 진행 4 · 예정 2. **MVP 태스크는 모두 완료다.**
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

| T-8.4 직관 카드 (REQ-F-704) | 브라우저에서 티켓 이미지를 그려 내려받는다. 서버는 관여하지 않는다 |
| T-2.7 구단 채널 (REQ-F-114) | API 도 없었다. 홈페이지 열 곳을 직접 호출해 확인하고 V10 으로 넣었다 |
| T-3.6 경기 추천 (REQ-F-204) | 사진 EXIF 를 **서버가 지우기 전에 브라우저에서** 읽는다 |
| T-6.4 실시간 수신 (REQ-F-512) | WebSocket. 토큰을 URL 에 싣지 않고 연결 뒤 첫 프레임으로 인증한다 |
| T-1.4 재설정 · 탈퇴 (REQ-F-004, 007) | 메일 발송기와 서명 토큰을 그대로 쓴다. 탈퇴는 행을 지우지 않는다 |

곁들여 찾은 것.

- `GET /games/{gameId}` 가 명세에만 있고 구현이 없었다. REQ-F-103 은 MVP 다
- 코드 주석에 없는 요구사항 번호(REQ-F-607 · 608)가 있었다. 607 은 V8 에서 없앤
  제보 확정 기능의 잔재라 죽은 코드까지 함께 지웠다
- 상세 응답에 `stadiumZoneId` · `cheerTeamId` 가 없어 수정 화면이 값을 채울 수 없었다
- **기록 수정이 PATCH 인데 실은 전체 치환이었다.** 비용을 안 실어 보냈다가 데이터가
  날아가서야 알았다. PUT 으로 바로잡았다
- `isEmpty()` 를 두었더니 Jackson 이 프로퍼티로 잡아 명세에 없는 `"empty": false` 가 샜다
- WebSocket 메시지가 안 갔다. `jackson-datatype-jsr310` 이 없어 직렬화가 실패했고
  **그 예외를 조용히 삼키고 있었다.** 로그를 남기게 고치고서야 보였다

## 다음에 할 일

**막힌 데 없이 바로 되는 것**

| 태스크 | 무엇을 | 왜 할 만한가 |
|---|---|---|
| REQ-F-106 데이터 기준 시각 표시 (T-2.8 일부) | 외부에서 받아 온 데이터에 마지막 동기화 시각과 출처를 함께 표시. `games.synced_at` 은 이미 채워진다 | 키 없이 된다. 작다 |
| 모바일 웹 다듬기 | 발표 데모가 모바일 화면이면 값어치가 있다 | |
| 모바일 웹 다듬기 | — | 발표 데모가 모바일 화면이면 값어치가 있다 |

**밖에서 값을 받아야 되는 것** → 아래 "밖에서 받아 와야 하는 것" 절

T-1.3 소셜 로그인 · T-2.6 구장 날씨 · T-2.7 구단 예매처 · SNS ·
T-3.7 관람일 날씨 자동 기록 · T-4.5 날씨별 전적

## 다음 세션에서 할 것 — 프런트 디자인을 새로 짠다

**디자인을 새로 짜는 일이다.** 화면 코드를 버리는 것이 아니라, 새 디자인을 정하고
그것을 20개 화면에 입힌다. API 호출과 라우팅은 지금이 맞으니 그대로 둔다.

지금 문제는 **기준이 되는 디자인이 아예 없다**는 것이다. 공통 컴포넌트가 네 개뿐이고
버튼과 카드가 화면마다 따로 적혀 있으며 `#16355c` 같은 색값이 직접 박혀 있다.
그래서 밋밋하고, 화면마다 다르고, 모바일이 덜 다듬어졌다.

**목표는 "실제로 배포한 서비스처럼 보이는 것" 이다.**

### 순서

1. **디자인을 정한다**
   야구장의 밤 · 티켓 · 기록이라는 성격에 맞는 색과 결을 잡는다. 로고의 네이비가
   기준색이다. 정한 것을 CSS 변수로 적어 둔다 — 색 · 간격 · 모서리 · 그림자 · 글자 크기.
   **여기서 정한 것이 곧 디자인이다.** 화면에 값을 직접 적지 않는다.

2. **화면 요소를 다시 그린다** (`src/components/`)
   `Button` · `Card` · `Field` · `Chip` · `Stars` · `EmptyState`.
   지금은 같은 버튼이 화면마다 조금씩 다르게 적혀 있다. 한 벌로 만들어 쓴다.

3. **모바일을 먼저 맞춘다**
   화면 설계서 9장이 브레이크포인트를 900px 로 정해 두었다. 하단 탭 · 안전 영역 ·
   터치 크기(44px)를 먼저 맞추고 데스크톱은 그 위에 얹는다.

4. **데모 다섯 화면부터 입힌다**
   `docs/발표_메모.md` 의 데모 순서가 지나는 화면이다.
   기록 작성 → 전적 → 구장 상세 → 기록 상세(직관 카드) → 홈.
   시간이 모자라면 여기까지만 해도 발표는 된다.

### 건드리지 말 것

- API 호출과 라우팅. 화면이 서버와 주고받는 방식은 지금이 맞다
- 백엔드. 이번 작업은 프런트만이다
- `src/lib/ticketCard.js` 의 카드 도안은 새 디자인이 정해진 뒤에 맞춘다

## 밖에서 받아 와야 하는 것 (여기만 채우면 됨)

코드는 다 되어 있고 값만 없다. 아래 표의 왼쪽을 구해 오면 오른쪽 자리에 넣으면 끝난다.
**하나도 없어도 서비스는 돈다.** 그 기능만 꺼진 채로 있다.

| 필요한 것 | 어디서 | 넣는 자리 | 닫히는 태스크 |
|---|---|---|---|
| 구글 OAuth 클라이언트 ID · 시크릿 | Google Cloud Console | 환경변수 `JIKGWAN_GOOGLE_CLIENT_ID` · `_SECRET` | T-1.3 |
| 네이버 OAuth 클라이언트 ID · 시크릿 | 네이버 개발자센터 | `JIKGWAN_NAVER_CLIENT_ID` · `_SECRET` | T-1.3 |
| 카카오 REST API 키 · 시크릿 | 카카오 개발자 | `JIKGWAN_KAKAO_CLIENT_ID` · `_SECRET` | T-1.3 |
| 기상청 단기예보 서비스키 | 공공데이터포털 | `JIKGWAN_KMA_SERVICE_KEY` | T-2.6 · T-3.7 · T-4.5 |
| 기상청 기상특보 서비스키 | 공공데이터포털 | 위와 같은 키 | T-2.6 |
| **구단 예매처 주소 10개** | 각 구단 홈페이지의 예매 버튼 | 새 마이그레이션 `V11` | T-2.7 |
| **구단 인스타그램 주소 10개** | 각 구단 공식 계정 | 새 마이그레이션 `V11` | T-2.7 |
| **구단 유튜브 주소 10개** | 각 구단 공식 채널 | 새 마이그레이션 `V11` | T-2.7 |

### 구단 링크는 왜 아직 비어 있나

홈페이지 열 곳은 직접 호출해 응답을 확인하고 `V10` 으로 넣었다. 예매처와 SNS 는
계정 주소를 확인할 방법이 마땅치 않아 비웠다. **확인하지 않은 주소를 넣으면 눌렀을 때
엉뚱한 데로 간다.** 링크는 눌러 보기 전에는 맞는지 알 수 없으므로 추측해서 채우지 않는다.

주소를 모으면 아래 모양으로 `V11__team_links.sql` 을 만들어 넣는다.
`V10` 은 이미 적용됐으므로 고치지 않는다. Flyway 체크섬이 깨져 기동이 실패한다.

```sql
-- REQ-F-114 예매처 · SNS. 열 곳 모두 눌러 확인한 주소만 넣는다
UPDATE teams SET ticket_url = '...', instagram_url = '...', youtube_url = '...'
 WHERE short_name = 'LG';
-- 나머지 아홉 구단도 같은 모양으로
```

넣은 뒤 확인은 이렇다.

```bash
curl -s localhost:8080/api/v1/stadiums/1 | python3 -m json.tool   # homeTeams[].channels
open http://localhost:5173/stadiums/1                            # 구단 공식 채널 카드
```

화면은 값이 있는 링크만 그리므로, 셋 중 하나만 채워도 그것만 뜬다.

---

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

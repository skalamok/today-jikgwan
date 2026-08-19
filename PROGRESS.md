# 개발 현황

> 작업 이어받을 때 이 문서부터 읽는다. 마지막 갱신 2026-08-19 15:40

## 제출 일정

| 일자 | 제출물 | 상태 |
|---|---|---|
| 2026-08-19 17:59 | 프로젝트 기술서(.pdf) · DB ERD(.dbml) · API(.yaml) | **준비 완료**, `docs/_제출본/` |
| 2026-08-20 09:59 | 발표 자료(.pdf) | 갱신 완료, 저녁에 재검토 예정 |

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

기능 요구사항 79 · 비기능 24 · 화면 24(상세 7) · 테이블 27 · API 오퍼레이션 55
MVP 32건 / API 25개, 확장 47건 / API 30개

## 최근 결정 (되돌리지 말 것)

| 결정 | 내용 |
|---|---|
| 경기 결과 확정 | **운영자만.** 사용자 제보 자동 확정을 제거했다 (V8). 스코어는 객관적 사실이라 사용자 입력으로 정할 이유가 없고, 틀리면 그 경기 기록자 전원의 전적이 틀어진다 |
| 승률 표시 | 통산은 1경기부터, 차원별(구장·상대팀·요일)은 5경기부터. 나란히 놓이면 순위처럼 읽히기 때문 |
| 요구사항 ID | `REQ-N` → `REQ-NF`. 마이그레이션 SQL 주석은 Flyway 체크섬 때문에 그대로 둔다 |
| 미사용 테이블 | `auth_tokens`, `viewing_plan_stadiums` 제거 (V9). `user_team_history` 는 REQ-F-005 근거라 유지 |
| 용어 | 동행 → **직관 메이트**. DB·API 는 `companion` 유지 |
| 도메인 | `todayjikgwan.site` (취득 예정). groupId `com.todayjikgwan` 는 유지 |

## 진행 중인 작업

사용자 검토를 받아 문서를 다듬는 중이다. 남은 지적 사항:

1. "뺀 기능" → "MVP 제외 기능 / 확장 기능" 으로 용어 변경
2. 로고 형태 재검토 (티켓+실밥이 잘 안 읽힌다는 지적)
3. 0장 규모 줄의 "이 가운데 먼저 만드는" 문구, "(4.3)" 참조 표기
4. 6.1 배포 구조에서 "애플리케이션" 이 왜 나오는지 설명 부족
5. **설계 완료 후 애자일 전환** 내용 추가 (7.1)
6. 일정을 오전/오후/야간 → 실제 시각(HH:MM ~ HH:MM)
7. SP 단위 정의(1시간? 30분?) + 지금까지 한 설계 작업도 에픽·태스크에 넣고 완료 표기 및 색칠

## 남은 기능

- 티켓 카드 공유 (REQ-F-704) — 기록 1건을 티켓 이미지로 만들어 내려받기. 저녁 구현 예정
- 소셜 로그인 실제 키 발급 후 종단 확인 (코드는 완료)
- 모바일 웹 다듬기

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

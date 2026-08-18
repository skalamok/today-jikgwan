# -*- coding: utf-8 -*-
"""
2026 시즌 경기 일정 시드 데이터 생성기

외부 API 무료 티어가 시즌 일정을 15건으로 제한하므로(docs/03_API명세/외부API_실측결과.md),
개발 및 시연용 일정을 KBO 실제 구조에 맞춰 합성한다.

구조
  - 10개 구단, 팀당 144경기 (총 720경기)
  - 서클 방식 라운드로빈 9라운드 = 45경기가 1사이클, 16사이클 = 720경기
  - 사이클마다 홈/원정을 뒤집어 맞대결당 홈 8 / 원정 8로 균형
  - 월요일 휴식, 하루 5경기
  - 평일 18:30 / 토 17:00 / 일 14:00 개시

결과는 실제 경기 데이터가 아닌 합성 데이터이며, 정식 데이터는 유료 구독 시 대체한다.
"""
import datetime as dt
import random

SEASON = 2026
OPENING = dt.date(2026, 3, 24)
TODAY = dt.date(2026, 8, 18)          # 이 날짜 이전 경기에만 결과를 채운다
SEED = 20260818

# TheSportsDB external_ref (docs/03_API명세/외부API_실측결과.md)
TEAMS = ["139819", "139820", "139821", "139822", "139823",
         "139824", "139825", "139826", "139827", "139828"]

CYCLES = 16                            # 16 × 45 = 720경기


def round_robin(teams):
    """서클 방식. n-1 라운드, 라운드마다 n/2 경기."""
    ts = list(teams)
    n = len(ts)
    rounds = []
    for _ in range(n - 1):
        pairs = [(ts[i], ts[n - 1 - i]) for i in range(n // 2)]
        rounds.append(pairs)
        ts = [ts[0]] + [ts[-1]] + ts[1:-1]   # 첫 팀 고정, 나머지 회전
    return rounds


def game_days(start, count):
    """월요일을 건너뛰며 경기일을 생성한다."""
    days, d = [], start
    while len(days) < count:
        if d.weekday() != 0:             # 0=월
            days.append(d)
        d += dt.timedelta(days=1)
    return days


def start_time(d):
    if d.weekday() == 5:   return dt.time(17, 0)   # 토
    if d.weekday() == 6:   return dt.time(14, 0)   # 일
    return dt.time(18, 30)


def main():
    rnd = random.Random(SEED)
    base = round_robin(TEAMS)
    schedule = []
    for cycle in range(CYCLES):
        for rd in base:
            # 사이클마다 홈/원정 반전 → 맞대결당 홈 8 / 원정 8
            schedule.append([(a, b) if cycle % 2 == 0 else (b, a) for a, b in rd])

    days = game_days(OPENING, len(schedule))
    rows = []
    for day, pairs in zip(days, schedule):
        for home, away in pairs:
            ts = dt.datetime.combine(day, start_time(day))
            if day < TODAY:
                hs = rnd.choices(range(0, 14), weights=[6,9,11,12,12,11,9,7,5,4,3,2,1,1])[0]
                as_ = rnd.choices(range(0, 14), weights=[6,9,11,12,12,11,9,7,5,4,3,2,1,1])[0]
                if hs == as_ and rnd.random() > 0.05:      # 무승부는 약 5%만 유지
                    hs += 1
                rows.append((day, ts, home, away, hs, as_, "FINISHED", True))
            else:
                rows.append((day, ts, home, away, None, None, "SCHEDULED", False))

    def lit(v):
        return "NULL" if v is None else str(v)

    out = []
    out.append("-- =====================================================================")
    out.append("-- 2026 시즌 경기 일정 (합성 시드 데이터)")
    out.append("-- 생성: backend/tools/generate_schedule.py")
    out.append("-- 외부 API 무료 티어 제한으로 실제 일정을 확보할 수 없어 구조만 재현한다.")
    out.append("-- 실제 경기 데이터가 아니며, 유료 구독 확보 시 EXTERNAL 소스로 대체한다.")
    out.append(f"-- 총 {len(rows)}경기 / {days[0]} ~ {days[-1]}")
    out.append("-- =====================================================================")
    out.append("")
    out.append("INSERT INTO games (season_year, game_date, start_at, stadium_id, home_team_id,")
    out.append("                   away_team_id, home_score, away_score, status, source,")
    out.append("                   result_confirmed, confirmed_at)")
    out.append("SELECT %d, v.game_date, v.start_at, ht.home_stadium_id, ht.id, at.id," % SEASON)
    out.append("       v.home_score, v.away_score, v.status, 'MANUAL', v.confirmed,")
    out.append("       CASE WHEN v.confirmed THEN v.start_at ELSE NULL END")
    out.append("FROM (VALUES")
    body = []
    for i, (d, ts, h, a, hs, as_, st, conf) in enumerate(rows):
        cast = "" if i else "::date"
        cast2 = "" if i else "::timestamptz"
        cast3 = "" if i else "::int"
        body.append("    (DATE '%s'%s, TIMESTAMPTZ '%s+09'%s, '%s', '%s', %s%s, %s%s, '%s', %s)"
                    % (d, "", ts.strftime("%Y-%m-%d %H:%M:%S"), "", h, a,
                       lit(hs), cast3, lit(as_), cast3, st, "TRUE" if conf else "FALSE"))
    out.append(",\n".join(body))
    out.append(") AS v(game_date, start_at, home_ref, away_ref, home_score, away_score, status, confirmed)")
    out.append("JOIN teams ht ON ht.external_ref = v.home_ref")
    out.append("JOIN teams at ON at.external_ref = v.away_ref;")
    out.append("")

    path = "src/main/resources/db/migration/V3__seed_games_2026.sql"
    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(out))

    # 검증
    from collections import Counter
    home_c, away_c, total_c = Counter(), Counter(), Counter()
    for _, _, h, a, *_ in rows:
        home_c[h] += 1; away_c[a] += 1; total_c[h] += 1; total_c[a] += 1
    print("생성 경기 수:", len(rows))
    print("기간:", days[0], "~", days[-1], "(%d일)" % len(days))
    print("팀당 총경기:", set(total_c.values()))
    print("팀당 홈경기:", set(home_c.values()))
    print("완료 경기:", sum(1 for r in rows if r[6] == "FINISHED"))
    print("예정 경기:", sum(1 for r in rows if r[6] == "SCHEDULED"))
    print("출력:", path)


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""openapi.yaml 에서 엔드포인트 목록을 만들어 기술서 API 설계 장으로 넣는다.

명세를 고치면 이 스크립트를 다시 돌려 표를 갱신한다. 손으로 옮겨 적지 않는 이유는
두 곳이 어긋나기 때문이다.
"""
import io, os, re, yaml

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(BASE, "docs", "03_API명세", "openapi.yaml")
OUT = os.path.join(BASE, "docs", "01_기술서", "04_API설계.md")
METHODS = ("get", "post", "put", "patch", "delete")

spec = yaml.safe_load(io.open(SRC, encoding="utf-8"))
tags = {t["name"]: t.get("description", "") for t in spec.get("tags", [])}

# 태그별로 모은다. 명세의 태그 선언 순서를 그대로 따른다.
grouped = {name: [] for name in tags}
for path, item in spec["paths"].items():
    for method, op in item.items():
        if method not in METHODS:
            continue
        tag = (op.get("tags") or ["기타"])[0]
        grouped.setdefault(tag, []).append((method.upper(), path, op))

def auth_mark(op):
    return "공개" if op.get("security") == [] else "인증"

def status_mark(op):
    return "미구현" if "**현재 미구현.**" in (op.get("description") or "") else ""

def req_ids(op):
    """미구현 안내 블록(> 로 시작하는 인용문)의 ID 는 그 기능의 요구사항이 아니므로 뺀다."""
    desc = op.get("description") or ""
    desc = "\n".join(l for l in desc.splitlines() if not l.lstrip().startswith(">"))
    ids = re.findall(r"REQ-[A-Z]+-\d{3}", desc)
    seen = []
    for i in ids:
        if i not in seen:
            seen.append(i)
    return ", ".join(seen[:3])

lines = [
    "# API 설계", "",
    "| 항목 | 내용 |", "|---|---|",
    "| 프로젝트명 | 오늘의직관 — KBO 직관 기록 서비스 |",
    "| 작성자 | 울산 U133 이채목 |",
    "| 최종 수정일 | 2026-08-19 |",
    "| 원본 | `docs/03_API명세/openapi.yaml` (OpenAPI %s) |" % spec["openapi"],
    "| 버전 | 원본을 따른다. 개정 이력은 `openapi.yaml` 의 info.description 에 있다 |", "",
    "아래 목록은 `openapi.yaml` 에서 생성한다. 전체 요청 · 응답 스키마는 원본에 있고,",
    "Swagger Editor 에 붙여넣으면 화면으로 확인할 수 있다.", "",
    "---", "",
    # 55개 요청·응답을 여기 다 옮기면 원본과 이중 관리가 되고 문서만 길어진다.
    # 대신 값 자체가 설계 판단인 세 곳만 예시로 싣는다.
    "## 공통 규칙", "",
    "| 항목 | 내용 |", "|---|---|",
    "| 기준 경로 | `/api/v1` |",
    "| 인증 | `Authorization: Bearer {accessToken}` (JWT). 공개 엔드포인트만 예외 |",
    "| 형식 | 요청 · 응답 모두 JSON (UTF-8). 사진 업로드만 `multipart/form-data` |",
    "| 오류 | `{ code, message }` 형태로 통일한다. `code` 는 화면이 분기에 쓰는 값이고 `message` 는 사람이 읽는 문구다 |",
    "| 목록 | `page` · `size` 로 나눠 주고 응답에 `totalElements` 를 담는다 |", "",
    "## 주요 요청 · 응답 예시", "",
    "값 자체가 설계 판단인 세 곳만 싣는다. 나머지는 원본을 본다.", "",
    "### 1) 직관 기록 작성 — 같은 경기를 두 번 기록할 때",
    "",
    "`POST /attendance-logs` (REQ-F-201)",
    "",
    "```",
    "요청",
    "{ \"gameId\": 512, \"cheerTeamId\": 2, \"stadiumZoneId\": 14,",
    "  \"zoneRating\": 4, \"memo\": \"3연승 직관\", \"visibility\": \"PRIVATE\" }",
    "",
    "409 Conflict",
    "{ \"code\": \"DUPLICATE_LOG\", \"message\": \"이미 이 경기 기록이 있어요\" }",
    "```",
    "",
    "애플리케이션 검사만 두면 같은 사람이 동시에 두 번 눌렀을 때 뚫린다.",
    "`attendance_logs (user_id, game_id)` 유일 제약으로 막고 그 위반을 409 로 옮긴다.", "",
    "### 2) 내 전적 요약 — 표본이 모자란 항목",
    "",
    "`GET /stats/me/summary` (REQ-F-305)",
    "",
    "```",
    "200 OK",
    "{ \"total\": { \"games\": 12, \"wins\": 7, \"draws\": 1, \"losses\": 4, \"winRate\": 0.636 },",
    "  \"byStadium\": [",
    "    { \"key\": \"잠실야구장\", \"games\": 5, \"wins\": 3, \"losses\": 2, \"winRate\": 0.600 },",
    "    { \"key\": \"고척스카이돔\", \"games\": 3, \"wins\": 2, \"losses\": 1, \"winRate\": null }",
    "  ] }",
    "```",
    "",
    "통산 승률은 1경기부터 보여주고, 나눠 놓은 집계는 5경기 미만이면 `winRate` 를 `null` 로 준다.",
    "0 이나 0.667 을 주면 화면이 그것을 순위처럼 늘어놓게 된다. 값을 비워 화면이 판단하지 않게 한다.", "",
    "### 3) 메이트 신청 — 정원이 찬 순간",
    "",
    "`POST /companion-posts/{postId}/applications` (REQ-F-503, 504)",
    "",
    "```",
    "201 Created",
    "{ \"applicationId\": 88, \"seq\": 3, \"status\": \"CONFIRMED\" }",
    "",
    "409 Conflict",
    "{ \"code\": \"CAPACITY_FULL\", \"message\": \"방금 정원이 찼어요\" }",
    "```",
    "",
    "여러 명이 같은 순간에 눌러도 `seq` 가 겹치거나 비지 않는다. 모집글의 버전으로 낙관적 잠금을",
    "걸어 정원을 넘긴 요청만 409 로 돌려보낸다. 대기자 개념은 두지 않았다.", "",
    "---", "",
]

total = 0
for tag, rows in grouped.items():
    if not rows:
        continue
    lines += ["## %s" % tag]
    if tags.get(tag):
        lines += ["", tags[tag], ""]
    lines += ["| 메서드 | 경로 | 설명 | 범위 | 인증 | 관련 요구사항 | 비고 |", "|---|---|---|---|---|---|---|"]
    for method, path, op in rows:
        lines.append("| %s | `%s` | %s | %s | %s | %s | %s |" % (
            method, path, op.get("summary", ""), op.get("x-scope", ""),
            auth_mark(op), req_ids(op), status_mark(op)))
        total += 1
    lines.append("")

lines += ["---", "",
          "총 %d개 경로 · %d개 오퍼레이션. 인증이 필요한 요청은 `Authorization: Bearer {accessToken}` 헤더를 요구한다."
          % (len(spec["paths"]), total)]

io.open(OUT, "w", encoding="utf-8").write("\n".join(lines) + "\n")
print("생성: %s (%d 오퍼레이션)" % (os.path.basename(OUT), total))

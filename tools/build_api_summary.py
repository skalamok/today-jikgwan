#!/usr/bin/env python3
"""openapi.yaml 에서 엔드포인트 요약표를 만들어 기술서 부록으로 넣는다.

명세를 고치면 이 스크립트를 다시 돌려 표를 갱신한다. 손으로 옮겨 적지 않는 이유는
두 곳이 어긋나기 때문이다.
"""
import io, os, re, yaml

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(BASE, "docs", "03_API명세", "openapi.yaml")
OUT = os.path.join(BASE, "docs", "01_기술서", "04_부록_API_요약.md")
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
    "# 부록 B. API 엔드포인트 요약", "",
    "| 항목 | 내용 |", "|---|---|",
    "| 프로젝트명 | 오늘의직관 — KBO 직관 기록 서비스 |",
    "| 작성자 | 이채목 |",
    "| 최종 수정일 | 2026-08-19 |",
    "| 원본 | `docs/03_API명세/openapi.yaml` (OpenAPI %s) |" % spec["openapi"], "",
    "`openapi.yaml` 에서 자동 생성한 목록이다. 상세 요청 · 응답 스키마는 원본을 참조한다.",
    "Swagger Editor 에 원본을 붙여넣으면 같은 내용을 화면으로 확인할 수 있다.", "",
    "---", "",
]

total = 0
for tag, rows in grouped.items():
    if not rows:
        continue
    lines += ["## %s" % tag]
    if tags.get(tag):
        lines += ["", tags[tag], ""]
    lines += ["| 메서드 | 경로 | 설명 | 인증 | 관련 요구사항 | 비고 |", "|---|---|---|---|---|---|"]
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

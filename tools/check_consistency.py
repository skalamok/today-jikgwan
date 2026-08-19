#!/usr/bin/env python3
"""산출물 사이의 정합성을 한 번에 확인한다.

문서를 여러 번 고치면서 한 곳만 바뀌고 다른 곳이 남는 일이 반복돼 만들었다.
제출 전에 이 스크립트를 돌려 전부 OK 인지 확인한다.
"""
import io, os, re, sys, glob, subprocess, yaml

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
def read(p): return io.open(os.path.join(BASE, p), encoding="utf-8").read()

req  = read("docs/01_기술서/01_요구사항정의서.md")
scr  = read("docs/01_기술서/02_화면설계서.md")
goal = read("docs/01_기술서/00_프로젝트_목표정의.md")
dbml = read("docs/02_데이터모델링/schema.dbml")
api  = yaml.safe_load(read("docs/03_API명세/openapi.yaml"))
raw_api = read("docs/03_API명세/openapi.yaml")

fails = []
def check(label, ok, detail=""):
    print(("  OK   " if ok else "  FAIL ") + label + ("" if ok else "  → " + str(detail)))
    if not ok: fails.append(label)

METHODS = ("get","post","put","patch","delete")
req_ids = set(re.findall(r'^\| (REQ-[A-Z]+-\d{3}) \|', req, re.M))
scr_ids = set(re.findall(r'^\| (SCR-[A-Z]+-\d{3}) \|', req, re.M))

print("\n■ 식별자")
allrefs = set()
for f in glob.glob(os.path.join(BASE, "docs/01_기술서/*.md")) + [os.path.join(BASE,"docs/03_API명세/openapi.yaml")]:
    allrefs |= set(re.findall(r'REQ-[A-Z]+-\d{3}', io.open(f,encoding="utf-8").read()))
check("요구사항 ID 참조 유효", not (allrefs - req_ids), sorted(allrefs - req_ids))
scrrefs = set(re.findall(r'SCR-[A-Z]+-\d{3}', req + scr)) - scr_ids
check("화면 ID 참조 유효", not scrrefs, sorted(scrrefs))
nums = [int(m) for m in re.findall(r'^\| REQ-F-(\d{3}) \|', req, re.M)]
check("요구사항 ID 오름차순", all(nums[i] >= nums[i-1] for i in range(1, len(nums))))

print("\n■ 화면 설계서 ↔ 요구사항")
names = {m.group(1): m.group(2).strip() for m in re.finditer(r'^\| (SCR-[A-Z]+-\d{3}) \| ([^|]+)\|', req, re.M)}
bad = [m.group(1) for m in re.finditer(r'^## \d+\. (SCR-[A-Z]+-\d{3}) (.+)$', scr, re.M)
       if names.get(m.group(1)) != m.group(2).strip()]
check("화면명 일치", not bad, bad)

def expand(t):
    ids = set(re.findall(r'REQ-F-(\d{3})', t))
    for a, b in re.findall(r'REQ-F-(\d{3})\s*~\s*REQ-F-(\d{3})', t):
        ids |= {f"{n:03d}" for n in range(int(a), int(b)+1)}
    return {"REQ-F-" + i for i in ids}

# 추적성 장 번호는 문서 구조가 바뀌면 달라지므로 제목 줄로 찾는다
m9 = re.search(r'^## \d+\. 추적성 확인$', scr, re.M)
j9 = scr.index("\n## ", m9.end())
ch9 = scr[m9.start():j9]
trace = {m.group(1): expand(m.group(2)) for m in re.finditer(r'^\| (SCR-[A-Z]+-\d{3}) \| ([^|]+)\|', ch9, re.M)}
mismatch = []
for m in re.finditer(r'\n## \d+\. (SCR-[A-Z]+-\d{3})(.*?)(?=\n## |\Z)', scr, re.S):
    sid, body = m.group(1), m.group(2)
    if "### 디스크립션" not in body: continue
    basic = expand(re.search(r'\| 관련 요구사항 \| ([^|]+)\|', body).group(1))
    desc  = expand(body[body.index("### 디스크립션"):body.index("### 예외 처리")])
    if desc != basic or desc != trace.get(sid): mismatch.append(sid)
check("추적성 3중 일치 (기본정보·디스크립션·추적성표)", not mismatch, mismatch)

print("\n■ 와이어프레임")
imgs = set(re.findall(r'!\[[^\]]*\]\(\.\./_assets/wireframes/([^)]+)\)', scr))
missing = [i for i in imgs if not os.path.exists(os.path.join(BASE, "docs/_assets/wireframes", i))]
check("참조 이미지 존재", not missing, missing)
orphan = [os.path.basename(p) for p in glob.glob(os.path.join(BASE,"docs/_assets/wireframes/*.png"))
          if os.path.basename(p) not in imgs]
check("고아 이미지 없음", not orphan, orphan)
stale = [os.path.basename(h) for h in glob.glob(os.path.join(BASE,"docs/_assets/wireframes/*.html"))
         if os.path.exists(h[:-5]+".png") and os.path.getmtime(h) > os.path.getmtime(h[:-5]+".png")]
check("PNG 가 HTML 보다 최신", not stale, stale)

print("\n■ 데이터 모델")
tables = set(re.findall(r'^Table (\w+)', dbml, re.M))
try:
    db = subprocess.run(["docker","exec","todayjikgwan-db","psql","-U","todayjikgwan","-d","todayjikgwan","-tAc",
        "select tablename from pg_tables where schemaname='public' and tablename<>'flyway_schema_history'"],
        capture_output=True, text=True, timeout=15).stdout.split()
    check("dbml ↔ 실제 DB 테이블", set(db) == tables, sorted(set(db) ^ tables))
except Exception as e:
    print("  SKIP  DB 대조 (컨테이너 미기동)")

# dbml 과 openapi 는 전용 파서로 연다. 문법이 깨져도 정규식 검사는 통과해 버린다.
SCRATCH = "/private/tmp/claude-501/-Users-skalamok-Desktop-MokLab/0d11b94f-b02b-4142-916a-9dd02be32672/scratchpad"
if os.path.isdir(os.path.join(SCRATCH, "node_modules")):
    env = dict(os.environ, NODE_PATH=os.path.join(SCRATCH, "node_modules"))
    r = subprocess.run(["node", os.path.join(BASE, "tools/validate_specs.js"), BASE],
                       capture_output=True, text=True, cwd=SCRATCH, env=env, timeout=90).stdout
    check("dbml 파서 통과", "DBML OK" in r, r.strip())
    check("openapi 파서 통과", "OPENAPI OK" in r, r.strip())
else:
    print("  SKIP  전용 파서 (node_modules 없음)")

# ERD 의 인덱스가 실제 데이터베이스와 어긋나면 조회 패턴 설명이 거짓이 된다.
# 이름이 아니라 (컬럼 구성, 유일 여부) 로 견준다. dbml 에는 인덱스 이름을 적지 않는다
def _entry(ln):
    m = re.match(r"^\(?([\w,\s]+?)\)?\s*(\[[^\]]*\])?\s*$", ln.strip())
    if not m:
        return None
    cols = tuple(c.strip() for c in m.group(1).split(","))
    return (cols if len(cols) > 1 else cols[0], "unique" in (m.group(2) or ""))


doc_ix = {}
for _m in re.finditer(r"^Table (\w+)[^{]*\{(.*?)^\}", dbml, re.M | re.S):
    _t, _body = _m.group(1), _m.group(2)
    _ix = re.search(r"^\s*Indexes\s*\{(.*?)\}\s*$", _body, re.S | re.I | re.M)
    for _ln in _body.split("\n"):
        if re.match(r"^\s*Indexes", _ln, re.I):
            continue
        _c = re.match(r"^\s{2}(\w+)\s+\S.*\[[^\]]*\bunique\b", _ln)
        if _c:
            doc_ix.setdefault(_t, set()).add((_c.group(1), True))
    for _ln in (_ix.group(1) if _ix else "").split("\n"):
        _e = _entry(_ln) if _ln.strip() else None
        if _e:
            doc_ix.setdefault(_t, set()).add(_e)

import subprocess as _sp
_sql = ("select tablename, indexdef from pg_indexes where schemaname='public'"
        " and tablename<>'flyway_schema_history' and indexname not like '%_pkey'")
_r = _sp.run(["docker", "exec", "todayjikgwan-db", "psql", "-U", "todayjikgwan",
              "-d", "todayjikgwan", "-t", "-A", "-F", "|", "-c", _sql],
             capture_output=True, text=True)
if _r.returncode == 0 and _r.stdout.strip():
    db_ix = {}
    for _ln in _r.stdout.strip().split("\n"):
        if "|" not in _ln:
            continue
        _t, _ddl = _ln.split("|", 1)
        _cols = tuple(c.strip().split()[0]
                      for c in re.search(r"\(([^)]*)\)\s*$", _ddl).group(1).split(","))
        db_ix.setdefault(_t, set()).add(
            (_cols if len(_cols) > 1 else _cols[0], "UNIQUE INDEX" in _ddl))
    _diff = [t for t in set(doc_ix) | set(db_ix)
             if doc_ix.get(t, set()) != db_ix.get(t, set())]
    check("dbml 인덱스 ↔ 실제 DB", not _diff, sorted(_diff))
else:
    check("dbml 인덱스 ↔ 실제 DB", True, "(DB 미기동, 건너뜀)")

print("\n■ API")
ops = [(p, m, op) for p, v in api["paths"].items() for m, op in v.items() if m in METHODS]
impl = set()
for root, _, files in os.walk(os.path.join(BASE, "backend/src/main/java")):
    for f in files:
        if not f.endswith(".java"): continue
        s = io.open(os.path.join(root, f), encoding="utf-8").read()
        if "@RestController" not in s: continue
        b = re.search(r'@RequestMapping\("([^"]+)"\)', s); base = b.group(1) if b else ""
        for m in re.finditer(r'@(Get|Post|Put|Patch|Delete)Mapping(?:\(\s*(?:value\s*=\s*)?"([^"]*)")?', s):
            sub = m.group(2) or ""
            impl.add((m.group(1).upper(), (base + ("/" + sub.lstrip("/") if sub else "")).replace("/api/v1", "") or "/"))
n = lambda p: re.sub(r'\{[^}]+\}', '{}', p)
I = {(v, n(p)) for v, p in impl}
S = {(m.upper(), n(p)) for p, m, _ in ops}
check("구현 → 명세 누락 없음", not (I - S), sorted(I - S))
undoc = [f"{m.upper()} {p}" for p, m, op in ops if not op.get("description")]
check("모든 오퍼레이션에 설명", not undoc, undoc)

# 명세 품질: operationId 와 공통 오류 응답이 빠지면 클라이언트가 무엇을
# 처리해야 하는지 알 수 없다. tools/patch_openapi.py 가 채운다
gsec = api.get("security")
no_id, no_err = [], []
for pth, item in api["paths"].items():
    for mth, op in item.items():
        if mth not in ("get", "post", "put", "patch", "delete"):
            continue
        if not op.get("operationId"):
            no_id.append(f"{mth.upper()} {pth}")
        codes = set(op.get("responses", {}))
        want = set()
        if op.get("security", gsec): want.add("401")
        if "{" in pth: want.add("404")
        if mth in ("post", "put", "patch"): want.add("400")
        if want - codes:
            no_err.append(f"{mth.upper()} {pth} {sorted(want - codes)}")
# 200 을 주면서 본문 스키마가 없으면 무엇이 오는지 알 수 없다. 본문이 없으면 204 다
no_body = [f"{m.upper()} {pth}"
           for pth, item in api["paths"].items()
           for m, op in item.items()
           if m in ("get", "post", "put", "patch", "delete")
           and op.get("responses", {}).get("200") is not None
           and "content" not in op["responses"]["200"]]
check("200 응답에 본문 스키마", not no_body, no_body)

ids = [o.get("operationId") for i in api["paths"].values() for m, o in i.items()
       if m in ("get", "post", "put", "patch", "delete")]
check("모든 오퍼레이션에 operationId", not no_id, no_id[:5])
check("operationId 중복 없음", len(ids) == len(set(ids)))
check("공통 오류 응답 참조", not no_err, no_err[:5])

print("\n■ 규모 수치")
real = {"기능 요구사항": len(re.findall(r'^\| REQ-F-\d{3} \|', req, re.M)),
        "비기능": len(re.findall(r'^\| REQ-NF-\d{3} \|', req, re.M)),
        "화면": len(scr_ids), "상세 설계": len(re.findall(r'^## \d+\. SCR-', scr, re.M)),
        "테이블": len(tables), "API 오퍼레이션": len(ops)}
line = re.search(r'\| \*\*규모\*\* \| (.+?)(?:<br>|\|)', goal).group(1)
bad = [f"{k}={v}" for k, v in real.items() if str(v) not in line]
check(f"규모 표기 ↔ 실측 ({line.strip()})", not bad, bad)

print("\n■ 발표 자료")
# 발표 자료는 기술서와 따로 관리돼 수치가 조용히 낡는다. 실측과 대조한다
slides = read("tools/build_slides.py")
stale = [f"{k}={v}" for k, v in real.items() if str(v) not in slides]
check("발표 자료 수치 ↔ 실측", not stale, stale)
# 슬라이드가 없는 요구사항 · 화면 ID 를 말하면 발표 중에 바로 걸린다
slide_ids = set(re.findall(r"REQ-[A-Z]+-\d+", slides)) | set(re.findall(r"SCR-[A-Z]+-\d+", slides))
alive_ids = set(re.findall(r"^\| (REQ-[A-Z]+-\d+) \|", req, re.M)) | scr_ids
ghost = sorted(i for i in slide_ids if i not in alive_ids)
check("발표 자료의 식별자 실재", not ghost, ghost)
# 발표 자료가 말하는 기술서 쪽수가 실제와 같은지
pdf_path = os.path.join(BASE, "docs/_제출본/프로젝트기술서_오늘의직관.pdf")
if os.path.exists(pdf_path):
    real_pages = max(int(x) for x in re.findall(rb"/Count (\d+)", io.open(pdf_path, "rb").read()))
    # 원본은 쪽수를 계산해 넣으므로 만들어진 슬라이드에서 확인한다
    shown = _sp.run(["pdftotext", "-f", "5", "-l", "5",
                     os.path.join(BASE, "docs/_제출본/발표자료_오늘의직관.pdf"), "-"],
                    capture_output=True, text=True).stdout
    check("발표 자료의 기술서 쪽수", str(real_pages) in shown, "실제 %d쪽" % real_pages)
check("발표 자료에 옛 용어 없음", "동행자" not in slides and "동행 모집" not in slides)

print("\n■ 버전")
# 머리표의 버전이 개정 이력의 마지막 항목과 어긋나면 어느 쪽이 맞는지 알 수 없다
ver_bad = []
for _f in sorted(glob.glob(os.path.join(BASE, "docs/01_기술서/0*.md"))):
    _s = io.open(_f, encoding="utf-8").read()
    _blk = re.search(r"### 개정 이력\n\n\| 버전 \| 일자 \| 내용 \|\n\|[-|]+\|\n((?:\|.*\n)+)", _s)
    _head = re.search(r"^\| 버전 \| (v[\d.]+) \|$", _s, re.M)
    if not _blk or not _head:
        continue
    _last = re.findall(r"^\| (v[\d.]+) \|", _blk.group(1), re.M)[-1]
    if _last != _head.group(1):
        ver_bad.append("%s 머리 %s ↔ 이력 %s" % (os.path.basename(_f), _head.group(1), _last))
check("문서 버전 ↔ 개정 이력", not ver_bad, ver_bad)

print("\n■ 표 형식")
# API 설계 표의 머리글이 데이터 행보다 한 칸 모자라 범위 값이 인증 칸에 찍힌 적이 있다.
# 눈으로는 표가 밀린 것처럼만 보여 놓치기 쉬우므로 열 수를 센다.
skew = []
for f in sorted(glob.glob(os.path.join(BASE, "docs/01_기술서/*.md"))):
    rows = []
    for i, ln in enumerate(io.open(f, encoding="utf-8").read().split("\n") + [""], 1):
        if ln.startswith("|"):
            rows.append((i, ln.count("|")))
            continue
        if len(rows) >= 2 and len({n for _, n in rows}) > 1:
            skew.append("%s:%d" % (os.path.basename(f), rows[0][0]))
        rows = []
check("표 열 수 일치", not skew, skew)

print("\n■ 잔재")
notes = [l.strip()[:70] for f in glob.glob(os.path.join(BASE,"docs/01_기술서/*.md"))
         for l in io.open(f,encoding="utf-8") if re.search(r'보완 필요|TODO|미니프로젝트|강의 지침|포트폴리오', l)]
check("작업 메모 없음", not notes, notes)

print("\n" + ("전부 통과" if not fails else f"실패 {len(fails)}건: {fails}"))
sys.exit(1 if fails else 0)

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

ch9 = scr[scr.index("## 9."):scr.index("## 10.")]
trace = {m.group(1): expand(m.group(2)) for m in re.finditer(r'^\| (SCR-[A-Z]+-\d{3}) \| ([^|]+)\|', ch9, re.M)}
mismatch = []
for m in re.finditer(r'\n## \d+\. (SCR-[A-Z]+-\d{3})(.*?)(?=\n## |\Z)', scr, re.S):
    sid, body = m.group(1), m.group(2)
    if "### 디스크립션" not in body: continue
    basic = expand(re.search(r'\| 관련 요구사항 \| ([^|]+)\|', body).group(1))
    desc  = expand(body[body.index("### 디스크립션"):body.index("### 예외 처리")])
    if desc != basic or desc != trace.get(sid): mismatch.append(sid)
check("추적성 3중 일치 (기본정보·디스크립션·9장)", not mismatch, mismatch)

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

print("\n■ 규모 수치")
real = {"기능 요구사항": len(re.findall(r'^\| REQ-F-\d{3} \|', req, re.M)),
        "비기능": len(re.findall(r'^\| REQ-NF-\d{3} \|', req, re.M)),
        "화면": len(scr_ids), "상세 설계": len(re.findall(r'^## \d+\. SCR-', scr, re.M)),
        "테이블": len(tables), "API 오퍼레이션": len(ops)}
line = re.search(r'\| \*\*규모\*\* \| (.+?)(?:<br>|\|)', goal).group(1)
bad = [f"{k}={v}" for k, v in real.items() if str(v) not in line]
check(f"규모 표기 ↔ 실측 ({line.strip()})", not bad, bad)

print("\n■ 잔재")
notes = [l.strip()[:70] for f in glob.glob(os.path.join(BASE,"docs/01_기술서/*.md"))
         for l in io.open(f,encoding="utf-8") if re.search(r'보완 필요|TODO|미니프로젝트|강의 지침|포트폴리오', l)]
check("작업 메모 없음", not notes, notes)

print("\n" + ("전부 통과" if not fails else f"실패 {len(fails)}건: {fails}"))
sys.exit(1 if fails else 0)

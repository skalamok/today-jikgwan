# -*- coding: utf-8 -*-
"""
schema.dbml → ERD 이미지

dbdiagram.io 캡처를 쓰지 않는다. 27개 테이블을 한 장에 담으면 글자를 읽을 수 없고,
캡처는 손으로 하는 일이라 dbml 을 고쳐도 그림이 따라오지 않는다. 여기서 그리면
원본이 하나로 유지된다.

두 장을 만든다.
  ERD-CORE  MVP 11개 테이블. 기술서 본문과 발표에 쓴다
  ERD-ALL   전체 27개 테이블. 그룹별로 늘어놓는다

관계선은 브라우저가 카드 위치를 잰 뒤 SVG 로 긋는다. 캡처 전에 스크립트가 돌기 때문에
좌표를 미리 계산해 둘 필요가 없다.

사용: python3 tools/build_erd.py
"""
import io
import json
import os
import re
import subprocess

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SPEC = os.path.join(BASE, "docs", "02_데이터모델링", "schema.dbml")
OUT = os.path.join(BASE, "docs", "_assets", "erd")
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"


def parse():
    src = io.open(SPEC, encoding="utf-8").read()
    tables = {}
    for m in re.finditer(r"^Table (\w+)[^{]*\{(.*?)^\}", src, re.M | re.S):
        name, body = m.group(1), m.group(2)
        cols = []
        for ln in body.split("\n"):
            if re.match(r"^\s*(Indexes|Note|//)", ln, re.I) or not ln.strip():
                continue
            c = re.match(r"^\s{2}(\w+)\s+([\w()\[\],. ]+?)\s*(\[.*\])?$", ln)
            if not c:
                continue
            opts = c.group(3) or ""
            cols.append({"name": c.group(1), "type": c.group(2).strip(),
                         "pk": "pk" in opts, "null": "not null" not in opts})
        note = re.search(r"Note: '(\[[^\]]+\])", body)
        tables[name] = {"cols": cols, "scope": note.group(1)[1:-1] if note else "확장"}

    refs = []
    for a, ac, rel, b, bc in re.findall(
            r"^Ref:\s*(\w+)\.(\w+)\s*([<>-])\s*(\w+)\.(\w+)", src, re.M):
        refs.append({"from": a, "fromCol": ac, "to": b, "toCol": bc, "rel": rel})

    groups = []
    for gm in re.finditer(r'TableGroup\s+"?([^"\n{]+?)"?\s*\{([^}]*)\}', src):
        groups.append({"name": gm.group(1).strip(),
                       "tables": re.findall(r"\b(\w+)\b", gm.group(2))})
    return tables, refs, groups


CSS = """
* { box-sizing: border-box; }
body { margin: 0; background: #fff;
       font-family: Pretendard, 'Apple SD Gothic Neo', sans-serif; color: #1a1a1a; }
.sheet { position: relative; padding: 22px; }
.head { display: flex; justify-content: space-between; align-items: baseline;
        border-bottom: 2px solid #333; padding-bottom: 8px; margin-bottom: 16px; }
.head b { font-size: 17px; } .head span { font-size: 11px; color: #666; }
.cols { display: flex; gap: 54px; align-items: flex-start; position: relative; z-index: 2; }
.grp { flex: 1; min-width: 0; }
.grp > h3 { font-size: 11px; color: #444; margin: 0 0 8px; padding-bottom: 4px;
            border-bottom: 1px dashed #bbb; letter-spacing: .5px; }
.tbl { border: 1.5px solid #333; background: #fff; margin-bottom: 12px; }
.tbl > .t { background: #eef1f5; border-bottom: 1.5px solid #333; padding: 5px 7px;
            font-size: 11.5px; font-weight: 700; display: flex; justify-content: space-between; }
.tbl.ext > .t { background: #fff; }
.tbl .sc { font-size: 9px; font-weight: 400; color: #666; }
.tbl .c { display: flex; justify-content: space-between; gap: 8px;
          padding: 2.5px 7px; font-size: 10px; border-bottom: 1px solid #eee; }
.tbl .c:last-child { border-bottom: 0; }
.tbl .c .k { color: #999; font-size: 9px; }
.tbl .c.pk { font-weight: 700; }
.tbl .c.fk .n { color: #1d4b8f; }
.tbl .c .ty { color: #888; font-size: 9px; white-space: nowrap; }
svg.wires { position: absolute; inset: 0; width: 100%; height: 100%;
            pointer-events: none; z-index: 1; }
svg.wires path { fill: none; stroke: #6b7686; stroke-width: 1.1; }
svg.wires circle { fill: #6b7686; }
svg.wires polygon { fill: #6b7686; }
.legend { margin-top: 14px; border-top: 2px solid #333; padding-top: 8px;
          font-size: 10px; color: #444; display: flex; gap: 18px; }
.sw { display: inline-block; width: 14px; height: 9px; border: 1.5px solid #333;
      background: #eef1f5; margin-right: 4px; vertical-align: -1px; }
.sw.e { background: #fff; }
"""

JS = """
const svg = document.querySelector('svg.wires');
const R = svg.getBoundingClientRect();
const NS = 'http://www.w3.org/2000/svg';
const at = n => document.querySelector(`[data-t="${n}"]`);
const col = (t, c) => document.querySelector(`[data-c="${t}.${c}"]`);
const grps = [...document.querySelectorAll('.grp')].map(g => g.getBoundingClientRect());
const groupOf = el => grps.findIndex(g => el.left >= g.left - 1 && el.right <= g.right + 1);

// 곡선으로 이으면 카드 위를 넘어 다녀 어디서 어디로 가는 선인지 읽히지 않는다.
// 열 사이 빈 공간에 세로줄을 세우고 직각으로 꺾는다. 세로줄은 선마다 조금씩 어긋나게
// 세워 겹치지 않게 한다.
const lane = {};
function laneX(key, base, step) {
  if (!(key in lane)) lane[key] = 0;
  return base + (lane[key]++) * step;
}
function corner(x1, y1, bx, y2, x2, r) {
  const s1 = Math.sign(bx - x1) || 1, s2 = Math.sign(x2 - bx) || 1, sy = Math.sign(y2 - y1) || 1;
  const rr = Math.min(r, Math.abs(bx - x1) / 2, Math.abs(x2 - bx) / 2, Math.abs(y2 - y1) / 2);
  return `M${x1},${y1} H${bx - s1 * rr} Q${bx},${y1} ${bx},${y1 + sy * rr}`
       + ` V${y2 - sy * rr} Q${bx},${y2} ${bx + s2 * rr},${y2} H${x2}`;
}
for (const r of REFS) {
  const a = col(r.from, r.fromCol) || at(r.from);
  const b = at(r.to);
  if (!a || !b) continue;
  const ra = a.getBoundingClientRect(), rb = b.getBoundingClientRect();
  const ga = groupOf(ra), gb = groupOf(rb);
  const y1 = ra.top + ra.height / 2 - R.top;
  const y2 = rb.top + 11 - R.top;
  let x1, x2, bx;
  if (ga === gb) {                       // 같은 열이면 왼쪽 바깥으로 돌린다
    x1 = ra.left - R.left;
    x2 = rb.left - R.left;
    bx = laneX('L' + ga, grps[ga].left - R.left - 12, -7);
  } else if (ga < gb) {                  // 오른쪽 열로 간다
    x1 = ra.right - R.left;
    x2 = rb.left - R.left;
    bx = laneX('G' + ga + '_' + gb, grps[ga].right - R.left + 10, 7);
  } else {                               // 왼쪽 열로 돌아간다
    x1 = ra.left - R.left;
    x2 = rb.right - R.left;
    bx = laneX('G' + gb + '_' + ga, grps[ga].left - R.left - 10, -7);
  }
  const p = document.createElementNS(NS, 'path');
  p.setAttribute('d', corner(x1, y1, bx, y2, x2, 6));
  svg.appendChild(p);
  const dot = document.createElementNS(NS, 'circle');
  dot.setAttribute('cx', x1); dot.setAttribute('cy', y1); dot.setAttribute('r', 2.4);
  svg.appendChild(dot);
  const dir = x2 > bx ? -1 : 1;          // 화살촉은 도착 카드를 향한다
  const tip = document.createElementNS(NS, 'polygon');
  tip.setAttribute('points',
    `${x2},${y2} ${x2 + dir * 6},${y2 - 3.2} ${x2 + dir * 6},${y2 + 3.2}`);
  svg.appendChild(tip);
}
"""


def card(name, t, fks):
    rows = []
    for c in t["cols"]:
        cls = " pk" if c["pk"] else (" fk" if c["name"] in fks else "")
        key = "PK" if c["pk"] else ("FK" if c["name"] in fks else "")
        rows.append(
            '<div class="c%s" data-c="%s.%s"><span class="n">%s</span>'
            '<span class="ty">%s%s</span></div>'
            % (cls, name, c["name"], c["name"], (key + " ") if key else "", c["type"]))
    return ('<div class="tbl %s" data-t="%s"><div class="t"><span>%s</span>'
            '<span class="sc">%s</span></div>%s</div>'
            % ("mvp" if t["scope"] == "MVP" else "ext", name, name, t["scope"], "".join(rows)))


def sheet(title, sub, groups, tables, refs, width):
    fk_of = {}
    for r in refs:
        fk_of.setdefault(r["from"], set()).add(r["fromCol"])
    cols = []
    for g in groups:
        body = "".join(card(n, tables[n], fk_of.get(n, set())) for n in g["tables"] if n in tables)
        if body:
            cols.append('<div class="grp"><h3>%s</h3>%s</div>' % (g["name"], body))
    return """<!doctype html><html lang="ko"><head><meta charset="utf-8">
<style>%s
@font-face { font-family: Pretendard; font-weight: 400;
  src: url('file://%s/docs/_assets/font/Pretendard-Regular.woff2') format('woff2'); }
@font-face { font-family: Pretendard; font-weight: 700;
  src: url('file://%s/docs/_assets/font/Pretendard-Bold.woff2') format('woff2'); }
body { width: %dpx; }</style></head><body>
<div class="sheet"><div class="head"><b>%s</b><span>%s</span></div>
<svg class="wires"></svg><div class="cols">%s</div>
<div class="legend"><span><span class="sw"></span>MVP</span>
<span><span class="sw e"></span>확장</span>
<span>선은 외래키 관계다. 원본은 docs/02_데이터모델링/schema.dbml 이며 이 그림은 거기서 생성한다.</span></div>
</div>
<script>const REFS = %s;\n%s</script></body></html>""" % (
        CSS, BASE, BASE, width, title, sub, "".join(cols), json.dumps(refs, ensure_ascii=False), JS)


def shoot(name, html, width):
    os.makedirs(OUT, exist_ok=True)
    hp = os.path.join(OUT, name + ".html")
    io.open(hp, "w", encoding="utf-8").write(html)
    png = os.path.join(OUT, name + ".png")
    subprocess.run([CHROME, "--headless", "--disable-gpu", "--hide-scrollbars",
                    "--force-device-scale-factor=2",
                    "--window-size=%d,%d" % (width, 2400),
                    "--virtual-time-budget=3000",
                    "--screenshot=" + png, "file://" + hp], capture_output=True)
    from PIL import Image, ImageChops
    im = Image.open(png).convert("RGB")
    bb = ImageChops.difference(im, Image.new("RGB", im.size, (255, 255, 255))).getbbox()
    if bb:
        im = im.crop((0, 0, im.width, min(im.height, bb[3] + 24)))
    im.save(png, optimize=True)
    print("  %-10s %dx%d  %.1fKB" % (name, im.width, im.height, os.path.getsize(png) / 1024))


def main():
    tables, refs, groups = parse()
    core = {n: t for n, t in tables.items() if t["scope"] == "MVP"}
    cg = [{"name": g["name"], "tables": [n for n in g["tables"] if n in core]} for g in groups]
    cr = [r for r in refs if r["from"] in core and r["to"] in core]
    shoot("ERD-CORE", sheet("데이터 모델 — MVP 범위", "%d개 테이블 · %d개 관계" % (len(core), len(cr)),
                            cg, core, cr, 1500), 1500)
    shoot("ERD-ALL", sheet("데이터 모델 — 전체", "%d개 테이블 · %d개 관계" % (len(tables), len(refs)),
                           groups, tables, refs, 2000), 2000)


if __name__ == "__main__":
    main()

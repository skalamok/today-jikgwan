# -*- coding: utf-8 -*-
"""
마크다운 산출물 → 제출용 PDF 생성기

프로젝트 기술서 = 목표 정의 + 요구사항 정의서 + 화면 설계서 (한 파일)
headless Chrome 으로 렌더링하므로 표 폭·페이지 나눔을 CSS 로 제어할 수 있고,
문서를 고치면 이 스크립트만 다시 실행하면 된다.

사용: python3 tools/build_pdf.py
"""
import html as html_mod
import io
import os
import re
import subprocess
import sys

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TECH = os.path.join(BASE, "docs", "01_기술서")
OUT = os.path.join(BASE, "docs", "_제출본")
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"

SOURCES = [
    ("00_프로젝트_목표정의.md", "프로젝트 개요"),
    ("01_요구사항정의서.md", "요구사항 정의서"),
    ("02_화면설계서.md", "화면 설계서"),
    ("03_부록_경기데이터_확보정책.md", "부록 A. 경기 데이터 확보 정책"),
    ("04_부록_API_요약.md", "부록 B. API 엔드포인트 요약"),
]

CSS = """
@page { size: A4; margin: 16mm 13mm 18mm; }
@page :first { margin: 0; }

* { box-sizing: border-box; }
body {
  font-family: 'Apple SD Gothic Neo', 'Malgun Gothic', -apple-system, sans-serif;
  font-size: 10pt; line-height: 1.55; color: #1a1a1a; margin: 0;
}

/* ---------- 표지 ---------- */
.cover {
  height: 297mm; display: flex; flex-direction: column;
  align-items: center; justify-content: center; text-align: center;
  page-break-after: always; background: #fff;
}
.cover .kicker { font-size: 12pt; letter-spacing: 6px; color: #666; margin-bottom: 18mm; }
.cover h1 { font-size: 34pt; margin: 0 0 6mm; letter-spacing: -1px; }
.cover .sub { font-size: 15pt; color: #333; margin-bottom: 4mm; }
.cover .desc { font-size: 11pt; color: #666; margin-bottom: 28mm; }
.cover .meta { font-size: 11pt; color: #333; line-height: 2; }
.cover .rule { width: 60mm; border-top: 2px solid #1a1a1a; margin: 0 0 8mm; }

/* ---------- 목차 ---------- */
.toc { page-break-after: always; padding-top: 8mm; }
.toc h2 { font-size: 16pt; border-bottom: 2px solid #1a1a1a; padding-bottom: 3mm; }
.toc ul { list-style: none; padding: 0; margin: 6mm 0 0; }
.toc li { padding: 2.4mm 0; border-bottom: 1px dotted #ccc; font-size: 11pt; }
.toc li.lv2 { padding-left: 8mm; font-size: 10pt; color: #444; }

/* ---------- 본문 ---------- */
.part { page-break-before: always; }
h2.new-page { page-break-before: always; margin-top: 0; }
.part-head {
  border-bottom: 2.5px solid #1a1a1a; padding-bottom: 3mm; margin: 0 0 7mm;
}
.part-head .no { font-size: 9pt; color: #777; letter-spacing: 3px; }
.part-head h1 { font-size: 20pt; margin: 1mm 0 0; }

h1 { font-size: 16pt; margin: 9mm 0 3mm; }
h2 {
  font-size: 13pt; margin: 8mm 0 3mm; padding-bottom: 1.6mm;
  border-bottom: 1.4px solid #333; page-break-after: avoid;
}
h3 { font-size: 11.5pt; margin: 6mm 0 2.5mm; page-break-after: avoid; }
h4 { font-size: 10.5pt; margin: 4mm 0 2mm; page-break-after: avoid; }
p { margin: 2mm 0; }
ul { margin: 2mm 0 2mm 5mm; padding: 0; }
li { margin: 1mm 0; }
blockquote {
  margin: 3mm 0; padding: 2.5mm 4mm; border-left: 3px solid #999;
  background: #f6f6f6; color: #333; font-size: 9.5pt;
}
code {
  font-family: 'SFMono-Regular', Consolas, monospace; font-size: 8.8pt;
  background: #f0f0f0; padding: 0.4mm 1.2mm; border-radius: 2px;
}
pre {
  background: #f6f6f6; border: 1px solid #ddd; padding: 3mm;
  font-size: 8pt; line-height: 1.35; overflow: hidden; white-space: pre-wrap;
}

/* ---------- 표 ---------- */
table {
  width: 100%; border-collapse: collapse; margin: 3mm 0 5mm;
  font-size: 8.4pt; line-height: 1.4; page-break-inside: auto;
}
th, td {
  border: 0.6px solid #999; padding: 1.6mm 2mm;
  vertical-align: top;
  /* 한글은 글자 단위로 끊지 않고 어절 단위로 줄바꿈한다 */
  word-break: keep-all; overflow-wrap: break-word;
}
th { background: #e9edf2; font-weight: 700; text-align: left; }
tr { page-break-inside: avoid; }

thead { display: table-header-group; }

/* ---------- 요구사항 표 ---------- */
/* 열 너비를 고정하지 않으면 ID·분류 열이 글자 단위로 쪼개진다 */
table.req { font-size: 7.4pt; table-layout: fixed; }
table.req th, table.req td { padding: 1.1mm 1.4mm; }
/* 분류 열 머리글은 3글자라 좁은 폭에서 접힌다. 머리글만 줄바꿈을 막는다 */
table.req th:nth-child(2), table.req th:nth-child(3) { white-space: nowrap; }

/* 9열: 기능 요구사항 (ID/대분류/중분류/요구사항명/상세/중요도/난이도/관련화면/비고) */
table.req9 th:nth-child(1), table.req9 td:nth-child(1) { width: 8.5%; white-space: nowrap; }
table.req9 th:nth-child(2), table.req9 td:nth-child(2) { width: 5.5%; }
table.req9 th:nth-child(3), table.req9 td:nth-child(3) { width: 5.5%; }
table.req9 th:nth-child(4), table.req9 td:nth-child(4) { width: 10%; }
table.req9 th:nth-child(5), table.req9 td:nth-child(5) { width: 28%; }
table.req9 th:nth-child(6), table.req9 td:nth-child(6) { width: 7%; white-space: nowrap; text-align: center; }
table.req9 th:nth-child(7), table.req9 td:nth-child(7) { width: 5%; white-space: nowrap; text-align: center; }
table.req9 th:nth-child(8), table.req9 td:nth-child(8) { width: 12%; }
table.req9 th:nth-child(9), table.req9 td:nth-child(9) { width: 18.5%; }

/* 8열: 비기능 요구사항 (관련 화면 ID 열 없음) */
table.req8 th:nth-child(1), table.req8 td:nth-child(1) { width: 9%; white-space: nowrap; }
table.req8 th:nth-child(2), table.req8 td:nth-child(2) { width: 6%; }
table.req8 th:nth-child(3), table.req8 td:nth-child(3) { width: 6%; }
table.req8 th:nth-child(4), table.req8 td:nth-child(4) { width: 12%; }
table.req8 th:nth-child(5), table.req8 td:nth-child(5) { width: 35.5%; }
table.req8 th:nth-child(6), table.req8 td:nth-child(6) { width: 7%; white-space: nowrap; text-align: center; }
table.req8 th:nth-child(7), table.req8 td:nth-child(7) { width: 5%; white-space: nowrap; text-align: center; }
table.req8 th:nth-child(8), table.req8 td:nth-child(8) { width: 19%; }

/* API 요약 표(6열): 메서드 / 경로 / 설명 / 인증 / 요구사항 / 비고 */
table.api { table-layout: fixed; font-size: 7.8pt; }
table.api th:nth-child(1), table.api td:nth-child(1) { width: 8%; white-space: nowrap; text-align: center; }
table.api th:nth-child(2), table.api td:nth-child(2) { width: 30%; word-break: break-all; }
table.api th:nth-child(3), table.api td:nth-child(3) { width: 28%; }
table.api th:nth-child(4), table.api td:nth-child(4) { width: 7%; white-space: nowrap; text-align: center; }
table.api th:nth-child(5), table.api td:nth-child(5) { width: 19%; }
table.api th:nth-child(6), table.api td:nth-child(6) { width: 8%; white-space: nowrap; text-align: center; }

/* 디스크립션 표(5열): NO / 매핑 요구사항 ID / UI 요소명 / 로직 / 이동 화면 */
table.desc { table-layout: fixed; }
table.desc th:nth-child(1), table.desc td:nth-child(1) { width: 5%; }
table.desc th:nth-child(2), table.desc td:nth-child(2) { width: 17%; }
table.desc th:nth-child(3), table.desc td:nth-child(3) { width: 14%; }
table.desc th:nth-child(4), table.desc td:nth-child(4) { width: 44%; }
table.desc th:nth-child(5), table.desc td:nth-child(5) { width: 20%; }

/* ---------- 이미지(와이어프레임) ---------- */
img { display: block; margin: 4mm auto; border: 1px solid #ccc; }
img.mobile { width: 62mm; }
img.pc { width: 165mm; }
figure { page-break-inside: avoid; margin: 0; }
"""


def md_inline(t):
    t = html_mod.escape(t)
    t = re.sub(r"`([^`]+)`", r"<code>\1</code>", t)
    t = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", t)
    t = re.sub(r"~~(.+?)~~", r"<del>\1</del>", t)
    t = t.replace("&lt;br&gt;", "<br>")
    return t


def md_to_html(md, doc_dir):
    lines = md.split("\n")
    out, i, n = [], 0, len(lines)
    headings = []
    while i < n:
        raw = lines[i]
        s = raw.strip()
        if not s:
            i += 1
            continue

        if s.startswith("```"):
            buf = []
            i += 1
            while i < n and not lines[i].strip().startswith("```"):
                buf.append(html_mod.escape(lines[i]))
                i += 1
            i += 1
            out.append("<pre>" + "\n".join(buf) + "</pre>")
            continue

        if set(s) <= set("-") and len(s) >= 3:
            i += 1
            continue

        m = re.match(r"^(#{1,4})\s+(.*)$", s)
        if m:
            lv = len(m.group(1))
            text = m.group(2).strip()
            anchor = "h%d" % len(headings)
            headings.append((lv, re.sub(r"[*~`]", "", text), anchor))
            # 분량이 큰 장은 새 페이지에서 시작해 앞 장의 표와 섞이지 않게 한다
            cls = ' class="new-page"' if text.startswith(("3. 기능 요구사항", "4. 비기능 요구사항")) else ""
            out.append('<h%d id="%s"%s>%s</h%d>' % (lv, anchor, cls, md_inline(text), lv))
            i += 1
            continue

        m = re.match(r"^!\[[^\]]*\]\(([^)]+)\)$", s)
        if m:
            src = os.path.normpath(os.path.join(doc_dir, m.group(1)))
            cls = "pc" if src.endswith("-PC.png") else "mobile"
            out.append('<figure><img class="%s" src="file://%s"></figure>' % (cls, src))
            i += 1
            continue

        if s.startswith("|") and i + 1 < n and re.match(r"^\|[\s:\-\|]+\|$", lines[i + 1].strip()):
            rows = []
            while i < n and lines[i].strip().startswith("|"):
                r = lines[i].strip()
                if not re.match(r"^\|[\s:\-\|]+\|$", r):
                    rows.append([c.strip() for c in r.strip("|").split("|")])
                i += 1
            ncol = len(rows[0])
            first = rows[0][0]
            if ncol >= 8 and first.startswith("요구사항"):
                cls = ' class="req req%d"' % ncol
            elif ncol == 6 and first == "메서드":
                cls = ' class="api"'
            elif ncol == 5 and first == "NO":
                cls = ' class="desc"'
            else:
                cls = ""
            t = ["<table%s><thead><tr>" % cls]
            t += ["<th>%s</th>" % md_inline(c) for c in rows[0]]
            t.append("</tr></thead><tbody>")
            for r in rows[1:]:
                t.append("<tr>" + "".join("<td>%s</td>" % md_inline(c) for c in r) + "</tr>")
            t.append("</tbody></table>")
            out.append("".join(t))
            continue

        if s.startswith("> "):
            buf = []
            while i < n and lines[i].strip().startswith("> "):
                buf.append(md_inline(lines[i].strip()[2:]))
                i += 1
            out.append("<blockquote>%s</blockquote>" % "<br>".join(buf))
            continue

        if s.startswith("- ") or s.startswith("* "):
            buf = []
            while i < n and (lines[i].strip().startswith("- ") or lines[i].strip().startswith("* ")):
                buf.append("<li>%s</li>" % md_inline(lines[i].strip()[2:]))
                i += 1
            out.append("<ul>%s</ul>" % "".join(buf))
            continue

        out.append("<p>%s</p>" % md_inline(s))
        i += 1
    return "\n".join(out), headings


def build():
    parts, toc = [], []
    for idx, (fname, title) in enumerate(SOURCES, 1):
        path = os.path.join(TECH, fname)
        body, headings = md_to_html(io.open(path, encoding="utf-8").read(), TECH)
        # 원본 문서의 최상위 제목은 파트 헤더로 대체한다
        body = re.sub(r"<h1[^>]*>.*?</h1>", "", body, count=1)
        parts.append(
            '<section class="part"><div class="part-head">'
            '<div class="no">PART %d</div><h1>%s</h1></div>%s</section>' % (idx, title, body)
        )
        toc.append((1, "PART %d. %s" % (idx, title)))
        for lv, text, _ in headings:
            if lv == 2 and not text.startswith("0-"):
                toc.append((2, text))

    toc_html = "".join(
        '<li class="lv%d">%s</li>' % (lv, html_mod.escape(t)) for lv, t in toc
    )

    cover = """
<div class="cover">
  <div class="kicker">SKALA MINI PROJECT</div>
  <div class="rule"></div>
  <h1>프로젝트 기술서</h1>
  <div class="sub">오늘의직관</div>
  <div class="desc">KBO 직관 기록 및 관람 정보 서비스</div>
  <div class="meta">
    작성자 · 이채목<br>작성일 · 2026-08-18<br>버전 · v1.0
  </div>
</div>
<div class="toc"><h2>목차</h2><ul>%s</ul></div>
""" % toc_html

    html = ("<!doctype html><html lang='ko'><head><meta charset='utf-8'>"
            "<title>프로젝트 기술서 - 오늘의직관</title>"
            "<style>%s</style></head><body>%s%s</body></html>"
            % (CSS, cover, "".join(parts)))

    os.makedirs(OUT, exist_ok=True)
    html_path = os.path.join(OUT, "_기술서.html")
    io.open(html_path, "w", encoding="utf-8").write(html)

    pdf_path = os.path.join(OUT, "프로젝트기술서_오늘의직관.pdf")
    subprocess.run([
        CHROME, "--headless", "--disable-gpu", "--no-pdf-header-footer",
        "--print-to-pdf=" + pdf_path, "--virtual-time-budget=10000",
        "file://" + html_path,
    ], check=True, capture_output=True)

    os.remove(html_path)   # 중간 산출물은 남기지 않는다
    print("PDF :", pdf_path, "(%.1fKB)" % (os.path.getsize(pdf_path) / 1024))
    return pdf_path


if __name__ == "__main__":
    if not os.path.exists(CHROME):
        sys.exit("Chrome 을 찾을 수 없습니다: " + CHROME)
    build()

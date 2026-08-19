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
FONT = os.path.join(BASE, "docs", "_assets", "font")

# 프리텐다드를 문서 폰트로 쓴다. 라틴 글자가 한글과 함께 설계돼 있어
# 영문 폰트를 따로 섞지 않아도 REQ-F-101 같은 표기가 어긋나지 않는다.
FONT_CSS = "".join(
    "@font-face { font-family: Pretendard; font-weight: %d; font-style: normal;"
    " font-display: block; src: url('file://%s/Pretendard-%s.woff2') format('woff2'); }\n"
    % (w, FONT, n)
    for w, n in ((400, "Regular"), (500, "Medium"), (600, "SemiBold"), (700, "Bold"))
)

# 로고를 data URI 로 심어 PDF 안에 함께 담는다 (외부 파일 참조 없이 열리게)
MARK_URI = "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCA2NCA2NCIgcm9sZT0iaW1nIiBhcmlhLWxhYmVsPSLsmKTripjsnZjsp4HqtIAiPgogIDwhLS0KICAgIO2LsOy8k+ydhCDso7zsnbjqs7XsnLzroZwg65GU64ukLiDsp4HqtIDsnYAg6rK96riw7J6l7JeQIOqwhCDtlonsnITsnbTqs6AsIOq3uCDspp3qsbDqsIAg7Yuw7LyT7J2064ukLgogICAg7JWI7Kq9IOuRkCDqs6HshKDsnYAg7JW86rWs6rO1IOyLpOuwpeydtOupsCwg64uo7IOJ7Jy866GcIOyTsOqxsOuCmCDrsJjsoITtlbTrj4Qg7IKs65287KeA7KeAIOyViuuPhOuhnQogICAg7ISg7J2EIOyWueyngCDslYrqs6Ag64+E7ZiV7JeQ7IScIO2MjOuCuOuLpC4KICAtLT4KICA8cGF0aCBmaWxsPSIjMTYzNTVjIiBmaWxsLXJ1bGU9ImV2ZW5vZGQiIGQ9IgogICAgTTggMTRoNDhhNCA0IDAgMCAxIDQgNHY3LjVhNi41IDYuNSAwIDAgMCAwIDEzVjQ2YTQgNCAwIDAgMS00IDRIOGE0IDQgMCAwIDEtNC00di03LjVhNi41IDYuNSAwIDAgMCAwLTEzVjE4YTQgNCAwIDAgMSA0LTRaCiAgICBNMjMuNCAyMS42YTIgMiAwIDAgMC0yLjggMi44YzMuOSAzLjkgMy45IDExLjIgMCAxNS4yYTIgMiAwIDEgMCAyLjggMi44YzUuNC01LjQgNS40LTE1LjQgMC0yMC44WgogICAgTTQzLjQgMjEuNmMtNS40IDUuNC01LjQgMTUuNCAwIDIwLjhhMiAyIDAgMCAwIDIuOC0yLjhjLTMuOS00LTMuOS0xMS4zIDAtMTUuMmEyIDIgMCAwIDAtMi44LTIuOFoKICAiLz4KPC9zdmc+Cg=="

SOURCES = [
    ("00_프로젝트_목표정의.md", "프로젝트 개요"),
    ("01_요구사항정의서.md", "요구사항 정의서"),
    ("02_화면설계서.md", "화면 설계서"),
    ("03_데이터모델.md", "데이터 모델"),
    ("04_부록_경기데이터_확보정책.md", "부록 A. 경기 데이터 확보 정책"),
    ("05_부록_API_요약.md", "부록 B. API 엔드포인트 요약"),
]

CSS = """
/* 여백은 인쇄 단계(print_pdf.js)에서 준다. 여기에 margin 을 쓰면
   CDP 의 margin 값이 통째로 무시돼 본문이 종이 끝에 붙는다. */
@page { size: A4; }

* { box-sizing: border-box; }
body {
  font-family: Pretendard, 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif;
  font-size: 10pt; line-height: 1.55; color: #1a1a1a; margin: 0;
}

/* ---------- 표지 ---------- */
.cover {
  height: 255mm; display: flex; flex-direction: column;   /* 인쇄 가능 높이 263mm */
  align-items: center; justify-content: center; text-align: center;
  page-break-after: always; background: #fff;
}
.cover .kicker { font-size: 12pt; letter-spacing: 6px; color: #666; margin-bottom: 12mm; }
.cover .logo { width: 78mm; margin: 0 auto 14mm; display: block; background: none; }
/* 매 쪽 머리글에 심볼을 작게 넣는다 */
@page { @top-right { content: none; } }
.part-head { position: relative; }
.part-head .mark { position: absolute; right: 0; top: 2mm; width: 9mm; opacity: .9; }
.cover h1 { font-size: 34pt; margin: 0 0 6mm; letter-spacing: -1px; }
.cover .sub { font-size: 15pt; color: #333; margin-bottom: 4mm; }
.cover .desc { font-size: 11pt; color: #666; margin-bottom: 28mm; }
.cover .meta { font-size: 11pt; color: #333; line-height: 2; }
.cover .rule { width: 60mm; border-top: 2px solid #1a1a1a; margin: 0 0 8mm; }

/* ---------- 목차 ---------- */
.toc { page-break-after: always; padding-top: 8mm; }
.toc h2 { font-size: 16pt; border-bottom: 2px solid #1a1a1a; padding-bottom: 3mm; }
.toc ul {
  list-style: none; padding: 0; margin: 6mm 0 0;
  column-count: 2; column-gap: 10mm;
}
.toc li {
  padding: 1.7mm 0; border-bottom: 1px dotted #ccc; font-size: 10pt;
  break-inside: avoid;
}
.toc li.lv1 { font-weight: 700; margin-top: 2mm; break-after: avoid; }
.toc li.lv2 { padding-left: 6mm; font-size: 9pt; color: #444; }

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

/* 태스크 표: 상태별로 행을 구분한다 */
tr.st-done { background: #eef7f0; }
tr.st-done td:last-child { color: #1d6b3a; font-weight: 700; }
tr.st-wip  { background: #fff8e8; }
tr.st-wip  td:last-child { color: #9a6a00; font-weight: 700; }
tr.st-todo td:last-child { color: #8a94a6; }

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
table.req9 th:nth-child(5), table.req9 td:nth-child(5) { width: 25%; }
table.req9 th:nth-child(6), table.req9 td:nth-child(6) { width: 7%; white-space: nowrap; text-align: center; }
table.req9 th:nth-child(7), table.req9 td:nth-child(7) { width: 5%; white-space: nowrap; text-align: center; }
table.req9 th:nth-child(8), table.req9 td:nth-child(8) { width: 12%; }
table.req9 th:nth-child(9), table.req9 td:nth-child(9) { width: 18.5%; }

/* 8열: 비기능 요구사항 (관련 화면 ID 열 없음) */
/* 기능 요구사항 - 범위 열이 붙어 10칸.
   좁은 칸은 데이터만 줄바꿈을 막는다. 머리글까지 막으면 "요구사항 ID" 가 칸을 넘친다 */
table.req10 { font-size: 7.6pt; }
table.req10 th, table.req10 td { padding: 0.9mm 1.1mm; }
table.req10 td:nth-child(1) {  width: 9.5%; white-space: nowrap; }
table.req10 td:nth-child(2) {  width: 4.5%; white-space: nowrap; }
table.req10 td:nth-child(3) {  width: 7%; white-space: nowrap; }
table.req10 th:nth-child(4), table.req10 td:nth-child(4) { width: 9.5%; }
table.req10 th:nth-child(5), table.req10 td:nth-child(5) { width: 24%; }
table.req10 td:nth-child(6) {  width: 4.5%; white-space: nowrap; text-align: center; }
table.req10 td:nth-child(7) {  width: 5.5%; white-space: nowrap; text-align: center; }
table.req10 td:nth-child(8) {  width: 5.5%; white-space: nowrap; text-align: center; }
table.req10 th:nth-child(9), table.req10 td:nth-child(9) { width: 12%; }
table.req10 th:nth-child(10), table.req10 td:nth-child(10) { width: 18%; }
table.req10 th:nth-child(1) { width: 9.5%; }
table.req10 th:nth-child(2) { width: 4.5%; }
table.req10 th:nth-child(3) { width: 7%; }
table.req10 th:nth-child(6) { width: 4.5%; }
table.req10 th:nth-child(7) { width: 5.5%; }
table.req10 th:nth-child(8) { width: 5.5%; }
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
figure img { display: block; margin: 4mm auto; border: 1px solid #ccc; }
img.logo, img.mark { border: 0; }
/* 모바일 판과 데스크톱 판을 한 쪽에 같이 싣는다. 높이를 제한하지 않으면
   세로로 긴 모바일 캡처가 한 쪽을 다 먹고 데스크톱 판이 다음 쪽으로 밀린다. */
img.mobile { max-width: 58mm; max-height: 108mm; }
img.pc { max-width: 160mm; max-height: 108mm; }
img.flow { max-width: 170mm; }
/* ERD 는 세로로 길어 폭을 다 쓰고 높이 제한을 두지 않는다 */
img.erd { max-width: 178mm; }
figure { page-break-inside: avoid; margin: 0; }
/* 그림 바로 앞의 설명 문단은 그림과 떨어지지 않게 둔다 */
p + figure { page-break-before: avoid; }
"""


ID_CELL = re.compile(r"^[A-Z][A-Z0-9]*(?:-[A-Z0-9]+)+$")


def keep_id(cell):
    """G-4, REQ-F-001, SCR-GAME-001 처럼 통째로 읽어야 하는 칸은 줄바꿈을 막는다."""
    return ('<span style="white-space:nowrap">%s</span>' % cell) if ID_CELL.match(cell.strip()) else cell


def md_inline(t):
    # 원본의 줄바꿈은 읽기 편하려고 넣은 것이므로 한 문단으로 이어 붙인다.
    # 이렇게 하지 않으면 **강조**가 줄을 넘어갈 때 변환되지 않고 별표가 그대로 남는다.
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
            cls = ("pc" if src.endswith("-PC.png") else "erd" if "/erd/" in src
                   else "flow" if "/FLOW-" in src else "mobile")
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
                # 상태 열(완료·진행·예정)이 있으면 행에 표시해 한눈에 보이게 한다
                st = next((c for c in r if c in ("완료", "진행", "예정")), "")
                cls = ' class="st-%s"' % {"완료": "done", "진행": "wip", "예정": "todo"}[st] if st else ""
                t.append("<tr%s>" % cls + "".join("<td>%s</td>" % keep_id(md_inline(c)) for c in r) + "</tr>")
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
                item = [lines[i].strip()[2:]]
                i += 1
                # 들여쓰기 없이 이어지는 줄은 같은 항목의 연속으로 본다
                while i < n:
                    nxt = lines[i].strip()
                    if not nxt or nxt.startswith(("#", "|", ">", "- ", "* ", "```", "![")):
                        break
                    item.append(nxt)
                    i += 1
                buf.append("<li>%s</li>" % md_inline(" ".join(item)))
            out.append("<ul>%s</ul>" % "".join(buf))
            continue

        # 빈 줄이 나올 때까지 이어 붙인다. 원본의 줄바꿈은 편집 편의를 위한 것이고,
        # 줄 단위로 변환하면 **강조**가 줄을 넘어갈 때 별표가 그대로 남는다.
        buf = []
        while i < n:
            cur = lines[i].strip()
            if not cur or cur.startswith(("#", "|", ">", "- ", "* ", "```", "![")):
                break
            buf.append(cur)
            i += 1
        out.append("<p>%s</p>" % md_inline(" ".join(buf)))
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
            '<img class="mark" src="' + MARK_URI + '" alt="">'
            '<div class="no">PART %d</div><h1>%s</h1></div>%s</section>' % (idx, title, body)
        )
        toc.append((1, "PART %d. %s" % (idx, title)))
        # 부록 B 는 태그별 소제목이 열둘이라 목차만 길어진다. 파트 제목으로 갈음한다.
        if "API 엔드포인트 요약" in title:
            continue
        for lv, text, _ in headings:
            if lv == 2 and not text.startswith("0-"):
                toc.append((2, text))

    toc_html = "".join(
        '<li class="lv%d">%s</li>' % (lv, html_mod.escape(t)) for lv, t in toc
    )

    cover = """
<div class="cover">
  <div class="kicker">SKALA MINI PROJECT</div>
  <img class="logo" src="data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxODggNjQiIHJvbGU9ImltZyIgYXJpYS1sYWJlbD0i7Jik64qY7J2Y7KeB6rSAIj4KICA8IS0tIHRvb2xzL2J1aWxkX2xvZ28ucHkg6rCAIOyDneyEse2VnOuLpC4g7KeB7KCRIOqzoOy5mOyngCDslYrripTri6QuIC0tPgogIDxwYXRoIGZpbGw9IiMxNjM1NWMiIGZpbGwtcnVsZT0iZXZlbm9kZCIgZD0iCiAgICBNOCAxNGg0OGE0IDQgMCAwIDEgNCA0djcuNWE2LjUgNi41IDAgMCAwIDAgMTNWNDZhNCA0IDAgMCAxLTQgNEg4YTQgNCAwIDAgMS00LTR2LTcuNWE2LjUgNi41IDAgMCAwIDAtMTNWMThhNCA0IDAgMCAxIDQtNFoKICAgIE0yMy40IDIxLjZhMiAyIDAgMCAwLTIuOCAyLjhjMy45IDMuOSAzLjkgMTEuMiAwIDE1LjJhMiAyIDAgMSAwIDIuOCAyLjhjNS40LTUuNCA1LjQtMTUuNCAwLTIwLjhaCiAgICBNNDMuNCAyMS42Yy01LjQgNS40LTUuNCAxNS40IDAgMjAuOGEyIDIgMCAwIDAgMi44LTIuOGMtMy45LTQtMy45LTExLjMgMC0xNS4yYTIgMiAwIDAgMC0yLjgtMi44WgogICIvPgogIDxwYXRoIGZpbGw9IiMxNjM1NWMiIGQ9Ik05NS45MzE2NDA2MjUgMjcuNzQ2MDkzNzVROTUuOTE4OTQ1MzEyNSAyOS40NDcyNjU2MjUgOTUuMDE3NTc4MTI1IDMwLjc5Mjk2ODc1UTk0LjExNjIxMDkzNzUgMzIuMTM4NjcxODc1IDkyLjUxMDI1MzkwNjI1IDMyLjk3MDIxNDg0Mzc1UTkwLjkwNDI5Njg3NSAzMy44MDE3NTc4MTI1IDg4LjgyMjI2NTYyNSAzNC4wNDI5Njg3NVYzNy45Nzg1MTU2MjVIOTcuODM1OTM3NVY0MC42NDQ1MzEyNUg3Ni42ODU1NDY4NzVWMzcuOTc4NTE1NjI1SDg1LjU5NzY1NjI1VjM0LjAzMDI3MzQzNzVRODMuNTI4MzIwMzEyNSAzMy44MDE3NTc4MTI1IDgxLjkyODcxMDkzNzUgMzIuOTYzODY3MTg3NVE4MC4zMjkxMDE1NjI1IDMyLjEyNTk3NjU2MjUgNzkuNDM0MDgyMDMxMjUgMzAuNzg2NjIxMDkzNzVRNzguNTM5MDYyNSAyOS40NDcyNjU2MjUgNzguNTM5MDYyNSAyNy43NDYwOTM3NVE3OC41MzkwNjI1IDI1Ljg3OTg4MjgxMjUgNzkuNjYyNTk3NjU2MjUgMjQuNDM4OTY0ODQzNzVRODAuNzg2MTMyODEyNSAyMi45OTgwNDY4NzUgODIuNzY2NjAxNTYyNSAyMi4yMTA5Mzc1UTg0Ljc0NzA3MDMxMjUgMjEuNDIzODI4MTI1IDg3LjIyMjY1NjI1IDIxLjQyMzgyODEyNVE4OS42OTgyNDIxODc1IDIxLjQyMzgyODEyNSA5MS42Nzg3MTA5Mzc1IDIyLjIxMDkzNzVROTMuNjU5MTc5Njg3NSAyMi45OTgwNDY4NzUgOTQuNzg5MDYyNSAyNC40Mzg5NjQ4NDM3NVE5NS45MTg5NDUzMTI1IDI1Ljg3OTg4MjgxMjUgOTUuOTMxNjQwNjI1IDI3Ljc0NjA5Mzc1Wk04MS43MzgyODEyNSAyNy43NDYwOTM3NVE4MS43MzgyODEyNSAyOC45Mzk0NTMxMjUgODIuNDMwMTc1NzgxMjUgMjkuNzk2Mzg2NzE4NzVRODMuMTIyMDcwMzEyNSAzMC42NTMzMjAzMTI1IDg0LjM1OTg2MzI4MTI1IDMxLjEwNDAwMzkwNjI1UTg1LjU5NzY1NjI1IDMxLjU1NDY4NzUgODcuMjIyNjU2MjUgMzEuNTU0Njg3NVE4OC44NjAzNTE1NjI1IDMxLjU1NDY4NzUgOTAuMDk4MTQ0NTMxMjUgMzEuMTEwMzUxNTYyNVE5MS4zMzU5Mzc1IDMwLjY2NjAxNTYyNSA5Mi4wMjc4MzIwMzEyNSAyOS44MDI3MzQzNzVROTIuNzE5NzI2NTYyNSAyOC45Mzk0NTMxMjUgOTIuNzMyNDIxODc1IDI3Ljc0NjA5Mzc1UTkyLjcxOTcyNjU2MjUgMjYuNjAzNTE1NjI1IDkyLjAyNzgzMjAzMTI1IDI1Ljc1OTI3NzM0Mzc1UTkxLjMzNTkzNzUgMjQuOTE1MDM5MDYyNSA5MC4wOTE3OTY4NzUgMjQuNDY0MzU1NDY4NzVRODguODQ3NjU2MjUgMjQuMDEzNjcxODc1IDg3LjIyMjY1NjI1IDI0LjAxMzY3MTg3NVE4NS42MTAzNTE1NjI1IDI0LjAxMzY3MTg3NSA4NC4zNjYyMTA5Mzc1IDI0LjQ2NDM1NTQ2ODc1UTgzLjEyMjA3MDMxMjUgMjQuOTE1MDM5MDYyNSA4Mi40MzAxNzU3ODEyNSAyNS43NjU2MjVRODEuNzM4MjgxMjUgMjYuNjE2MjEwOTM3NSA4MS43MzgyODEyNSAyNy43NDYwOTM3NVpNMTE3LjM0Mzc1IDI3LjQ2Njc5Njg3NUgxMDEuMjcxNDg0Mzc1VjIwLjUwOTc2NTYyNUgxMDQuNDk2MDkzNzVWMjQuOTAyMzQzNzVIMTE3LjM0Mzc1Wk0xMTkuNzgxMjUgMzEuNTgwMDc4MTI1SDk4LjYzMDg1OTM3NVYyOS4wMTU2MjVIMTE5Ljc4MTI1Wk0xMTcuMjQyMTg3NSAzOS4xMjEwOTM3NUgxMDQuMzQzNzVWNDAuNTkzNzVIMTE3Ljk1MzEyNVY0My4wODIwMzEyNUgxMDEuMDkzNzVWMzYuNzg1MTU2MjVIMTE0LjAxNzU3ODEyNVYzNS40NjQ4NDM3NUgxMDEuMDQyOTY4NzVWMzIuOTc2NTYyNUgxMTcuMjQyMTg3NVpNMTM0LjUxNTYyNSAyNy40NDE0MDYyNVExMzQuNTE1NjI1IDI5LjA3OTEwMTU2MjUgMTMzLjY4NDA4MjAzMTI1IDMwLjM3NDAyMzQzNzVRMTMyLjg1MjUzOTA2MjUgMzEuNjY4OTQ1MzEyNSAxMzEuMzkyNTc4MTI1IDMyLjM5ODkyNTc4MTI1UTEyOS45MzI2MTcxODc1IDMzLjEyODkwNjI1IDEyOC4xMTcxODc1IDMzLjEyODkwNjI1UTEyNi4zMDE3NTc4MTI1IDMzLjEyODkwNjI1IDEyNC44MzU0NDkyMTg3NSAzMi4zOTg5MjU3ODEyNVExMjMuMzY5MTQwNjI1IDMxLjY2ODk0NTMxMjUgMTIyLjUzMTI1IDMwLjM3NDAyMzQzNzVRMTIxLjY5MzM1OTM3NSAyOS4wNzkxMDE1NjI1IDEyMS42OTMzNTkzNzUgMjcuNDQxNDA2MjVRMTIxLjY5MzM1OTM3NSAyNS44MTY0MDYyNSAxMjIuNTMxMjUgMjQuNTIxNDg0Mzc1UTEyMy4zNjkxNDA2MjUgMjMuMjI2NTYyNSAxMjQuODQxNzk2ODc1IDIyLjQ5NjU4MjAzMTI1UTEyNi4zMTQ0NTMxMjUgMjEuNzY2NjAxNTYyNSAxMjguMTE3MTg3NSAyMS43NzkyOTY4NzVRMTI5LjkxOTkyMTg3NSAyMS43NjY2MDE1NjI1IDEzMS4zODYyMzA0Njg3NSAyMi40OTY1ODIwMzEyNVExMzIuODUyNTM5MDYyNSAyMy4yMjY1NjI1IDEzMy42ODQwODIwMzEyNSAyNC41MjE0ODQzNzVRMTM0LjUxNTYyNSAyNS44MTY0MDYyNSAxMzQuNTE1NjI1IDI3LjQ0MTQwNjI1Wk0xMjQuOTE3OTY4NzUgMjcuNDQxNDA2MjVRMTI0LjkwNTI3MzQzNzUgMjguMzMwMDc4MTI1IDEyNS4zMTc4NzEwOTM3NSAyOC45Nzc1MzkwNjI1UTEyNS43MzA0Njg3NSAyOS42MjUgMTI2LjQ2MDQ0OTIxODc1IDI5Ljk2Nzc3MzQzNzVRMTI3LjE5MDQyOTY4NzUgMzAuMzEwNTQ2ODc1IDEyOC4xMTcxODc1IDMwLjMxMDU0Njg3NVExMjkuMDQzOTQ1MzEyNSAzMC4zMTA1NDY4NzUgMTI5Ljc3MzkyNTc4MTI1IDI5Ljk2Nzc3MzQzNzVRMTMwLjUwMzkwNjI1IDI5LjYyNSAxMzAuOTI5MTk5MjE4NzUgMjguOTc3NTM5MDYyNVExMzEuMzU0NDkyMTg3NSAyOC4zMzAwNzgxMjUgMTMxLjM2NzE4NzUgMjcuNDQxNDA2MjVRMTMxLjM1NDQ5MjE4NzUgMjYuNTUyNzM0Mzc1IDEzMC45MzU1NDY4NzUgMjUuOTA1MjczNDM3NVExMzAuNTE2NjAxNTYyNSAyNS4yNTc4MTI1IDEyOS43ODAyNzM0Mzc1IDI0LjkxNTAzOTA2MjVRMTI5LjA0Mzk0NTMxMjUgMjQuNTcyMjY1NjI1IDEyOC4xMTcxODc1IDI0LjU3MjI2NTYyNVExMjcuMjAzMTI1IDI0LjU3MjI2NTYyNSAxMjYuNDY2Nzk2ODc1IDI0LjkxNTAzOTA2MjVRMTI1LjczMDQ2ODc1IDI1LjI1NzgxMjUgMTI1LjMxNzg3MTA5Mzc1IDI1LjkxMTYyMTA5Mzc1UTEyNC45MDUyNzM0Mzc1IDI2LjU2NTQyOTY4NzUgMTI0LjkxNzk2ODc1IDI3LjQ0MTQwNjI1Wk0xNDAuMDUwNzgxMjUgNDMuMzg2NzE4NzVIMTM2LjgyNjE3MTg3NVYyMC4yMzA0Njg3NUgxNDAuMDUwNzgxMjVaTTEyMS45OTgwNDY4NzUgMzYuMDIzNDM3NVExMzAuMTM1NzQyMTg3NSAzNi4wMjM0Mzc1IDEzNS41MDU4NTkzNzUgMzUuMjg3MTA5Mzc1TDEzNS43NTk3NjU2MjUgMzcuNjIzMDQ2ODc1UTEzMi4zNTc0MjE4NzUgMzguMjgzMjAzMTI1IDEyOC44MTU0Mjk2ODc1IDM4LjQ3MzYzMjgxMjVRMTI1LjI3MzQzNzUgMzguNjY0MDYyNSAxMjEuMTA5Mzc1IDM4LjY2NDA2MjVMMTIwLjcyODUxNTYyNSAzNi4wMjM0Mzc1Wk0xNjEuOTQ1MzEyNSAzNC4xNDQ1MzEyNUgxNTguNjk1MzEyNVYyMC4yMzA0Njg3NUgxNjEuOTQ1MzEyNVpNMTYxLjk0NTMxMjUgNDMuMzM1OTM3NUgxNTguNjk1MzEyNVYzNy43NUgxNDYuMDUwNzgxMjVWMzUuMTYwMTU2MjVIMTYxLjk0NTMxMjVaTTE1Ni4yNTc4MTI1IDI0LjE2NjAxNTYyNUgxNTEuNjExMzI4MTI1UTE1MS42MjQwMjM0Mzc1IDI1LjU3NTE5NTMxMjUgMTUyLjIyNzA1MDc4MTI1IDI2Ljg5NTUwNzgxMjVRMTUyLjgzMDA3ODEyNSAyOC4yMTU4MjAzMTI1IDE1NC4wNjc4NzEwOTM3NSAyOS4yNTY4MzU5Mzc1UTE1NS4zMDU2NjQwNjI1IDMwLjI5Nzg1MTU2MjUgMTU3LjEyMTA5Mzc1IDMwLjg0Mzc1TDE1NS40NzA3MDMxMjUgMzMuMzU3NDIxODc1UTE1My41NzkxMDE1NjI1IDMyLjc3MzQzNzUgMTUyLjE5NTMxMjUgMzEuNTkyNzczNDM3NVExNTAuODExNTIzNDM3NSAzMC40MTIxMDkzNzUgMTUwLjAxMTcxODc1IDI4Ljc5OTgwNDY4NzVRMTQ5LjE5OTIxODc1IDMwLjU1MTc1NzgxMjUgMTQ3Ljc3NzM0Mzc1IDMxLjg0MDMzMjAzMTI1UTE0Ni4zNTU0Njg3NSAzMy4xMjg5MDYyNSAxNDQuMzc1IDMzLjc4OTA2MjVMMTQyLjY5OTIxODc1IDMxLjI3NTM5MDYyNVExNDQuNTI3MzQzNzUgMzAuNjc4NzEwOTM3NSAxNDUuNzkwNTI3MzQzNzUgMjkuNTY3ODcxMDkzNzVRMTQ3LjA1MzcxMDkzNzUgMjguNDU3MDMxMjUgMTQ3LjY3NTc4MTI1IDI3LjA1NDE5OTIxODc1UTE0OC4yOTc4NTE1NjI1IDI1LjY1MTM2NzE4NzUgMTQ4LjMxMDU0Njg3NSAyNC4xNjYwMTU2MjVIMTQzLjU2MjVWMjEuNTc2MTcxODc1SDE1Ni4yNTc4MTI1Wk0xNzcuNTE3NTc4MTI1IDIzLjM1MzUxNTYyNVExNzcuNTE3NTc4MTI1IDI1LjEzMDg1OTM3NSAxNzcuNDI4NzEwOTM3NSAyNi42ODYwMzUxNTYyNVExNzcuMzM5ODQzNzUgMjguMjQxMjEwOTM3NSAxNzYuOTg0Mzc1IDMwLjEzMjgxMjVMMTczLjgxMDU0Njg3NSAyOS44MjgxMjVRMTc0LjI5Mjk2ODc1IDI3LjE3NDgwNDY4NzUgMTc0LjM0Mzc1IDI0LjU0Njg3NUgxNjUuNjM0NzY1NjI1VjIxLjkwNjI1SDE3Ny41MTc1NzgxMjVaTTE3MS43NzkyOTY4NzUgMzEuNjY4OTQ1MzEyNVExNzUuNzE0ODQzNzUgMzEuNDkxMjEwOTM3NSAxNzguNjA5Mzc1IDMxLjA3MjI2NTYyNUwxNzguNzg3MTA5Mzc1IDMzLjQwODIwMzEyNVExNzUuNjAwNTg1OTM3NSAzMy45OTIxODc1IDE3Mi4xNDc0NjA5Mzc1IDM0LjE4MjYxNzE4NzVRMTY4LjY5NDMzNTkzNzUgMzQuMzczMDQ2ODc1IDE2NC41NDI5Njg3NSAzNC4zOTg0Mzc1TDE2NC4yNjM2NzE4NzUgMzEuODA4NTkzNzVRMTY3LjAxODU1NDY4NzUgMzEuODA4NTkzNzUgMTY4LjU1NDY4NzUgMzEuNzcwNTA3ODEyNVYyNy4yMzgyODEyNUgxNzEuNzc5Mjk2ODc1Wk0xODMuMTc5Njg3NSAyNy4yNjM2NzE4NzVIMTg1Ljk3MjY1NjI1VjI5Ljk4MDQ2ODc1SDE4My4xNzk2ODc1VjM3LjUyMTQ4NDM3NUgxNzkuODUzNTE1NjI1VjIwLjIzMDQ2ODc1SDE4My4xNzk2ODc1Wk0xODMuODkwNjI1IDQyLjkyOTY4NzVIMTY3LjY2NjAxNTYyNVYzNS45NzI2NTYyNUgxNzAuOTE2MDE1NjI1VjQwLjMzOTg0Mzc1SDE4My44OTA2MjVaIi8+Cjwvc3ZnPgo=" alt="오늘의직관">
  <div class="rule"></div>
  <h1>프로젝트 기술서</h1>
  <div class="desc">KBO 직관 기록 서비스</div>
  <div class="meta">
    작성자 · 울산 U133 이채목<br>작성일 · 2026-08-18<br>버전 · v1.3
  </div>
</div>
<div class="toc"><h2>목차</h2><ul>%s</ul></div>
""" % toc_html

    html = ("<!doctype html><html lang='ko'><head><meta charset='utf-8'>"
            "<title>프로젝트 기술서 - 오늘의직관</title>"
            "<style>%s</style></head><body>%s%s</body></html>"
            % (FONT_CSS + CSS, cover, "".join(parts)))

    os.makedirs(OUT, exist_ok=True)
    html_path = os.path.join(OUT, "_기술서.html")
    io.open(html_path, "w", encoding="utf-8").write(html)

    pdf_path = os.path.join(OUT, "프로젝트기술서_오늘의직관.pdf")
    # --print-to-pdf 로는 푸터 서식을 지정할 수 없어 쪽 번호가 안 들어간다.
    # DevTools 프로토콜을 쓰는 print_pdf.js 로 인쇄하고, 실패하면 기존 방식으로 넘어간다.
    printer = os.path.join(BASE, "tools", "print_pdf.js")
    scratch = "/private/tmp/claude-501/-Users-skalamok-Desktop-MokLab/0d11b94f-b02b-4142-916a-9dd02be32672/scratchpad"
    footer = "오늘의직관 · 프로젝트 기술서"
    r = subprocess.run(["node", printer, html_path, pdf_path, footer],
                       capture_output=True, text=True,
                       env=dict(os.environ, NODE_PATH=os.path.join(scratch, "node_modules")))
    if r.returncode != 0:
        print("  (쪽 번호 없이 생성:", r.stderr.strip()[:80], ")")
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

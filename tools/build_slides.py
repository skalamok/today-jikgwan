# -*- coding: utf-8 -*-
"""
발표 자료 생성기 (HTML → PDF)

구성안: docs/04_발표자료/발표_구성안.md
사용: python3 tools/build_slides.py
"""
import io, os, subprocess, sys

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(BASE, "docs", "_제출본")
WF = os.path.join(BASE, "docs", "_assets", "wireframes")
SC = os.path.join(BASE, "docs", "_assets", "screens")
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
FONT = os.path.join(BASE, "docs", "_assets", "font")

# 기술서와 같은 프리텐다드를 쓴다. 라틴 글자가 한글과 함께 설계돼 있어
# 영문 폰트를 따로 섞지 않는다.
FONT_CSS = "".join(
    "@font-face { font-family: Pretendard; font-weight: %d; font-style: normal;"
    " font-display: block; src: url('file://%s/Pretendard-%s.woff2') format('woff2'); }\n"
    % (w, FONT, n)
    for w, n in ((400, "Regular"), (500, "Medium"), (600, "SemiBold"), (700, "Bold"))
)

def img(path):
    return "file://" + os.path.join(BASE, path)

CSS = """
@page { size: 297mm 167mm; margin: 0; }
* { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: Pretendard,'Apple SD Gothic Neo','Malgun Gothic',sans-serif; color: #17191d; }

.s {
  width: 297mm; height: 167mm; padding: 14mm 18mm 11mm;
  page-break-after: always; position: relative;
  display: flex; flex-direction: column; background: #fff;
}
.s:last-child { page-break-after: auto; }
.s .num { position: absolute; right: 14mm; bottom: 7mm; font-size: 8pt; color: #9aa0a6; }
.s .tag { font-size: 8.5pt; letter-spacing: 3px; color: #8a9099; margin-bottom: 3mm; }
.s h2 { font-size: 21pt; letter-spacing: -0.6px; margin-bottom: 2.5mm; }
.s .lead { font-size: 11pt; color: #55595f; margin-bottom: 7mm; line-height: 1.5; }
.s .body { flex: 1; }

/* 표지 */
.cover { background: #17191d; color: #fff; align-items: center; justify-content: center; text-align: center; }
.cover .tag { color: #7d858f; }
.cover h1 { font-size: 44pt; letter-spacing: -2px; margin-bottom: 4mm; }
.cover .sub { font-size: 15pt; color: #c8ccd2; margin-bottom: 3mm; }
.cover .desc { font-size: 10.5pt; color: #8a9099; margin-bottom: 16mm; }
.cover .meta { font-size: 10pt; color: #c8ccd2; line-height: 1.9; }
.cover .rule { width: 40mm; border-top: 2px solid #fff; margin: 0 auto 7mm; }
.cover .logo { width: 92mm; margin: 0 auto 9mm; display: block; }

/* 레이아웃 */
.cols { display: grid; grid-template-columns: repeat(3, 1fr); gap: 7mm; }
.cols2 { display: grid; grid-template-columns: 1fr 1fr; gap: 8mm; align-items: start; }
.cols23 { display: grid; grid-template-columns: 1.1fr 1fr; gap: 8mm; align-items: start; }

.box { border: 1px solid #dfe3e8; border-radius: 3mm; padding: 5mm; background: #fbfcfd; }
.box.dark { background: #17191d; color: #fff; border-color: #17191d; }
.box h3 { font-size: 11pt; margin-bottom: 2.5mm; }
.box p, .box li { font-size: 9.5pt; color: #4a4f55; line-height: 1.6; }
.box.dark p, .box.dark li { color: #c8ccd2; }
.box ul { margin-left: 4mm; }

.big { font-size: 30pt; font-weight: 800; letter-spacing: -1px; }
.mid { font-size: 13pt; font-weight: 700; }
.small { font-size: 9pt; color: #6b7076; line-height: 1.6; }
.hl { background: #fff3cd; padding: 0 1mm; }
.bad { color: #c0392b; font-weight: 700; }
.good { color: #1e6fd9; font-weight: 700; }

table { width: 100%; border-collapse: collapse; font-size: 9pt; }
th, td { border: 1px solid #dfe3e8; padding: 2.2mm 2.6mm; text-align: left; vertical-align: top;
         word-break: keep-all; }
th { background: #eef1f5; font-weight: 700; }

.flow { display: flex; align-items: stretch; gap: 3mm; }
.flow .step { flex: 1; border: 1px solid #dfe3e8; border-radius: 2.5mm; padding: 4mm 3mm; text-align: center; }
.flow .step .n { font-size: 8pt; color: #8a9099; }
.flow .step .t { font-size: 10pt; font-weight: 700; margin: 1.5mm 0; }
.flow .step .d { font-size: 8pt; color: #6b7076; line-height: 1.45; }
.flow .arw { display: flex; align-items: center; color: #b6bcc4; font-size: 13pt; }

pre { background: #17191d; color: #e6e9ec; padding: 4mm 5mm; border-radius: 2.5mm;
      font-family: 'SFMono-Regular',Consolas,monospace; font-size: 8.5pt; line-height: 1.6; }
pre .ok { color: #6ee7a8; }
pre .cm { color: #8a9099; }

img { display: block; max-width: 100%; }
.shot { border: 1px solid #dfe3e8; border-radius: 2mm; width: 100%; }
.wf { border: 1px solid #dfe3e8; height: 78mm; margin: 0 auto; }
"""

S = []
def slide(cls, html):
    S.append('<section class="s %s">%s<div class="num">%d</div></section>' % (cls, html, len(S) + 1))

def head(tag, title, lead=""):
    return ('<div class="tag">%s</div><h2>%s</h2>%s'
            % (tag, title, ('<div class="lead">%s</div>' % lead) if lead else ""))

# 1 표지
S.append("""<section class="s cover">
<div class="tag">SKALA MINI PROJECT</div>
<img class="logo" src="data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxODggNjQiIHJvbGU9ImltZyIgYXJpYS1sYWJlbD0i7Jik64qY7J2Y7KeB6rSAIj4KICA8IS0tIHRvb2xzL2J1aWxkX2xvZ28ucHkg6rCAIOyDneyEse2VnOuLpC4g7KeB7KCRIOqzoOy5mOyngCDslYrripTri6QuIC0tPgogIDxwYXRoIGZpbGw9IiMxNjM1NWMiIGZpbGwtcnVsZT0iZXZlbm9kZCIgZD0iCiAgICBNOCAxNGg0OGE0IDQgMCAwIDEgNCA0djcuNWE2LjUgNi41IDAgMCAwIDAgMTNWNDZhNCA0IDAgMCAxLTQgNEg4YTQgNCAwIDAgMS00LTR2LTcuNWE2LjUgNi41IDAgMCAwIDAtMTNWMThhNCA0IDAgMCAxIDQtNFoKICAgIE0yMy40IDIxLjZhMiAyIDAgMCAwLTIuOCAyLjhjMy45IDMuOSAzLjkgMTEuMiAwIDE1LjJhMiAyIDAgMSAwIDIuOCAyLjhjNS40LTUuNCA1LjQtMTUuNCAwLTIwLjhaCiAgICBNNDMuNCAyMS42Yy01LjQgNS40LTUuNCAxNS40IDAgMjAuOGEyIDIgMCAwIDAgMi44LTIuOGMtMy45LTQtMy45LTExLjMgMC0xNS4yYTIgMiAwIDAgMC0yLjgtMi44WgogICIvPgogIDxwYXRoIGZpbGw9IiMxNjM1NWMiIGQ9Ik05NS45MzE2NDA2MjUgMjcuNzQ2MDkzNzVROTUuOTE4OTQ1MzEyNSAyOS40NDcyNjU2MjUgOTUuMDE3NTc4MTI1IDMwLjc5Mjk2ODc1UTk0LjExNjIxMDkzNzUgMzIuMTM4NjcxODc1IDkyLjUxMDI1MzkwNjI1IDMyLjk3MDIxNDg0Mzc1UTkwLjkwNDI5Njg3NSAzMy44MDE3NTc4MTI1IDg4LjgyMjI2NTYyNSAzNC4wNDI5Njg3NVYzNy45Nzg1MTU2MjVIOTcuODM1OTM3NVY0MC42NDQ1MzEyNUg3Ni42ODU1NDY4NzVWMzcuOTc4NTE1NjI1SDg1LjU5NzY1NjI1VjM0LjAzMDI3MzQzNzVRODMuNTI4MzIwMzEyNSAzMy44MDE3NTc4MTI1IDgxLjkyODcxMDkzNzUgMzIuOTYzODY3MTg3NVE4MC4zMjkxMDE1NjI1IDMyLjEyNTk3NjU2MjUgNzkuNDM0MDgyMDMxMjUgMzAuNzg2NjIxMDkzNzVRNzguNTM5MDYyNSAyOS40NDcyNjU2MjUgNzguNTM5MDYyNSAyNy43NDYwOTM3NVE3OC41MzkwNjI1IDI1Ljg3OTg4MjgxMjUgNzkuNjYyNTk3NjU2MjUgMjQuNDM4OTY0ODQzNzVRODAuNzg2MTMyODEyNSAyMi45OTgwNDY4NzUgODIuNzY2NjAxNTYyNSAyMi4yMTA5Mzc1UTg0Ljc0NzA3MDMxMjUgMjEuNDIzODI4MTI1IDg3LjIyMjY1NjI1IDIxLjQyMzgyODEyNVE4OS42OTgyNDIxODc1IDIxLjQyMzgyODEyNSA5MS42Nzg3MTA5Mzc1IDIyLjIxMDkzNzVROTMuNjU5MTc5Njg3NSAyMi45OTgwNDY4NzUgOTQuNzg5MDYyNSAyNC40Mzg5NjQ4NDM3NVE5NS45MTg5NDUzMTI1IDI1Ljg3OTg4MjgxMjUgOTUuOTMxNjQwNjI1IDI3Ljc0NjA5Mzc1Wk04MS43MzgyODEyNSAyNy43NDYwOTM3NVE4MS43MzgyODEyNSAyOC45Mzk0NTMxMjUgODIuNDMwMTc1NzgxMjUgMjkuNzk2Mzg2NzE4NzVRODMuMTIyMDcwMzEyNSAzMC42NTMzMjAzMTI1IDg0LjM1OTg2MzI4MTI1IDMxLjEwNDAwMzkwNjI1UTg1LjU5NzY1NjI1IDMxLjU1NDY4NzUgODcuMjIyNjU2MjUgMzEuNTU0Njg3NVE4OC44NjAzNTE1NjI1IDMxLjU1NDY4NzUgOTAuMDk4MTQ0NTMxMjUgMzEuMTEwMzUxNTYyNVE5MS4zMzU5Mzc1IDMwLjY2NjAxNTYyNSA5Mi4wMjc4MzIwMzEyNSAyOS44MDI3MzQzNzVROTIuNzE5NzI2NTYyNSAyOC45Mzk0NTMxMjUgOTIuNzMyNDIxODc1IDI3Ljc0NjA5Mzc1UTkyLjcxOTcyNjU2MjUgMjYuNjAzNTE1NjI1IDkyLjAyNzgzMjAzMTI1IDI1Ljc1OTI3NzM0Mzc1UTkxLjMzNTkzNzUgMjQuOTE1MDM5MDYyNSA5MC4wOTE3OTY4NzUgMjQuNDY0MzU1NDY4NzVRODguODQ3NjU2MjUgMjQuMDEzNjcxODc1IDg3LjIyMjY1NjI1IDI0LjAxMzY3MTg3NVE4NS42MTAzNTE1NjI1IDI0LjAxMzY3MTg3NSA4NC4zNjYyMTA5Mzc1IDI0LjQ2NDM1NTQ2ODc1UTgzLjEyMjA3MDMxMjUgMjQuOTE1MDM5MDYyNSA4Mi40MzAxNzU3ODEyNSAyNS43NjU2MjVRODEuNzM4MjgxMjUgMjYuNjE2MjEwOTM3NSA4MS43MzgyODEyNSAyNy43NDYwOTM3NVpNMTE3LjM0Mzc1IDI3LjQ2Njc5Njg3NUgxMDEuMjcxNDg0Mzc1VjIwLjUwOTc2NTYyNUgxMDQuNDk2MDkzNzVWMjQuOTAyMzQzNzVIMTE3LjM0Mzc1Wk0xMTkuNzgxMjUgMzEuNTgwMDc4MTI1SDk4LjYzMDg1OTM3NVYyOS4wMTU2MjVIMTE5Ljc4MTI1Wk0xMTcuMjQyMTg3NSAzOS4xMjEwOTM3NUgxMDQuMzQzNzVWNDAuNTkzNzVIMTE3Ljk1MzEyNVY0My4wODIwMzEyNUgxMDEuMDkzNzVWMzYuNzg1MTU2MjVIMTE0LjAxNzU3ODEyNVYzNS40NjQ4NDM3NUgxMDEuMDQyOTY4NzVWMzIuOTc2NTYyNUgxMTcuMjQyMTg3NVpNMTM0LjUxNTYyNSAyNy40NDE0MDYyNVExMzQuNTE1NjI1IDI5LjA3OTEwMTU2MjUgMTMzLjY4NDA4MjAzMTI1IDMwLjM3NDAyMzQzNzVRMTMyLjg1MjUzOTA2MjUgMzEuNjY4OTQ1MzEyNSAxMzEuMzkyNTc4MTI1IDMyLjM5ODkyNTc4MTI1UTEyOS45MzI2MTcxODc1IDMzLjEyODkwNjI1IDEyOC4xMTcxODc1IDMzLjEyODkwNjI1UTEyNi4zMDE3NTc4MTI1IDMzLjEyODkwNjI1IDEyNC44MzU0NDkyMTg3NSAzMi4zOTg5MjU3ODEyNVExMjMuMzY5MTQwNjI1IDMxLjY2ODk0NTMxMjUgMTIyLjUzMTI1IDMwLjM3NDAyMzQzNzVRMTIxLjY5MzM1OTM3NSAyOS4wNzkxMDE1NjI1IDEyMS42OTMzNTkzNzUgMjcuNDQxNDA2MjVRMTIxLjY5MzM1OTM3NSAyNS44MTY0MDYyNSAxMjIuNTMxMjUgMjQuNTIxNDg0Mzc1UTEyMy4zNjkxNDA2MjUgMjMuMjI2NTYyNSAxMjQuODQxNzk2ODc1IDIyLjQ5NjU4MjAzMTI1UTEyNi4zMTQ0NTMxMjUgMjEuNzY2NjAxNTYyNSAxMjguMTE3MTg3NSAyMS43NzkyOTY4NzVRMTI5LjkxOTkyMTg3NSAyMS43NjY2MDE1NjI1IDEzMS4zODYyMzA0Njg3NSAyMi40OTY1ODIwMzEyNVExMzIuODUyNTM5MDYyNSAyMy4yMjY1NjI1IDEzMy42ODQwODIwMzEyNSAyNC41MjE0ODQzNzVRMTM0LjUxNTYyNSAyNS44MTY0MDYyNSAxMzQuNTE1NjI1IDI3LjQ0MTQwNjI1Wk0xMjQuOTE3OTY4NzUgMjcuNDQxNDA2MjVRMTI0LjkwNTI3MzQzNzUgMjguMzMwMDc4MTI1IDEyNS4zMTc4NzEwOTM3NSAyOC45Nzc1MzkwNjI1UTEyNS43MzA0Njg3NSAyOS42MjUgMTI2LjQ2MDQ0OTIxODc1IDI5Ljk2Nzc3MzQzNzVRMTI3LjE5MDQyOTY4NzUgMzAuMzEwNTQ2ODc1IDEyOC4xMTcxODc1IDMwLjMxMDU0Njg3NVExMjkuMDQzOTQ1MzEyNSAzMC4zMTA1NDY4NzUgMTI5Ljc3MzkyNTc4MTI1IDI5Ljk2Nzc3MzQzNzVRMTMwLjUwMzkwNjI1IDI5LjYyNSAxMzAuOTI5MTk5MjE4NzUgMjguOTc3NTM5MDYyNVExMzEuMzU0NDkyMTg3NSAyOC4zMzAwNzgxMjUgMTMxLjM2NzE4NzUgMjcuNDQxNDA2MjVRMTMxLjM1NDQ5MjE4NzUgMjYuNTUyNzM0Mzc1IDEzMC45MzU1NDY4NzUgMjUuOTA1MjczNDM3NVExMzAuNTE2NjAxNTYyNSAyNS4yNTc4MTI1IDEyOS43ODAyNzM0Mzc1IDI0LjkxNTAzOTA2MjVRMTI5LjA0Mzk0NTMxMjUgMjQuNTcyMjY1NjI1IDEyOC4xMTcxODc1IDI0LjU3MjI2NTYyNVExMjcuMjAzMTI1IDI0LjU3MjI2NTYyNSAxMjYuNDY2Nzk2ODc1IDI0LjkxNTAzOTA2MjVRMTI1LjczMDQ2ODc1IDI1LjI1NzgxMjUgMTI1LjMxNzg3MTA5Mzc1IDI1LjkxMTYyMTA5Mzc1UTEyNC45MDUyNzM0Mzc1IDI2LjU2NTQyOTY4NzUgMTI0LjkxNzk2ODc1IDI3LjQ0MTQwNjI1Wk0xNDAuMDUwNzgxMjUgNDMuMzg2NzE4NzVIMTM2LjgyNjE3MTg3NVYyMC4yMzA0Njg3NUgxNDAuMDUwNzgxMjVaTTEyMS45OTgwNDY4NzUgMzYuMDIzNDM3NVExMzAuMTM1NzQyMTg3NSAzNi4wMjM0Mzc1IDEzNS41MDU4NTkzNzUgMzUuMjg3MTA5Mzc1TDEzNS43NTk3NjU2MjUgMzcuNjIzMDQ2ODc1UTEzMi4zNTc0MjE4NzUgMzguMjgzMjAzMTI1IDEyOC44MTU0Mjk2ODc1IDM4LjQ3MzYzMjgxMjVRMTI1LjI3MzQzNzUgMzguNjY0MDYyNSAxMjEuMTA5Mzc1IDM4LjY2NDA2MjVMMTIwLjcyODUxNTYyNSAzNi4wMjM0Mzc1Wk0xNjEuOTQ1MzEyNSAzNC4xNDQ1MzEyNUgxNTguNjk1MzEyNVYyMC4yMzA0Njg3NUgxNjEuOTQ1MzEyNVpNMTYxLjk0NTMxMjUgNDMuMzM1OTM3NUgxNTguNjk1MzEyNVYzNy43NUgxNDYuMDUwNzgxMjVWMzUuMTYwMTU2MjVIMTYxLjk0NTMxMjVaTTE1Ni4yNTc4MTI1IDI0LjE2NjAxNTYyNUgxNTEuNjExMzI4MTI1UTE1MS42MjQwMjM0Mzc1IDI1LjU3NTE5NTMxMjUgMTUyLjIyNzA1MDc4MTI1IDI2Ljg5NTUwNzgxMjVRMTUyLjgzMDA3ODEyNSAyOC4yMTU4MjAzMTI1IDE1NC4wNjc4NzEwOTM3NSAyOS4yNTY4MzU5Mzc1UTE1NS4zMDU2NjQwNjI1IDMwLjI5Nzg1MTU2MjUgMTU3LjEyMTA5Mzc1IDMwLjg0Mzc1TDE1NS40NzA3MDMxMjUgMzMuMzU3NDIxODc1UTE1My41NzkxMDE1NjI1IDMyLjc3MzQzNzUgMTUyLjE5NTMxMjUgMzEuNTkyNzczNDM3NVExNTAuODExNTIzNDM3NSAzMC40MTIxMDkzNzUgMTUwLjAxMTcxODc1IDI4Ljc5OTgwNDY4NzVRMTQ5LjE5OTIxODc1IDMwLjU1MTc1NzgxMjUgMTQ3Ljc3NzM0Mzc1IDMxLjg0MDMzMjAzMTI1UTE0Ni4zNTU0Njg3NSAzMy4xMjg5MDYyNSAxNDQuMzc1IDMzLjc4OTA2MjVMMTQyLjY5OTIxODc1IDMxLjI3NTM5MDYyNVExNDQuNTI3MzQzNzUgMzAuNjc4NzEwOTM3NSAxNDUuNzkwNTI3MzQzNzUgMjkuNTY3ODcxMDkzNzVRMTQ3LjA1MzcxMDkzNzUgMjguNDU3MDMxMjUgMTQ3LjY3NTc4MTI1IDI3LjA1NDE5OTIxODc1UTE0OC4yOTc4NTE1NjI1IDI1LjY1MTM2NzE4NzUgMTQ4LjMxMDU0Njg3NSAyNC4xNjYwMTU2MjVIMTQzLjU2MjVWMjEuNTc2MTcxODc1SDE1Ni4yNTc4MTI1Wk0xNzcuNTE3NTc4MTI1IDIzLjM1MzUxNTYyNVExNzcuNTE3NTc4MTI1IDI1LjEzMDg1OTM3NSAxNzcuNDI4NzEwOTM3NSAyNi42ODYwMzUxNTYyNVExNzcuMzM5ODQzNzUgMjguMjQxMjEwOTM3NSAxNzYuOTg0Mzc1IDMwLjEzMjgxMjVMMTczLjgxMDU0Njg3NSAyOS44MjgxMjVRMTc0LjI5Mjk2ODc1IDI3LjE3NDgwNDY4NzUgMTc0LjM0Mzc1IDI0LjU0Njg3NUgxNjUuNjM0NzY1NjI1VjIxLjkwNjI1SDE3Ny41MTc1NzgxMjVaTTE3MS43NzkyOTY4NzUgMzEuNjY4OTQ1MzEyNVExNzUuNzE0ODQzNzUgMzEuNDkxMjEwOTM3NSAxNzguNjA5Mzc1IDMxLjA3MjI2NTYyNUwxNzguNzg3MTA5Mzc1IDMzLjQwODIwMzEyNVExNzUuNjAwNTg1OTM3NSAzMy45OTIxODc1IDE3Mi4xNDc0NjA5Mzc1IDM0LjE4MjYxNzE4NzVRMTY4LjY5NDMzNTkzNzUgMzQuMzczMDQ2ODc1IDE2NC41NDI5Njg3NSAzNC4zOTg0Mzc1TDE2NC4yNjM2NzE4NzUgMzEuODA4NTkzNzVRMTY3LjAxODU1NDY4NzUgMzEuODA4NTkzNzUgMTY4LjU1NDY4NzUgMzEuNzcwNTA3ODEyNVYyNy4yMzgyODEyNUgxNzEuNzc5Mjk2ODc1Wk0xODMuMTc5Njg3NSAyNy4yNjM2NzE4NzVIMTg1Ljk3MjY1NjI1VjI5Ljk4MDQ2ODc1SDE4My4xNzk2ODc1VjM3LjUyMTQ4NDM3NUgxNzkuODUzNTE1NjI1VjIwLjIzMDQ2ODc1SDE4My4xNzk2ODc1Wk0xODMuODkwNjI1IDQyLjkyOTY4NzVIMTY3LjY2NjAxNTYyNVYzNS45NzI2NTYyNUgxNzAuOTE2MDE1NjI1VjQwLjMzOTg0Mzc1SDE4My44OTA2MjVaIi8+Cjwvc3ZnPgo=" alt="오늘의직관">
<div class="rule"></div>
<h1>오늘의직관</h1>
<div class="sub">KBO 직관 기록 서비스</div>
<div class="desc">직관한 경기를 기록하면 그대로 나만의 전적이 된다</div>
<div class="meta">울산 U133 이채목<br>2026.08.20</div>
</section>""")

# 2 문제 정의
slide("", head("PROBLEM", "직관을 다녀오면, 아무것도 남지 않는다",
               "티켓은 어딘가에 두고 잃어버린다. 사진은 갤러리에, 후기는 인스타에 흩어진다.") + """
<div class="body">
  <div class="cols">
    <div class="box"><h3>🎟️ 티켓</h3><p>실물로 서랍에 쌓인다.<br>언제 어느 경기였는지<br>시간이 지나면 알 수 없다</p></div>
    <div class="box"><h3>📷 사진</h3><p>갤러리에 수천 장 중<br>하나로 묻힌다.<br>찾으려면 날짜를 기억해야 한다</p></div>
    <div class="box"><h3>📝 후기</h3><p>인스타에 남는다.<br>어느 경기 이야기인지<br>사진·티켓과 연결이 끊긴다</p></div>
  </div>
  <div class="box dark" style="margin-top:7mm">
    <h3>그래서 생기는 진짜 문제</h3>
    <p style="font-size:11pt">개별 경기 결과는 어디서든 볼 수 있다. 그런데
    <span style="color:#ffd666; font-weight:700">내가 간 경기만 골라낸 전적</span>은 본인이 직접 세지 않으면 만들어지지 않는다.<br>
    팬들이 말하는 "내가 가면 이긴다"도 그래서 확인할 방법 없는 체감에 머문다.</p>
  </div>
</div>""")

# 3 서비스 소개
slide("", head("SOLUTION", "기록을 모으고, 데이터를 되돌려준다") + """
<div class="body cols23">
  <div>
    <div class="box" style="margin-bottom:5mm">
      <h3>① 기록을 한 곳에 모은다</h3>
      <p>경기 · 좌석 구역 · 사진 · 메모 · 비용 · 직관 메이트를 한 기록으로 저장</p>
    </div>
    <div class="box" style="margin-bottom:5mm">
      <h3>② 기록에서 전적을 만든다</h3>
      <p>구장별 · 상대팀별 · 요일별 승률, 연승 기록을 자동 집계</p>
    </div>
    <div class="box dark">
      <h3>③ 그 데이터를 다음 사람에게 돌려준다</h3>
      <p>기록할 때 받는 <b style="color:#ffd666">구역 만족도 한 문항</b>이 모여<br>
      처음 가는 사람의 좌석 선택 정보가 된다</p>
    </div>
  </div>
  <div>
    <img class="shot" src="%s">
    <div class="small" style="margin-top:3mm">6명이 평가한 1루 응원석만 평균이 표시되고,
    표본이 부족한 구역은 "평가 부족"으로 남는다</div>
  </div>
</div>""" % img("docs/_assets/screens/app-park.png"))

# 4 사용자 여정
slide("", head("USER JOURNEY", "처음 가는 팬이 자주 가는 팬이 되는 순환",
               "기록이 쌓여야 가치가 생기는 서비스는, 1년에 한두 번 가는 사람에게 줄 것이 없다.") + """
<div class="body">
  <div class="flow">
    <div class="step"><div class="n">STEP 1</div><div class="t">유입</div><div class="d">처음 가는데<br>자리를 모르겠음<br>→ 구장 상세</div></div>
    <div class="arw">›</div>
    <div class="step"><div class="n">STEP 2</div><div class="t">관람</div><div class="d">다녀옴</div></div>
    <div class="arw">›</div>
    <div class="step"><div class="n">STEP 3</div><div class="t">기록</div><div class="d">사진·메모<br>구역 평가</div></div>
    <div class="arw">›</div>
    <div class="step"><div class="n">STEP 4</div><div class="t">보상</div><div class="d">데이터가 없어도<br>배지·카드로<br>즉시 성취</div></div>
    <div class="arw">›</div>
    <div class="step"><div class="n">STEP 5</div><div class="t">정착</div><div class="d">전적 심화<br>메이트·계획</div></div>
  </div>
  <div class="cols2" style="margin-top:8mm">
    <div class="box"><h3>소비자로 들어와서</h3>
      <p>처음 가는 팬은 앞선 사용자들이 쌓은 구역 만족도를 보고 자리를 고른다</p></div>
    <div class="box"><h3>생산자가 되어 나간다</h3>
      <p>다녀와서 남긴 평가 한 줄이 다음 사람의 정보가 된다.<br>
      <b>사용자가 늘수록 구장 정보가 정확해지는 구조</b></p></div>
  </div>
</div>""")

# 5 산출물
slide("", head("DELIVERABLES", "산출물") + """
<div class="body">
  <div class="cols">
    <div class="box"><div class="big">79<span style="font-size:12pt">+24</span></div>
      <div class="mid" style="margin-top:2mm">요구사항</div>
      <p class="small">기능 79 · 비기능 24<br>MVP 33 · 확장 46</p></div>
    <div class="box"><div class="big">24<span style="font-size:12pt">/7</span></div>
      <div class="mid" style="margin-top:2mm">화면</div>
      <p class="small">전체 24개 정의<br>핵심 7개 상세 설계</p></div>
    <div class="box"><div class="big">27</div>
      <div class="mid" style="margin-top:2mm">테이블</div>
      <p class="small">7개 기능 영역<br>사전 집계 테이블 3</p></div>
  </div>
  <div class="cols" style="margin-top:6mm">
    <div class="box"><div class="big">55</div>
      <div class="mid" style="margin-top:2mm">API 오퍼레이션</div>
      <p class="small">경로 43 · MVP 25 · 확장 30<br>OpenAPI 3.0</p></div>
    <div class="box"><div class="big">54</div>
      <div class="mid" style="margin-top:2mm">기술서 쪽수</div>
      <p class="small">개요 · 요구사항 · 화면 · 데이터 · API</p></div>
    <div class="box dark"><div class="big">6/6</div>
      <div class="mid" style="margin-top:2mm">설계 판단 지점</div>
      <p class="small">파생 지표 · 소표본 · 동시성<br>이력 · 제약 편성 · 외부 데이터</p></div>
  </div>
</div>""")

# 6 요구사항 정의서
slide("", head("REQUIREMENTS", "요구사항 정의서 — 추적성",
               "요구사항 ID · 화면 ID · API 를 양방향으로 연결해 누락과 중복을 막는다.") + """
<div class="body cols23">
  <div>
    <table>
      <tr><th>대역</th><th>그룹</th><th>대역</th><th>그룹</th></tr>
      <tr><td>0xx</td><td>회원</td><td>4xx</td><td>관람 계획</td></tr>
      <tr><td>1xx</td><td>경기 · 구장</td><td>5xx</td><td>직관 메이트</td></tr>
      <tr><td>2xx</td><td>직관 기록</td><td>6xx</td><td>운영</td></tr>
      <tr><td>3xx</td><td>전적 집계</td><td>7xx</td><td>온보딩 · 성장</td></tr>
    </table>
    <div class="small" style="margin-top:3mm">
      순번이 아닌 <b>대역</b>으로 나눈 이유 — 나중에 요구사항이 추가돼도 기존 ID 를 건드리지 않는다.
      실제로 설계 검증 중 REQ-F-606~608 을 추가했으나 다른 번호는 그대로 유지됐다.
    </div>
  </div>
  <div class="box">
    <h3>추적 예시 — 소표본 표시 정책</h3>
    <table style="font-size:8.5pt">
      <tr><th>단계</th><th>내용</th></tr>
      <tr><td>요구사항</td><td>REQ-F-305</td></tr>
      <tr><td>화면</td><td>SCR-STAT-001 ⑤<br>SCR-PARK-002 ②</td></tr>
      <tr><td>API</td><td><code>winRate: null</code><br><code>smallSample: true</code></td></tr>
      <tr><td>DB</td><td><code>zone_stats.rating_count</code></td></tr>
    </table>
  </div>
</div>""")

# 7 화면 설계서
slide("", head("SCREEN DESIGN", "화면 설계서 — 요소마다 동작 조건을 정의") + """
<div class="body cols23">
  <div style="text-align:center">
    <img class="wf" src="%s">
    <div class="small" style="margin-top:2mm">SCR-LOG-001 직관 기록 작성</div>
  </div>
  <div>
    <table style="font-size:8.5pt">
      <tr><th>NO</th><th>요소</th><th>동작 조건</th></tr>
      <tr><td>①</td><td>사진</td><td>위치 메타데이터 제거 후 저장</td></tr>
      <tr><td>②</td><td>경기 선택</td><td>촬영 일시로 자동 추천</td></tr>
      <tr><td>③</td><td>응원팀</td><td>중립 선택 시 승패 집계 제외</td></tr>
      <tr><td>⑤</td><td>구역 만족도</td><td><b>필수</b> — 구장 집계의 입력원</td></tr>
      <tr><td>⑪</td><td>저장</td><td>②③④⑤ 입력 시 활성화<br>중복 시 409</td></tr>
    </table>
    <div class="small" style="margin-top:3mm">
      그림만 그리지 않고 <b>초기 상태 · 사용자 액션 · 화면 전환 · 예외 처리</b>를 함께 정의했다.
      각 행에는 매핑 요구사항 ID 를 함께 기재한다.
    </div>
  </div>
</div>""" % img("docs/_assets/wireframes/SCR-LOG-001.png"))

# 8 데이터 모델링
slide("", head("DATA MODEL", "데이터 모델링 — 핵심 설계 결정") + """
<div class="body cols2">
  <div>
    <div class="box" style="margin-bottom:4mm">
      <h3>중립 관람을 NULL 로 표현</h3>
      <p><code>is_neutral</code> 플래그를 두지 않고 <code>cheer_team_id</code> 를 nullable 로.
      플래그와 팀 ID 를 함께 두면 "중립인데 팀이 지정된" <b>모순 상태가 만들어질 수 있다</b></p>
    </div>
    <div class="box" style="margin-bottom:4mm">
      <h3>응원팀 변경을 소급하지 않는다</h3>
      <p>기록마다 <b>작성 시점의</b> 응원팀을 보관.
      프로필에서 응원팀을 바꿔도 과거 기록의 승패가 뒤집히지 않는다</p>
    </div>
    <div class="box">
      <h3>평균 대신 합계와 개수를 저장</h3>
      <p><code>zone_stats</code> 는 <code>rating_sum</code> · <code>rating_count</code> 보관.
      새 평가는 더하기만 하면 되고, <code>rating_count</code> 가 소표본 판정에 그대로 쓰인다</p>
    </div>
  </div>
  <div>
    <table style="font-size:8.5pt">
      <tr><th>도메인</th><th>테이블</th></tr>
      <tr><td>회원</td><td>users, user_social_accounts,<br>auth_tokens, user_team_history</td></tr>
      <tr><td>경기 · 구장</td><td>teams, stadiums, stadium_zones,<br>games, game_result_reports,<br>game_revisions</td></tr>
      <tr><td><b>직관 기록</b></td><td><b>attendance_logs</b>, attendance_photos,<br>attendance_companions</td></tr>
      <tr><td>집계</td><td>user_stats, user_streaks, zone_stats</td></tr>
      <tr><td>직관 메이트</td><td>companion_posts,<br>companion_applications</td></tr>
      <tr><td>계획 · 성장 · 알림</td><td>viewing_plans(3), badges(2),<br>notifications</td></tr>
    </table>
    <div class="small" style="margin-top:3mm">전체 ERD 는 제출한 <code>schema.dbml</code> 참조</div>
  </div>
</div>""")

# 9 설계 결정 1 — 데이터 출처
slide("", head("DESIGN DECISION 1", "외부 데이터를 쓸 수 있는지부터 확인했다",
               "기획대로라면 KBO 경기 데이터를 가져와야 했다. 그래서 먼저 확인했다.") + """
<div class="body">
  <div class="flow">
    <div class="step"><div class="n">STEP 1</div><div class="t">이용약관</div>
      <div class="d">KBO 제14조 차항<br><span class="bad">자동화 수집 금지</span></div></div>
    <div class="arw">›</div>
    <div class="step"><div class="n">STEP 2</div><div class="t">robots.txt</div>
      <div class="d">KBO · STATIZ 모두<br><span class="bad">User-agent: *<br>Disallow: /</span></div></div>
    <div class="arw">›</div>
    <div class="step"><div class="n">STEP 3</div><div class="t">권리자 문의</div>
      <div class="d">STATIZ 운영자<br><span class="bad">개인용 경로 없음</span></div></div>
    <div class="arw">›</div>
    <div class="step"><div class="n">STEP 4</div><div class="t">대안 탐색</div>
      <div class="d">TheSportsDB · 기상청<br><span class="good">약관상 이용 허용</span></div></div>
  </div>

  <div class="box dark" style="margin-top:7mm">
    <h3>STATIZ 문의 결과 (2026-08-18)</h3>
    <p style="font-size:10pt">공개된 안내는 "출처를 명시하면 데이터를 이용할 수 있다"였다.
    어디까지 적용되는지 불분명하여 운영자에게 직접 문의한 결과,
    <b style="color:#ffd666">크롤링을 금지하며 개인 이용자에게 제공할 수 있는 API 도 없다</b>는 답변을 받았다.
    수집 방법을 조정해 해결될 문제가 아니라 개인이 이용할 정식 경로 자체가 없었다.</p>
  </div>

  <div class="cols2" style="margin-top:5mm">
    <div class="box"><h3>결정</h3><p>웹 페이지 수집은 배제하고 <b>경기 결과는 운영자만 등록·정정</b>하도록 했다.
    외부 경로는 데이터 제공 인터페이스로 추상화해 선택을 뒤로 미뤘다</p></div>
    <div class="box"><h3>배운 것</h3><p>공개된 안내 문구만으로 이용 가능 여부를 판단하면 안 된다.
    <b>권리자에게 직접 확인하는 절차</b>가 필요하다</p></div>
  </div>
</div>""")

# 10 설계 결정 2 — 소표본
slide("", head("DESIGN DECISION 2", "승률을 언제부터 보여줄 것인가") + """
<div class="body">
  <div class="cols2">
    <div class="box" style="border-color:#b4c8e8; background:#f6f9fd">
      <h3 class="good">통산은 첫 기록부터 보여준다</h3>
      <p style="font-size:12pt"><b>1경기 1승 · 승률 1.000</b></p>
      <p style="margin-top:2mm">모수(1경기)가 함께 보여 오해가 적다.
      <b>첫 기록에서 바로 성취가 보이는 편이 다음 기록을 남길 이유가 된다</b></p>
    </div>
    <div class="box" style="border-color:#e8b4b4; background:#fdf6f6">
      <h3 class="bad">쪼갠 집계는 다르다</h3>
      <p style="font-size:12pt">잠실 <b>2경기 2승 1.000</b> · 고척 <b>1경기 1패 0.000</b></p>
      <p style="margin-top:2mm">나란히 놓이면 순위처럼 읽힌다. 실제로는 아무 의미 없는 서열이다.
      표본 5경기 미만은 <b>승률을 산출하지 않고</b> 전적만 표시한다</p>
    </div>
  </div>

  <div class="box" style="margin-top:6mm">
    <h3>이 정책이 4개 층을 관통한다</h3>
    <table style="margin-top:2mm">
      <tr><th>요구사항</th><th>화면 설계</th><th>API</th><th>데이터 모델</th></tr>
      <tr>
        <td>REQ-F-305<br>차원별 기준 5경기</td>
        <td>SCR-STAT-001 ⑤<br>SCR-PARK-002 ②</td>
        <td><code>winRate: null</code><br><code>smallSample: true</code></td>
        <td><code>zone_stats</code><br><code>.rating_count</code></td>
      </tr>
    </table>
    <div class="small" style="margin-top:3mm">
      평균값 하나만 저장했다면 표본 수를 알 수 없어 이 정책을 만들 수 없다.
      <b>합계와 개수를 나눠 저장한 설계가 화면 정책을 가능하게 한다</b>
    </div>
  </div>
</div>""")

# 11 설계 결정 3 — 동시성
slide("", head("DESIGN DECISION 3", "30명이 동시에 눌러도 정원은 넘지 않는다") + """
<div class="body cols23">
  <div>
    <div class="box" style="margin-bottom:4mm">
      <h3>문제</h3>
      <p>메이트 모집 정원이 3명인데 여러 명이 같은 순간 신청을 누르면
      확인 후 증가시키는 방식은 <b>정원을 넘긴다</b></p>
    </div>
    <div class="box">
      <h3>3중 방어</h3>
      <table style="font-size:8.5pt; margin-top:2mm">
        <tr><td style="width:8mm">①</td><td>애플리케이션 정원 검사</td></tr>
        <tr><td>②</td><td>낙관적 락(version) 충돌 감지 후 재시도</td></tr>
        <tr><td>③</td><td>DB CHECK 제약 (최종 방어선)</td></tr>
      </table>
      <div class="small" style="margin-top:3mm">
        재시도는 새 트랜잭션이어야 하므로 실행부를 <b>별도 빈으로 분리</b>했다.
        같은 빈에서 자기 메서드를 부르면 프록시를 거치지 않아 적용되지 않는다
      </div>
    </div>
  </div>
  <div>
    <div class="small" style="margin-bottom:2mm"><b>검증 — 정원 4명(작성자 포함)에 30명 동시 요청</b></div>
    <pre><span class="cm">[동시성 결과]</span> 도전 30명 → 성공 3 / 거절 27
<span class="cm">[확정 순번]</span> <span class="ok">[2, 3, 4]</span>
<span class="cm">[게시글]</span> confirmedCount=<span class="ok">4</span> capacity=<span class="ok">4</span>
          status=<span class="ok">FULL</span>

<span class="ok">✓ 정원 4명을 넘지 않음</span>
<span class="ok">✓ 순번 중복·누락 없음</span>
<span class="ok">✓ 나머지 27명은 409 응답</span></pre>
    <div class="small" style="margin-top:3mm">
      "설계했다"가 아니라 <b>멀티스레드 테스트로 확인했다</b>
    </div>
  </div>
</div>""")

# 12 구현 검증
slide("", head("VERIFICATION", "설계대로 구현하여 동작을 확인했다",
               "설계 문서가 실제로 성립하는지 확인하기 위해 구현까지 진행했다.") + """
<div class="body cols23">
  <div>
    <img class="shot" src="%s">
  </div>
  <div>
    <table style="font-size:8pt">
      <tr><th>검증 항목</th><th>결과</th></tr>
      <tr><td>기록 작성</td><td>승패 자동 판정</td></tr>
      <tr><td>동일 경기 중복</td><td>409 + 기존 기록 ID</td></tr>
      <tr><td>전적 재집계</td><td>6경기 5승 1패 .833</td></tr>
      <tr><td>소표본 정책</td><td>표본 2건 → 평균 미표시</td></tr>
      <tr><td>메이트 선착순</td><td>30명 중 3명만 확정</td></tr>
      <tr><td>사진 위치정보</td><td>GPS 4태그 → <b>0태그</b></td></tr>
      <tr><td>소셜 로그인</td><td>3개 제공자 인가 URL 생성</td></tr>
      <tr><td>결과 정정 재계산</td><td>8경기 5승3패 → 7경기 5승2패</td></tr>
      <tr><td>운영자 권한</td><td>일반 회원 접근 시 403</td></tr>
    </table>
    <div class="box dark" style="margin-top:4mm">
      <h3>구현하니 드러난 설계 누락</h3>
      <p>좌석 구역을 <b>비활성으로 돌려도 일반 화면에 그대로 노출</b>되고 있었다.
      조회 경로를 나누고, 목록에서 빠져도 ID 를 직접 보내는 경로가 남아
      기록 작성 시점에도 막았다. 설계서에 없던 규칙이다</p>
    </div>
  </div>
</div>""" % img("docs/_assets/screens/app-stats.png"))

# 13 마무리
slide("", head("NEXT", "정리") + """
<div class="body">
  <div class="cols">
    <div class="box"><h3>수행한 것</h3>
      <ul><li>요구사항 79 + 비기능 24</li><li>화면 24 (상세 7)</li>
      <li>테이블 27 · API 55</li><li>설계 검증용 구현 (화면 20)</li></ul></div>
    <div class="box"><h3>범위에서 제외한 것</h3>
      <ul><li>실시간 스코어보드<br><span class="small">데이터 제공처 유료 구독 전제</span></li>
      <li>선수 개인 기록<br><span class="small">데이터 확보 경로 없음</span></li></ul></div>
    <div class="box"><h3>다음 단계</h3>
      <ul><li>티켓 카드 공유</li><li>모바일 웹 최적화</li>
      <li>실시간 대화(WebSocket)</li><li>공개 배포</li></ul></div>
  </div>
  <div class="box dark" style="margin-top:7mm; text-align:center; padding:8mm">
    <p style="font-size:13pt; color:#fff">
      남이 만든 데이터를 보여주는 서비스가 아니라,<br>
      <b style="color:#ffd666">사용자가 만든 데이터가 쌓일수록 좋아지는 서비스</b>를 설계했습니다
    </p>
  </div>
</div>""")

html = ("<!doctype html><html lang='ko'><head><meta charset='utf-8'>"
        "<title>오늘의직관 - 발표 자료</title><style>%s</style></head><body>%s</body></html>"
        % (FONT_CSS + CSS, "".join(S)))
os.makedirs(OUT, exist_ok=True)
hp = os.path.join(OUT, "_slides.html")
io.open(hp, "w", encoding="utf-8").write(html)
pdf = os.path.join(OUT, "발표자료_오늘의직관.pdf")
subprocess.run([CHROME, "--headless", "--disable-gpu", "--no-pdf-header-footer",
                "--print-to-pdf=" + pdf, "--virtual-time-budget=12000", "file://" + hp],
               check=True, capture_output=True)
os.remove(hp)
print("슬라이드 %d장" % len(S))
print("PDF :", pdf, "(%.1fKB)" % (os.path.getsize(pdf) / 1024))

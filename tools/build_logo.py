# -*- coding: utf-8 -*-
"""
로고 산출물 생성기

마크(logo-mark.svg)를 원본으로 삼아 락업 SVG · PNG · 문서 도구에 심는 data URI 를
한꺼번에 만든다. 예전에는 락업 SVG 안에 <text> 로 "오늘의직관" 을 넣어 두었는데,
보는 사람 컴퓨터에 깔린 폰트에 따라 로고 모양이 달라졌다. 그래서 프리텐다드 Bold
글리프를 외곽선으로 변환해 넣는다.

사용: python3 tools/build_logo.py
"""
import base64
import io
import os
import re
import subprocess

from fontTools.misc.transform import Identity
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LOGO = os.path.join(BASE, "docs", "_assets", "logo")
FONT = os.path.join(BASE, "docs", "_assets", "font", "Pretendard-Bold.woff2")
FRONT = os.path.join(BASE, "frontend", "src", "assets")
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
NAVY = "#16355c"

WORD = "오늘의직관"
SIZE, ORIGIN_X, BASELINE, TRACKING = 26.0, 76.0, 41.0, -0.5


def wordmark_path():
    """워드마크를 글리프 외곽선 하나로 합쳐 돌려준다. (path, 오른쪽 끝 x)"""
    f = TTFont(FONT)
    gs, cmap, hmtx = f.getGlyphSet(), f.getBestCmap(), f["hmtx"]
    scale = SIZE / f["head"].unitsPerEm
    pen, x = SVGPathPen(gs), ORIGIN_X
    for ch in WORD:
        gn = cmap[ord(ch)]
        gs[gn].draw(TransformPen(pen, Identity.translate(x, BASELINE).scale(scale, -scale)))
        x += hmtx[gn][0] * scale + TRACKING
    return pen.getCommands(), x


def main():
    mark = io.open(os.path.join(LOGO, "logo-mark.svg"), encoding="utf-8").read()
    shape = re.search(r'd="\s*(.*?)\s*"', mark, re.S).group(1)
    word, right = wordmark_path()
    width = round(right + 2)

    lockup = (
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %d 64"'
        ' role="img" aria-label="%s">\n'
        "  <!-- tools/build_logo.py 가 생성한다. 직접 고치지 않는다. -->\n"
        '  <path fill="currentColor" fill-rule="evenodd" d="\n    %s\n  "/>\n'
        '  <path fill="currentColor" d="%s"/>\n</svg>\n'
    ) % (width, WORD, shape, word)

    for d in (LOGO, FRONT):
        io.open(os.path.join(d, "logo-lockup.svg"), "w", encoding="utf-8").write(lockup)

    # PDF·발표자료는 색을 직접 지정해야 해서 currentColor 를 네이비로 바꿔 심는다
    def uri(t):
        return "data:image/svg+xml;base64," + base64.b64encode(
            t.replace("currentColor", NAVY).encode("utf-8")).decode()

    lock_uri, mark_uri = uri(lockup), uri(mark)
    for name in ("build_pdf.py", "build_slides.py"):
        p = os.path.join(BASE, "tools", name)
        t = io.open(p, encoding="utf-8").read()

        def swap(m):
            old = base64.b64decode(m.group(1) + "===" [:(-len(m.group(1))) % 4]).decode("utf-8", "ignore")
            vb = re.search(r'viewBox="0 0 (\d+)', old)
            return mark_uri if vb and vb.group(1) == "64" else lock_uri

        io.open(p, "w", encoding="utf-8").write(
            re.sub(r"data:image/svg\+xml;base64,([A-Za-z0-9+/=]+)", swap, t))

    # 발표 자료·외부 공유용 PNG. 배경을 비워 어느 바탕에나 얹을 수 있게 한다
    for kind, src in (("mark", mark), ("lockup", lockup)):
        vb = re.search(r'viewBox="0 0 (\d+) (\d+)', src)
        w, h = int(vb.group(1)), int(vb.group(2))
        for tone, color in (("navy", NAVY), ("white", "#ffffff")):
            box = w * 6
            page = ('<html><body style="margin:0"><div style="width:%dpx;color:%s">%s</div>'
                    "</body></html>") % (box, color, src)
            tmp = os.path.join(LOGO, "_tmp.html")
            io.open(tmp, "w", encoding="utf-8").write(page)
            out = os.path.join(LOGO, "logo-%s-%s.png" % (kind, tone))
            subprocess.run([CHROME, "--headless", "--disable-gpu",
                            "--default-background-color=00000000",
                            "--window-size=%d,%d" % (box, box * h // w),
                            "--screenshot=" + out, "file://" + tmp], capture_output=True)
            os.remove(tmp)
            print("PNG :", os.path.basename(out), os.path.getsize(out), "bytes")

    print("락업 폭 %dpx · 마크 원본 logo-mark.svg" % width)


if __name__ == "__main__":
    main()

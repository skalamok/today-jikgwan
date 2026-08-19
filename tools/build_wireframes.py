# -*- coding: utf-8 -*-
"""
와이어프레임 HTML → PNG 캡처

원본은 docs/_assets/wireframes/*.html 하나뿐이다.
캡처를 위해 프론트 dev 서버로 잠시 복사했다가 끝나면 지우므로 사본이 남지 않는다.

사용: python3 tools/build_wireframes.py   (프론트 dev 서버가 5173 에 떠 있어야 함)
"""
import glob
import io
import os
import shutil
import subprocess
import sys

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(BASE, "docs", "_assets", "wireframes")
SERVE = os.path.join(BASE, "frontend", "public", "wireframes")
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
ORIGIN = "http://localhost:5173/wireframes"

# 데스크톱 와이어프레임은 가로가 넓다
SIZES = {"PC": (1040, 1400), "MOBILE": (390, 2200)}


def crop(path):
    from PIL import Image, ImageChops
    im = Image.open(path).convert("RGB")
    diff = ImageChops.difference(im, Image.new("RGB", im.size, (255, 255, 255)))
    box = diff.getbbox()
    if box:
        pad = 16
        im = im.crop((max(0, box[0] - pad), max(0, box[1] - pad),
                      min(im.width, box[2] + pad), min(im.height, box[3] + pad)))
    im.save(path, optimize=True)
    return im.size


def main():
    names = [os.path.basename(p)[:-5] for p in sorted(glob.glob(os.path.join(SRC, "*.html")))]
    if not names:
        sys.exit("와이어프레임 HTML 이 없습니다: " + SRC)

    # 서빙용 임시 복사
    os.makedirs(SERVE, exist_ok=True)
    for f in glob.glob(os.path.join(SRC, "*.html")) + [os.path.join(SRC, "wireframe.css")]:
        shutil.copy2(f, SERVE)

    try:
        for name in names:
            w, h = SIZES["PC" if name.endswith("-PC") or name.startswith("FLOW") else "MOBILE"]
            out = os.path.join(SRC, name + ".png")
            subprocess.run([
                CHROME, "--headless", "--disable-gpu", "--hide-scrollbars",
                "--force-device-scale-factor=2", "--window-size=%d,%d" % (w, h),
                "--screenshot=" + out, "%s/%s.html" % (ORIGIN, name),
            ], check=True, capture_output=True)
            size = crop(out)
            print("  %-22s %4dx%-5d %6.1fKB" % (name, size[0], size[1],
                                                os.path.getsize(out) / 1024))
    finally:
        # 사본을 남기지 않는다 (원본과 어긋나는 것을 막기 위함)
        shutil.rmtree(SERVE, ignore_errors=True)

    print("\n%d개 캡처 완료 → %s" % (len(names), SRC))
    print("문서에 반영하려면: python3 tools/build_pdf.py")


if __name__ == "__main__":
    main()

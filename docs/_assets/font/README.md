# 문서용 폰트

기술서 · 발표 자료 PDF 에 쓰는 **Pretendard** 다.
라틴 글자가 한글과 함께 설계돼 있어 영문 폰트를 따로 섞지 않는다.

| 항목 | 내용 |
|---|---|
| 출처 | https://github.com/orioncactus/pretendard (v1.3.9) |
| 라이선스 | SIL Open Font License 1.1 |
| 쓰는 곳 | `tools/build_pdf.py`, `tools/build_slides.py`, `tools/build_logo.py` |

## 알아 둘 것

- 로고 워드마크는 `tools/build_logo.py` 가 이 폰트의 Bold 글리프를 **외곽선으로 변환**해 넣는다.
  글자로 두면 보는 사람 컴퓨터에 깔린 폰트에 따라 로고 모양이 달라진다.
- 괘선 문자 `─ │ ┌ ┐ └ ┘ ├ ┤` 는 이 폰트에 없다. 화면 설계서의 ASCII 와이어프레임에만
  쓰이고 전부 코드 블록 안이라 고정폭 폰트로 떨어진다.
- PDF 푸터는 본문과 별개 문서로 그려져 `@font-face` 를 무시한다. 서브셋을 data URI 로
  심어 봐도 안 먹는다. 그래서 푸터만 시스템 고딕을 쓴다.

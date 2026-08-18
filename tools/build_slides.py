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

def img(path):
    return "file://" + os.path.join(BASE, path)

CSS = """
@page { size: 297mm 167mm; margin: 0; }
* { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: 'Apple SD Gothic Neo','Malgun Gothic',-apple-system,sans-serif; color: #17191d; }

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
<div class="rule"></div>
<h1>오늘의직관</h1>
<div class="sub">KBO 직관 기록 및 관람 정보 서비스</div>
<div class="desc">흩어진 직관 경험을 기록으로 모아, 나만의 전적을 만든다</div>
<div class="meta">이채목<br>2026.08.20</div>
</section>""")

# 2 문제 정의
slide("", head("PROBLEM", "직관을 다녀오면, 아무것도 남지 않는다",
               "티켓은 서랍에, 사진은 갤러리에, 감상은 메모장에. 셋은 서로 연결되지 않는다.") + """
<div class="body">
  <div class="cols">
    <div class="box"><h3>🎟️ 티켓</h3><p>실물로 서랍에 쌓인다.<br>언제 어느 경기였는지<br>시간이 지나면 알 수 없다</p></div>
    <div class="box"><h3>📷 사진</h3><p>갤러리에 수천 장 중<br>하나로 묻힌다.<br>찾으려면 날짜를 기억해야 한다</p></div>
    <div class="box"><h3>📝 감상</h3><p>SNS나 메모장에 흩어진다.<br>어느 경기 이야기인지<br>연결이 끊긴다</p></div>
  </div>
  <div class="box dark" style="margin-top:7mm">
    <h3>그래서 생기는 진짜 문제</h3>
    <p style="font-size:11pt">개별 경기 결과는 어디서든 볼 수 있다. 그런데
    <span style="color:#ffd666; font-weight:700">내가 직접 간 경기만 추린 전적</span>은 어떤 서비스도 제공하지 않는다.<br>
    팬들이 말하는 "내가 가면 이긴다"는 확인할 방법이 없는 체감에 머문다.</p>
  </div>
</div>""")

# 3 서비스 소개
slide("", head("SOLUTION", "기록을 모으고, 데이터를 되돌려준다") + """
<div class="body cols23">
  <div>
    <div class="box" style="margin-bottom:5mm">
      <h3>① 기록을 한 곳에 모은다</h3>
      <p>경기 · 좌석 구역 · 사진 · 메모 · 비용 · 동행자를 한 기록으로 저장</p>
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
slide("", head("USER JOURNEY", "라이트 유저가 헤비 유저가 되는 순환",
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
    <div class="step"><div class="n">STEP 5</div><div class="t">정착</div><div class="d">전적 심화<br>동행·계획</div></div>
  </div>
  <div class="cols2" style="margin-top:8mm">
    <div class="box"><h3>소비자로 들어와서</h3>
      <p>라이트 유저는 남이 쌓은 구역 만족도를 보고 자리를 고른다</p></div>
    <div class="box"><h3>생산자가 되어 나간다</h3>
      <p>다녀와서 남긴 평가 한 줄이 다음 사람의 정보가 된다.<br>
      <b>사용자가 늘수록 구장 정보가 정확해지는 구조</b></p></div>
  </div>
</div>""")

# 5 산출물
slide("", head("DELIVERABLES", "산출물") + """
<div class="body">
  <div class="cols">
    <div class="box"><div class="big">81<span style="font-size:12pt">+24</span></div>
      <div class="mid" style="margin-top:2mm">요구사항</div>
      <p class="small">기능 81 · 비기능 24<br>모두 화면 ID와 교차 참조</p></div>
    <div class="box"><div class="big">23<span style="font-size:12pt">/7</span></div>
      <div class="mid" style="margin-top:2mm">화면</div>
      <p class="small">전체 23개 정의<br>핵심 7개 상세 설계</p></div>
    <div class="box"><div class="big">30</div>
      <div class="mid" style="margin-top:2mm">테이블</div>
      <p class="small">9개 도메인<br>사전 집계 테이블 3</p></div>
  </div>
  <div class="cols" style="margin-top:6mm">
    <div class="box"><div class="big">42</div>
      <div class="mid" style="margin-top:2mm">API 오퍼레이션</div>
      <p class="small">엔드포인트 32<br>OpenAPI 3.0</p></div>
    <div class="box"><div class="big">35</div>
      <div class="mid" style="margin-top:2mm">기술서 쪽수</div>
      <p class="small">개요 + 요구사항<br>+ 화면 설계</p></div>
    <div class="box dark"><div class="big">4/4</div>
      <div class="mid" style="margin-top:2mm">기술 과제</div>
      <p class="small">집계 · 동시성 · 이력<br>제약조건 편성 모두 구현</p></div>
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
      <tr><td>1xx</td><td>경기 · 구장</td><td>5xx</td><td>동행</td></tr>
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
      <tr><td>동행</td><td>companion_posts,<br>companion_applications</td></tr>
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
    <div class="box"><h3>결정</h3><p>외부 자동 수집을 전면 배제.
    공식 API 와 사용자 제보를 <b>이중 출처</b>로 두고, 어느 한쪽이 비어도 메우는 구조로 설계</p></div>
    <div class="box"><h3>배운 것</h3><p>공개된 안내 문구만으로 이용 가능 여부를 판단하면 안 된다.
    <b>권리자에게 직접 확인하는 절차</b>가 필요하다</p></div>
  </div>
</div>""")

# 10 설계 결정 2 — 소표본
slide("", head("DESIGN DECISION 2", "2경기 2승을 승률 1.000 으로 보여주지 않는다") + """
<div class="body">
  <div class="cols2">
    <div class="box" style="border-color:#e8b4b4; background:#fdf6f6">
      <h3 class="bad">그대로 계산하면</h3>
      <p style="font-size:12pt">고척스카이돔 <b>2경기 2승 · 승률 1.000</b></p>
      <p style="margin-top:2mm">"고척에서 나는 무패다" 라는 잘못된 인상을 준다.
      표본 2개로는 아무것도 말할 수 없다</p>
    </div>
    <div class="box" style="border-color:#b4c8e8; background:#f6f9fd">
      <h3 class="good">적용한 정책</h3>
      <p style="font-size:12pt">고척스카이돔 <b>2경기 2승 <span style="color:#6b7076">(표본 부족)</span></b></p>
      <p style="margin-top:2mm">표본 5경기 미만은 승률을 <b>산출하지 않고</b> 전적만 표시.
      승률 순위에서도 제외</p>
    </div>
  </div>

  <div class="box" style="margin-top:6mm">
    <h3>이 정책이 4개 층을 관통한다</h3>
    <table style="margin-top:2mm">
      <tr><th>요구사항</th><th>화면 설계</th><th>API</th><th>데이터 모델</th></tr>
      <tr>
        <td>REQ-F-305<br>기준값 5경기</td>
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
      <p>동행 모집 정원이 3명인데 여러 명이 같은 순간 참여를 누르면
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
    <table style="font-size:8.5pt">
      <tr><th>검증 항목</th><th>결과</th></tr>
      <tr><td>회원가입 · 로그인</td><td>JWT 발급</td></tr>
      <tr><td>이메일 중복</td><td>409 DUPLICATE_EMAIL</td></tr>
      <tr><td>기록 작성</td><td>승패 자동 판정</td></tr>
      <tr><td>동일 경기 중복</td><td>409 + 기존 기록 ID 반환</td></tr>
      <tr><td>전적 재집계</td><td>6경기 5승 1패 .833</td></tr>
      <tr><td>소표본 정책</td><td>표본 2건 → 평균 미표시</td></tr>
      <tr><td>동행 선착순</td><td>30명 중 3명만 확정</td></tr>
      <tr><td>사진 위치정보</td><td>GPS 4태그 → <b>0태그</b></td></tr>
    </table>
    <div class="box dark" style="margin-top:4mm">
      <h3>구현 중 발견한 설계 누락</h3>
      <p>화면에서 응원팀을 보낼 때 <b>팀 ID 가 필요</b>한데
      경기 조회 API 가 팀 이름만 반환하고 있었다.
      API 명세에 <code>homeTeamId</code> · <code>awayTeamId</code> 를 추가했다</p>
    </div>
  </div>
</div>""" % img("docs/_assets/screens/app-stats.png"))

# 13 마무리
slide("", head("NEXT", "정리") + """
<div class="body">
  <div class="cols">
    <div class="box"><h3>수행한 것</h3>
      <ul><li>요구사항 81 + 비기능 24</li><li>화면 23 (상세 7)</li>
      <li>테이블 30 · API 42</li><li>설계 검증용 구현</li></ul></div>
    <div class="box"><h3>범위에서 제외한 것</h3>
      <ul><li>실시간 스코어보드<br><span class="small">데이터 제공처 유료 구독 전제</span></li>
      <li>선수 개인 기록<br><span class="small">데이터 확보 경로 없음</span></li></ul></div>
    <div class="box"><h3>다음 단계</h3>
      <ul><li>배지 · 카드 공유</li><li>동행 대화방</li>
      <li>운영자 화면</li><li>공개 배포</li></ul></div>
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
        % (CSS, "".join(S)))
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

/**
 * REQ-F-704 직관 카드.
 *
 * 기록 한 건을 티켓 모양 이미지로 그려 내려받는다. 서버에서 만들지 않는 것은
 * 폰트와 렌더링 부담을 서버가 질 이유가 없고, 비공개 기록도 본인은 만들 수 있어야
 * 하는데 화면에 이미 그 값이 있기 때문이다.
 *
 * 로고와 같은 티켓 실루엣을 쓴다. 좌우에 홈을 파서 표를 찢어 낸 모양으로 읽히게 한다.
 */

const W = 720
const H = 1000
const NAVY = '#16355c'
const INK = '#1a1a1a'
const MUTED = '#8a94a6'

const RESULT = { WIN: '승', LOSE: '패', DRAW: '무', NEUTRAL: '중립' }

/** 티켓 실루엣. 좌우 홈과 아래쪽 절취선 위치를 함께 돌려준다 */
function ticketPath(ctx, x, y, w, h, notchY) {
  const r = 20
  const nr = 26
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.lineTo(x + w - r, y)
  ctx.quadraticCurveTo(x + w, y, x + w, y + r)
  ctx.lineTo(x + w, y + notchY - nr)
  ctx.arc(x + w, y + notchY, nr, -Math.PI / 2, Math.PI / 2, true)
  ctx.lineTo(x + w, y + h - r)
  ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h)
  ctx.lineTo(x + r, y + h)
  ctx.quadraticCurveTo(x, y + h, x, y + h - r)
  ctx.lineTo(x, y + notchY + nr)
  ctx.arc(x, y + notchY, nr, Math.PI / 2, -Math.PI / 2, true)
  ctx.lineTo(x, y + r)
  ctx.quadraticCurveTo(x, y, x + r, y)
  ctx.closePath()
}

function text(ctx, s, x, y, { size = 24, weight = 400, color = INK, align = 'left' } = {}) {
  ctx.font = `${weight} ${size}px Pretendard, 'Apple SD Gothic Neo', sans-serif`
  ctx.fillStyle = color
  ctx.textAlign = align
  ctx.fillText(s, x, y)
}

/** 글자가 칸을 넘치면 줄여서라도 한 줄에 넣는다. 잘라내면 팀 이름이 뭉개진다 */
function fitText(ctx, s, x, y, maxWidth, opt) {
  let size = opt.size
  do {
    ctx.font = `${opt.weight || 400} ${size}px Pretendard, 'Apple SD Gothic Neo', sans-serif`
    if (ctx.measureText(s).width <= maxWidth) break
    size -= 2
  } while (size > 14)
  text(ctx, s, x, y, { ...opt, size })
}

export async function drawTicketCard(log) {
  // 웹폰트가 준비되기 전에 그리면 시스템 폰트로 찍힌다
  if (document.fonts && document.fonts.ready) await document.fonts.ready

  const canvas = document.createElement('canvas')
  canvas.width = W * 2                      // 2배로 그려야 공유했을 때 흐리지 않다
  canvas.height = H * 2
  const ctx = canvas.getContext('2d')
  ctx.scale(2, 2)

  ctx.fillStyle = '#eef1f5'
  ctx.fillRect(0, 0, W, H)

  const M = 40
  const notchY = 600
  ticketPath(ctx, M, M, W - M * 2, H - M * 2, notchY)
  ctx.fillStyle = '#fff'
  ctx.fill()

  const cx = W / 2
  let y = M + 78

  text(ctx, '오늘의직관', cx, y, { size: 26, weight: 800, color: NAVY, align: 'center' })
  y += 34
  text(ctx, 'KBO 직관 기록', cx, y, { size: 15, color: MUTED, align: 'center' })

  y += 76
  text(ctx, log.gameDate, cx, y, { size: 20, color: MUTED, align: 'center' })

  y += 76
  fitText(ctx, log.matchup, cx, y, W - M * 2 - 60, { size: 46, weight: 800, align: 'center' })

  y += 56
  text(ctx, log.stadiumName, cx, y, { size: 22, color: MUTED, align: 'center' })

  // 결과 배지
  y += 74
  const label = RESULT[log.result] || ''
  ctx.font = '800 34px Pretendard, sans-serif'
  const bw = Math.max(120, ctx.measureText(label).width + 76)
  ctx.beginPath()
  ctx.roundRect(cx - bw / 2, y - 46, bw, 68, 34)
  ctx.fillStyle = log.result === 'NEUTRAL' ? '#eef1f5' : NAVY
  ctx.fill()
  text(ctx, label, cx, y, {
    size: 34, weight: 800, align: 'center',
    color: log.result === 'NEUTRAL' ? MUTED : '#fff',
  })

  // 결과만 있으면 누구 기준인지 알 수 없다
  y += 48
  text(ctx, log.cheerTeam ? `${log.cheerTeam} 응원` : '중립 관람', cx, y,
       { size: 18, color: MUTED, align: 'center' })

  y += 66
  text(ctx, log.zoneName, cx, y, { size: 24, weight: 700, align: 'center' })
  y += 44
  text(ctx, '★'.repeat(log.zoneRating) + '☆'.repeat(5 - log.zoneRating), cx, y,
       { size: 26, color: NAVY, align: 'center' })

  // 절취선
  ctx.setLineDash([8, 8])
  ctx.strokeStyle = '#d8dde3'
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.moveTo(M + 30, M + notchY)
  ctx.lineTo(W - M - 30, M + notchY)
  ctx.stroke()
  ctx.setLineDash([])

  // 절취선 아래는 그날의 한마디
  y = M + notchY + 66
  if (log.memo) {
    fitText(ctx, `"${log.memo}"`, cx, y, W - M * 2 - 60, { size: 22, align: 'center' })
    y += 52
  }
  if (log.companions && log.companions.length) {
    text(ctx, `함께 · ${log.companions.join(', ')}`, cx, y, { size: 18, color: MUTED, align: 'center' })
    y += 40
  }
  if (log.totalCost) {
    text(ctx, `${log.totalCost.toLocaleString()}원`, cx, y, { size: 18, color: MUTED, align: 'center' })
  }

  text(ctx, 'todayjikgwan.site', cx, H - M - 36, { size: 15, color: MUTED, align: 'center' })

  return canvas
}

export async function downloadTicketCard(log) {
  const canvas = await drawTicketCard(log)
  const link = document.createElement('a')
  link.download = `오늘의직관_${log.gameDate}_${log.stadiumName}.png`
  link.href = canvas.toDataURL('image/png')
  link.click()
}

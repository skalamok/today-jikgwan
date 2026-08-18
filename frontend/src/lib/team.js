/**
 * 구단 시그니처 컬러.
 * 화면에서 팀을 색으로 구분하기 위한 값이며, 짧은 이름(shortName)으로 찾는다.
 */
const COLORS = {
  LG:   { bg: '#C30452', fg: '#fff' },
  두산: { bg: '#1A1A3C', fg: '#fff' },
  KIA:  { bg: '#EA0029', fg: '#fff' },
  삼성: { bg: '#074CA1', fg: '#fff' },
  롯데: { bg: '#041E42', fg: '#fff' },
  SSG:  { bg: '#CE0E2D', fg: '#fff' },
  NC:   { bg: '#315288', fg: '#fff' },
  키움: { bg: '#570514', fg: '#fff' },
  한화: { bg: '#FC4E00', fg: '#fff' },
  KT:   { bg: '#26262A', fg: '#fff' },
}
const FALLBACK = { bg: '#8a94a6', fg: '#fff' }

export function teamColor(shortName) {
  if (!shortName) return FALLBACK
  const key = Object.keys(COLORS).find((k) => shortName.startsWith(k))
  return key ? COLORS[key] : FALLBACK
}

/** "LG 트윈스" 같은 전체 이름에서도 찾을 수 있게 한다 */
export function teamColorByName(name) {
  return teamColor(name)
}

<script setup>
import { ref, onMounted, computed } from 'vue'
import client from '../api/client'
import TeamMark from '../components/TeamMark.vue'

const loading = ref(true)
const summary = ref(null)
const logs = ref([])
const games = ref([])
const gameDate = ref('')
const dataSource = ref('')

// 화면설계서 SCR-MAIN-001: 기록 유무에 따라 A/B 두 형태로 분기한다
const hasLogs = computed(() => logs.value.length > 0)
const winRateText = computed(() => {
  const s = summary.value
  if (!s) return ''
  // REQ-F-305 표본 부족 시 승률 대신 전적만 표시한다
  return s.smallSample ? '표본이 더 쌓이면 승률이 표시돼요' : `승률 ${s.winRate.toFixed(3).replace('0.', '.')}`
})

function kst(iso) {
  return new Date(iso).toLocaleTimeString('ko-KR',
    { hour: '2-digit', minute: '2-digit', hour12: false, timeZone: 'Asia/Seoul' })
}

onMounted(async () => {
  try {
    const g = await client.get('/games', { params: { date: '2026-08-18' } })
    games.value = g.data.content
    gameDate.value = g.data.date
    dataSource.value = g.data.dataSource
    const [s, l] = await Promise.all([
      client.get('/stats/me/summary'),
      client.get('/attendance-logs'),
    ])
    summary.value = s.data
    logs.value = l.data
  } finally { loading.value = false }
})
</script>

<template>
  <div v-if="loading">
    <div class="card"><div class="skeleton" style="height:60px"></div></div>
    <div class="card"><div class="skeleton" style="height:90px"></div></div>
  </div>

  <div v-else>
    <!-- A. 기록이 없는 사용자: 가이드와 오늘의 경기를 우선 노출 -->
    <div class="card accent wide" v-if="!hasLogs">
      <div class="mid" style="font-size:19px">야구장, 처음 가시나요?</div>
      <div class="muted" style="margin:6px 0 12px">
        직관 다녀온 사람들이 남긴 구역별 만족도로 자리를 고를 수 있어요
      </div>
      <RouterLink to="/guide"><button class="btn ghost small">첫 직관 가이드 보기</button></RouterLink>
    </div>

    <!-- B. 기록이 있는 사용자: 전적 요약을 우선 노출 -->
    <div class="card accent wide" v-else>
      <div class="row" style="align-items:flex-start">
        <div>
          <h2>내 직관 전적</h2>
          <div class="big">{{ summary.games }}<span style="font-size:17px; font-weight:700">경기</span></div>
          <div class="mid" style="margin-top:6px; opacity:.92">
            {{ summary.wins }}승 {{ summary.draws }}무 {{ summary.losses }}패
          </div>
        </div>
        <div style="text-align:right">
          <span class="chip warn" v-if="summary.currentStreak > 0">{{ summary.currentStreak }}연승</span>
          <span class="chip lose" v-else-if="summary.currentStreak < 0">{{ -summary.currentStreak }}연패</span>
          <div v-if="!summary.smallSample" class="big" style="font-size:26px; margin-top:8px">
            {{ summary.winRate.toFixed(3).replace(/^0/, '') }}
          </div>
        </div>
      </div>
      <!-- REQ-F-305 표본이 부족하면 승률 대신 안내를 보여준다 -->
      <div v-if="!summary.smallSample" class="bar on-dark">
        <span :style="{ width: (summary.winRate * 100) + '%' }"></span>
      </div>
      <div v-else class="muted" style="margin-top:10px">{{ winRateText }}</div>
      <RouterLink to="/stats" class="muted" style="display:block; margin-top:14px">자세히 보기 →</RouterLink>
    </div>

    <div class="card">
      <div class="row"><h2 style="margin:0">오늘의 경기</h2><span class="muted">{{ gameDate }}</span></div>
      <div v-if="!games.length" class="empty" style="padding:20px"><p>오늘은 경기가 없어요</p></div>
      <div v-else>
        <div v-for="g in games" :key="g.id" class="list-item" style="align-items:center">
          <div style="flex:1; min-width:0">
            <div class="row" style="justify-content:flex-start; gap:7px">
              <TeamMark :name="g.homeTeam" size="sm" />
              <span class="muted" style="font-size:11px">vs</span>
              <TeamMark :name="g.awayTeam" size="sm" />
            </div>
            <div class="muted" style="margin-top:5px">{{ g.stadium }} · {{ kst(g.startAt) }}</div>
          </div>
          <span v-if="g.resultConfirmed" class="mid">{{ g.homeScore }} : {{ g.awayScore }}</span>
          <span v-else class="chip warn">확인 중</span>
        </div>
      </div>
      <!-- REQ-NF-014 데이터 출처 표기 -->
      <div class="source">출처 · {{ dataSource }}</div>
    </div>

    <div class="card" v-if="hasLogs">
      <h2>최근 기록</h2>
      <RouterLink v-for="l in logs.slice(0, 3)" :key="l.id" :to="`/logs/${l.id}`" class="list-item" style="align-items:center">
        <div style="flex:1">
          <div class="mid">{{ l.matchup }}</div>
          <div class="muted">{{ l.gameDate }} · {{ l.stadiumName }}</div>
        </div>
        <span class="chip" :class="l.result.toLowerCase()">
          {{ { WIN: '승', LOSE: '패', DRAW: '무', NEUTRAL: '중립' }[l.result] }}
        </span>
      </RouterLink>
    </div>

    <RouterLink to="/logs/new"><button class="btn">+ 직관 기록 남기기</button></RouterLink>
  </div>
</template>

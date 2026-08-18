<script setup>
import { ref, onMounted, watch } from 'vue'
import client from '../api/client'

const date = ref('2026-08-18')
const games = ref([])
const dataSource = ref('')
const loading = ref(false)

// 서버는 UTC 로 내려주므로 화면에서 KST 로 바꾼다
function kst(iso) {
  return new Date(iso).toLocaleTimeString('ko-KR',
    { hour: '2-digit', minute: '2-digit', hour12: false, timeZone: 'Asia/Seoul' })
}

async function load() {
  loading.value = true
  try {
    const { data } = await client.get('/games', { params: { date: date.value } })
    games.value = data.content
    dataSource.value = data.dataSource
  } finally { loading.value = false }
}
onMounted(load)
watch(date, load)
</script>

<template>
  <div class="card">
    <div class="field" style="margin:0">
      <label>날짜</label>
      <input type="date" v-model="date" />
    </div>
    <RouterLink to="/standings"><button class="btn ghost small" style="margin-top:10px">팀 순위 보기</button></RouterLink>
  </div>

  <div class="card wide">
    <div v-if="loading"><div class="skeleton" style="height:56px; margin-bottom:8px"></div></div>
    <div v-else-if="!games.length" class="empty"><p>이 날짜에는 경기가 없어요</p></div>
    <div v-else>
      <div v-for="g in games" :key="g.id" class="list-item" style="align-items:center">
        <div style="flex:1; min-width:0">
          <div class="row" style="justify-content:flex-start; gap:8px">
            <span class="mid" :class="{ dim: g.resultConfirmed && g.homeScore < g.awayScore }">
              {{ g.homeTeam }}
            </span>
            <span v-if="g.resultConfirmed" class="score">{{ g.homeScore }}</span>
            <span class="muted" style="font-size:11px">:</span>
            <span v-if="g.resultConfirmed" class="score">{{ g.awayScore }}</span>
            <span class="mid" :class="{ dim: g.resultConfirmed && g.awayScore < g.homeScore }">
              {{ g.awayTeam }}
            </span>
            <span v-if="!g.resultConfirmed" class="muted">vs</span>
          </div>
          <div class="muted" style="margin-top:4px">
            {{ g.stadium }} · {{ kst(g.startAt) }}
          </div>
        </div>
        <span v-if="!g.resultConfirmed" class="chip warn">확인 중</span>
        <span v-else class="chip">종료</span>
      </div>
    </div>
    <div class="source" v-if="games.length">출처 · {{ dataSource }}</div>
  </div>
</template>

<style scoped>
.score { font-size: 18px; font-weight: 800; letter-spacing: -0.03em; }
.dim { color: var(--muted); font-weight: 600; }
</style>

<script setup>
import { ref, onMounted } from 'vue'
import client from '../api/client'

const summary = ref(null)
const dims = ref({ STADIUM: [], OPPONENT: [], DAY_OF_WEEK: [] })
const loading = ref(true)

function rate(v) { return v == null ? null : v.toFixed(3).replace(/^0/, '') }

onMounted(async () => {
  const [s, a, b, c] = await Promise.all([
    client.get('/stats/me/summary'),
    client.get('/stats/me', { params: { dimension: 'STADIUM' } }),
    client.get('/stats/me', { params: { dimension: 'OPPONENT' } }),
    client.get('/stats/me', { params: { dimension: 'DAY_OF_WEEK' } }),
  ])
  summary.value = s.data
  dims.value = { STADIUM: a.data, OPPONENT: b.data, DAY_OF_WEEK: c.data }
  loading.value = false
})
</script>

<template>
  <div v-if="loading"><div class="card"><div class="skeleton" style="height:80px"></div></div></div>

  <div v-else-if="!summary.games" class="card empty">
    <p>아직 기록이 없어요</p>
    <RouterLink to="/logs/new"><button class="btn small">첫 기록 남기기</button></RouterLink>
  </div>

  <div v-else>
    <div class="card">
      <h2>통산</h2>
      <div class="big">{{ summary.games }}경기</div>
      <div class="mid" style="margin-top:4px">
        {{ summary.wins }}승 {{ summary.draws }}무 {{ summary.losses }}패
      </div>
      <div style="margin-top:10px">
        <!-- REQ-F-305 표본이 부족하면 승률을 산출하지 않는다 -->
        <span v-if="summary.winRate != null" class="big" style="font-size:22px">
          승률 {{ rate(summary.winRate) }}
        </span>
        <span v-else class="muted">표본 5경기 미만이라 승률을 표시하지 않아요</span>
      </div>
      <div class="row" style="margin-top:12px">
        <span class="muted">현재 {{ summary.currentStreak >= 0 ? summary.currentStreak + '연승' : (-summary.currentStreak) + '연패' }}</span>
        <span class="muted">최장 {{ summary.longestWinStreak }}연승</span>
      </div>
      <div class="muted" v-if="summary.neutralCount" style="margin-top:8px">
        중립 관람 {{ summary.neutralCount }}경기는 승패 집계에서 제외했어요
      </div>
      <div class="muted" v-if="summary.totalCost" style="margin-top:4px">
        누적 지출 {{ summary.totalCost.toLocaleString() }}원
      </div>
    </div>

    <div class="card" v-for="(rows, key) in dims" :key="key">
      <h2>{{ { STADIUM: '구장별', OPPONENT: '상대팀별', DAY_OF_WEEK: '요일별' }[key] }}</h2>
      <div v-if="!rows.length" class="muted">아직 데이터가 없어요</div>
      <div v-for="r in rows" :key="r.key" class="list-item" style="align-items:center; padding:10px 0">
        <div style="flex:1" class="mid">{{ r.label }}</div>
        <div class="muted" style="margin-right:10px">
          {{ r.games }}경기 {{ r.wins }}승 {{ r.draws }}무 {{ r.losses }}패
        </div>
        <div style="min-width:74px; text-align:right">
          <span v-if="r.winRate != null" class="mid">{{ rate(r.winRate) }}</span>
          <span v-else class="muted" style="font-size:11px">표본 부족</span>
        </div>
      </div>
    </div>
  </div>
</template>

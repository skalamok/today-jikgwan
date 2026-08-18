<script setup>
import { ref, onMounted, watch } from 'vue'
import client from '../api/client'

const date = ref('2026-08-18')
const games = ref([])
const dataSource = ref('')
const loading = ref(false)

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

  <div class="card">
    <div v-if="loading"><div class="skeleton" style="height:56px; margin-bottom:8px"></div></div>
    <div v-else-if="!games.length" class="empty"><p>이 날짜에는 경기가 없어요</p></div>
    <div v-else>
      <div v-for="g in games" :key="g.id" class="list-item" style="align-items:center">
        <div style="flex:1">
          <div class="mid">{{ g.homeTeam }} vs {{ g.awayTeam }}</div>
          <div class="muted">{{ g.stadium }} · {{ g.startAt.slice(11, 16) }}</div>
        </div>
        <div style="text-align:right">
          <div v-if="g.resultConfirmed" class="mid">{{ g.homeScore }} : {{ g.awayScore }}</div>
          <span v-else class="chip warn">확인 중</span>
        </div>
      </div>
    </div>
    <div class="source" v-if="games.length">출처 · {{ dataSource }}</div>
  </div>
</template>

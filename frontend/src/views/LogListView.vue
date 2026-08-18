<script setup>
import { ref, onMounted } from 'vue'
import client from '../api/client'

const logs = ref([])
const loading = ref(true)
onMounted(async () => {
  const { data } = await client.get('/attendance-logs')
  logs.value = data
  loading.value = false
})
const resultText = { WIN: '승', LOSE: '패', DRAW: '무', NEUTRAL: '중립' }
</script>

<template>
  <div class="card">
    <div v-if="loading"><div class="skeleton" style="height:56px"></div></div>
    <div v-else-if="!logs.length" class="empty">
      <p>아직 기록이 없어요</p>
      <RouterLink to="/logs/new"><button class="btn small">첫 기록 남기기</button></RouterLink>
    </div>
    <div v-else>
      <div v-for="l in logs" :key="l.id" class="list-item" style="align-items:center">
        <div style="flex:1">
          <div class="mid">{{ l.matchup }}</div>
          <div class="muted">{{ l.gameDate }} · {{ l.stadiumName }} · {{ l.zoneName }}</div>
          <div class="muted" v-if="l.memo" style="margin-top:4px">“{{ l.memo }}”</div>
        </div>
        <div style="text-align:right">
          <span class="chip" :class="l.result.toLowerCase()">{{ resultText[l.result] }}</span>
          <div class="muted" style="margin-top:4px" v-if="l.totalCost">
            {{ l.totalCost.toLocaleString() }}원
          </div>
        </div>
      </div>
    </div>
  </div>
  <RouterLink to="/logs/new"><button class="btn">+ 새 기록</button></RouterLink>
</template>

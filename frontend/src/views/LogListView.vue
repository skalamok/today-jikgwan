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
  <div class="card wide">
    <div v-if="loading"><div class="skeleton" style="height:56px"></div></div>
    <div v-else-if="!logs.length" class="empty">
      <p>아직 기록이 없어요</p>
      <RouterLink to="/logs/new"><button class="btn small">첫 기록 남기기</button></RouterLink>
    </div>
    <div v-else>
      <div v-for="l in logs" :key="l.id" class="list-item" style="align-items:stretch">
        <div class="mark" :class="l.result.toLowerCase()"></div>
        <div style="flex:1; min-width:0">
          <div class="row">
            <span class="mid">{{ l.matchup }}</span>
            <span class="chip" :class="l.result.toLowerCase()">{{ resultText[l.result] }}</span>
          </div>
          <div class="muted" style="margin-top:4px">
            {{ l.gameDate }} · {{ l.stadiumName }} · {{ l.zoneName }}
          </div>
          <div v-if="l.memo" class="memo">“{{ l.memo }}”</div>
          <div class="muted" style="margin-top:6px" v-if="l.totalCost">
            {{ l.totalCost.toLocaleString() }}원
          </div>
        </div>
      </div>
    </div>
  </div>
  <RouterLink to="/logs/new"><button class="btn">+ 새 기록</button></RouterLink>
</template>

<style scoped>
.mark { width: 4px; border-radius: 999px; flex: none; background: var(--draw); }
.mark.win { background: var(--win); }
.mark.lose { background: var(--lose); }
.mark.neutral { background: var(--draw); }
.memo {
  margin-top: 8px; padding: 9px 11px; background: var(--card-soft);
  border-radius: 10px; font-size: 13px; color: var(--ink-2); line-height: 1.5;
}
</style>

<script setup>
import { ref, onMounted } from 'vue'
import client from '../api/client'
import TeamMark from '../components/TeamMark.vue'

const rows = ref([])
const loading = ref(true)
onMounted(async () => {
  rows.value = (await client.get('/standings', { params: { season: 2026 } })).data
  loading.value = false
})
function rate(v) { return v == null ? '-' : v.toFixed(3).replace(/^0/, '') }
</script>

<template>
  <div class="card wide">
    <h2>2026 시즌 순위</h2>
    <div class="muted" style="margin:-6px 0 10px">
      외부 순위표를 가져오지 않고 등록된 경기 결과로 직접 산출합니다
    </div>
    <div v-if="loading"><div class="skeleton" style="height:40px"></div></div>
    <div v-else-if="!rows.length" class="muted">아직 확정된 경기가 없어요</div>
    <div v-else>
      <div class="list-item muted" style="padding:6px 0; font-size:11px">
        <div style="width:28px">순위</div>
        <div style="flex:1">팀</div>
        <div style="width:36px; text-align:right">경기</div>
        <div style="width:70px; text-align:right">승·무·패</div>
        <div style="width:44px; text-align:right">승률</div>
        <div style="width:40px; text-align:right">승차</div>
      </div>
      <div v-for="r in rows" :key="r.teamId" class="list-item" style="align-items:center; padding:9px 0">
        <div style="width:28px"><span class="rank" :class="{ top: r.rank <= 3 }">{{ r.rank }}</span></div>
        <div style="flex:1"><TeamMark :name="r.team" size="sm" /></div>
        <div style="width:36px; text-align:right" class="muted">{{ r.games }}</div>
        <div style="width:70px; text-align:right" class="muted">{{ r.wins }}·{{ r.draws }}·{{ r.losses }}</div>
        <div style="width:44px; text-align:right" class="mid">{{ rate(r.winRate) }}</div>
        <div style="width:40px; text-align:right" class="muted">
          {{ r.gamesBehind === 0 ? '-' : r.gamesBehind.toFixed(1) }}
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.rank {
  display: inline-flex; align-items: center; justify-content: center;
  width: 22px; height: 22px; border-radius: 7px; font-size: 12px; font-weight: 800;
  background: var(--card-soft); color: var(--muted);
}
.rank.top { background: var(--brand); color: #fff; }
</style>

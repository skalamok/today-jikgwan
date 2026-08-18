<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import client from '../api/client'

const route = useRoute(); const router = useRouter()
const log = ref(null)
const viewer = ref(null)
const error = ref('')

const RESULT = { WIN: '승', LOSE: '패', DRAW: '무', NEUTRAL: '중립' }

onMounted(async () => {
  try { log.value = (await client.get(`/attendance-logs/${route.params.id}`)).data }
  catch (e) { error.value = e.response?.data?.message || '기록을 불러오지 못했어요' }
})

async function removeLog() {
  if (!confirm('이 기록을 삭제할까요? 전적에서도 제외됩니다.')) return
  await client.delete(`/attendance-logs/${route.params.id}`)
  router.replace('/logs')
}

async function removePhoto(id) {
  await client.delete(`/attendance-logs/${route.params.id}/photos/${id}`)
  log.value.photos = log.value.photos.filter((p) => p.id !== id)
}
</script>

<template>
  <div class="card" v-if="error">{{ error }}</div>

  <template v-if="log">
    <div class="card accent wide">
      <div class="row" style="align-items:flex-start">
        <div>
          <div class="muted">{{ log.gameDate }}</div>
          <div class="big" style="font-size:24px; margin-top:4px">{{ log.matchup }}</div>
          <div class="mid" style="margin-top:8px; opacity:.9">
            {{ log.stadiumName }} · {{ log.zoneName }}
          </div>
        </div>
        <span class="chip" :class="log.result.toLowerCase()">{{ RESULT[log.result] }}</span>
      </div>
    </div>

    <div class="card wide" v-if="log.photos.length">
      <h2>사진 {{ log.photos.length }}장</h2>
      <div class="grid">
        <div v-for="p in log.photos" :key="p.id" class="shot">
          <img :src="p.thumbnailUrl" @click="viewer = p" />
          <button class="del" @click.stop="removePhoto(p.id)">×</button>
        </div>
      </div>
    </div>

    <div class="card">
      <h2>기록</h2>
      <div class="kv"><span>응원팀</span><b>{{ log.cheerTeam || '중립 관람' }}</b></div>
      <div class="kv"><span>구역 만족도</span><b>{{ '★'.repeat(log.zoneRating) }}</b></div>
      <div class="kv" v-if="log.gameRating"><span>경기 평점</span><b>{{ '★'.repeat(log.gameRating) }}</b></div>
      <div class="kv" v-if="log.weatherSky"><span>날씨</span>
        <b>{{ log.weatherSky }}<span v-if="log.weatherTemp"> {{ log.weatherTemp }}°</span></b></div>
      <div class="kv"><span>공개 범위</span>
        <b>{{ log.visibility === 'PUBLIC' ? '공개' : '비공개' }}</b></div>
    </div>

    <div class="card" v-if="log.memo">
      <h2>그날의 한마디</h2>
      <p style="font-size:15px; line-height:1.65">{{ log.memo }}</p>
    </div>

    <div class="card" v-if="log.totalCost">
      <h2>비용</h2>
      <div class="kv" v-if="log.ticketCost"><span>티켓</span><b>{{ log.ticketCost.toLocaleString() }}원</b></div>
      <div class="kv" v-if="log.foodCost"><span>먹거리</span><b>{{ log.foodCost.toLocaleString() }}원</b></div>
      <div class="kv" v-if="log.transportCost"><span>교통</span><b>{{ log.transportCost.toLocaleString() }}원</b></div>
      <div class="kv total"><span>합계</span><b>{{ log.totalCost.toLocaleString() }}원</b></div>
    </div>

    <button class="btn ghost" @click="removeLog">기록 삭제</button>
  </template>

  <div v-if="viewer" class="viewer" @click="viewer = null">
    <img :src="viewer.originalUrl" />
  </div>
</template>

<style scoped>
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(96px, 1fr)); gap: 8px; }
.shot { position: relative; aspect-ratio: 1; }
.shot img {
  width: 100%; height: 100%; object-fit: cover; border-radius: 10px;
  cursor: zoom-in; background: var(--card-soft);
}
.del {
  position: absolute; top: 5px; right: 5px; width: 22px; height: 22px;
  border: 0; border-radius: 50%; background: rgba(0,0,0,.55); color: #fff;
  font-size: 15px; line-height: 1; cursor: pointer;
}
.kv {
  display: flex; justify-content: space-between; padding: 9px 0;
  border-bottom: 1px solid var(--line); font-size: 14px;
}
.kv:last-child { border-bottom: 0; }
.kv span { color: var(--muted); }
.kv.total b { font-size: 16px; }
.viewer {
  position: fixed; inset: 0; z-index: 90; background: rgba(0,0,0,.88);
  display: flex; align-items: center; justify-content: center; cursor: zoom-out;
}
.viewer img { max-width: 94%; max-height: 94%; border-radius: 8px; }
</style>

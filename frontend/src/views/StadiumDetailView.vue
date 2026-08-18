<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import client from '../api/client'
import TeamMark from '../components/TeamMark.vue'

const route = useRoute()
const stadium = ref(null)
const selectedZone = ref(null)
const reviews = ref([])

async function load() {
  const { data } = await client.get(`/stadiums/${route.params.id}`)
  stadium.value = data
  const first = data.zones.find((z) => z.ratingCount > 0)
  if (first) selectZone(first)
}
async function selectZone(z) {
  selectedZone.value = z
  const { data } = await client.get(`/stadiums/${route.params.id}/zones/${z.zoneId}/reviews`)
  reviews.value = data
}
onMounted(load)
watch(() => route.params.id, load)
</script>

<template>
  <div v-if="stadium">
    <div class="card accent">
      <div class="big" style="font-size:22px">{{ stadium.name }}</div>
      <div class="row" style="justify-content:flex-start; gap:7px; margin-top:10px">
        <TeamMark v-for="t in stadium.homeTeams" :key="t" :name="t" size="sm" />
      </div>
      <div class="muted" v-if="stadium.nameEn">{{ stadium.nameEn }}</div>
    </div>

    <div class="card">
      <h2>구역별 만족도</h2>
      <div class="muted" style="margin:-6px 0 10px">
        직관 다녀온 사람들이 남긴 평가입니다
      </div>
      <div v-for="z in stadium.zones" :key="z.zoneId" class="list-item"
           style="align-items:center; cursor:pointer"
           :style="selectedZone?.zoneId === z.zoneId ? 'background:var(--brand-soft); border-radius:8px' : ''"
           @click="selectZone(z)">
        <div style="flex:1" class="mid">{{ z.name }}</div>
        <div style="text-align:right">
          <!-- REQ-F-305 표본 부족 시 평균을 표시하지 않는다 -->
          <div v-if="z.avgRating != null" class="mid" style="font-size:17px; color:var(--accent)">
            ★ {{ z.avgRating.toFixed(1) }}
          </div>
          <span v-else class="chip draw">평가 부족</span>
          <div class="muted" style="font-size:11px; margin-top:3px">기록 {{ z.ratingCount }}건</div>
        </div>
      </div>
    </div>

    <div class="card" v-if="selectedZone">
      <h2>{{ selectedZone.name }} 후기</h2>
      <div v-if="!reviews.length" class="muted">아직 공개된 후기가 없어요</div>
      <div v-for="(r, i) in reviews" :key="i" class="list-item">
        <div style="flex:1">
          <div>“{{ r.memo }}”</div>
          <div class="muted" style="margin-top:4px">{{ r.attendedAt }} · 시야 {{ r.rating }}점</div>
        </div>
      </div>
    </div>

    <div class="card" v-if="stadium.myRecord">
      <h2>내 {{ stadium.name }} 전적</h2>
      <div class="mid">
        {{ stadium.myRecord.games }}경기
        {{ stadium.myRecord.wins }}승 {{ stadium.myRecord.draws }}무 {{ stadium.myRecord.losses }}패
      </div>
    </div>
  </div>
</template>

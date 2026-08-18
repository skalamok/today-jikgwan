<script setup>
import { ref, onMounted } from 'vue'
import client from '../api/client'

const STEPS = [
  { k: '구장', t: '어느 구장으로 갈까', d: '구장마다 분위기가 다릅니다. 아래에서 구역별 만족도를 보고 고르세요.' },
  { k: '예매', t: '표는 어떻게 사나', d: '구단 공식 예매처에서 경기 7일 전부터 예매할 수 있습니다. 주말 인기 경기는 오픈 직후 매진되기도 합니다.' },
  { k: '준비물', t: '뭘 챙겨가나', d: '응원 도구는 현장에서 살 수 있습니다. 돗자리는 외야 자유석에서만 필요하고, 우천 대비 우비를 챙기면 좋습니다. 우산은 반입이 제한될 수 있습니다.' },
  { k: '입장', t: '언제 들어가나', d: '보통 경기 시작 2시간 전부터 입장할 수 있습니다. 먹거리를 사거나 선수 훈련을 보려면 일찍 가는 편이 좋습니다.' },
  { k: '응원', t: '어떻게 응원하나', d: '응원석은 서서 응원하는 분위기이고, 내야 지정석은 앉아서 편하게 보는 편입니다. 처음이라면 응원가를 몰라도 괜찮습니다.' },
]

const step = ref(0)
const stadiums = ref([])
onMounted(async () => { stadiums.value = (await client.get('/stadiums')).data })
</script>

<template>
  <div class="card wide">
    <div class="row" style="flex-wrap:wrap; gap:6px; justify-content:flex-start">
      <button v-for="(s, i) in STEPS" :key="s.k" class="btn small"
              :class="{ ghost: i !== step }" @click="step = i">
        {{ i + 1 }} {{ s.k }}
      </button>
    </div>
  </div>

  <div class="card wide">
    <h2>STEP {{ step + 1 }}. {{ STEPS[step].t }}</h2>
    <p style="font-size:14px; line-height:1.6">{{ STEPS[step].d }}</p>
  </div>

  <div class="card" v-if="step === 0" v-for="s in stadiums" :key="s.id">
    <RouterLink :to="`/stadiums/${s.id}`">
      <div class="mid">{{ s.name }}</div>
      <div class="muted" style="margin-top:3px">{{ s.homeTeams }}</div>
      <div class="muted" style="margin-top:6px">구역별 만족도 보기 →</div>
    </RouterLink>
  </div>

  <button v-if="step < STEPS.length - 1" class="btn" @click="step++">다음 단계 ›</button>
  <RouterLink v-else to="/logs/new"><button class="btn">기록 남기러 가기</button></RouterLink>
</template>

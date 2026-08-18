<script setup>
import { ref, onMounted, computed } from 'vue'
import client from '../api/client'

const DAYS = [
  { v: 'MONDAY', l: '월' }, { v: 'TUESDAY', l: '화' }, { v: 'WEDNESDAY', l: '수' },
  { v: 'THURSDAY', l: '목' }, { v: 'FRIDAY', l: '금' }, { v: 'SATURDAY', l: '토' },
  { v: 'SUNDAY', l: '일' },
]

const stadiums = ref([])
const targetCount = ref(20)
const budgetTotal = ref('')
const maxCostPerGame = ref('')
const selectedDays = ref([])
const selectedStadiums = ref([])
const maxPrecipProb = ref('')
const result = ref(null)
const loading = ref(false)
const error = ref('')

onMounted(async () => { stadiums.value = (await client.get('/stadiums')).data })

// 템플릿에서 넘어오는 ref 는 이미 언랩된 배열이므로 .value 로 접근하지 않는다
function toggle(list, v) {
  const i = list.indexOf(v)
  if (i >= 0) list.splice(i, 1)
  else list.push(v)
}

const achievement = computed(() => {
  if (!result.value) return 0
  return Math.round((result.value.fulfilledCount / result.value.targetCount) * 100)
})

async function generate() {
  loading.value = true; error.value = ''
  try {
    const { data: created } = await client.post('/viewing-plans', {
      seasonYear: 2026,
      targetCount: Number(targetCount.value),
      budgetTotal: budgetTotal.value === '' ? null : Number(budgetTotal.value),
      maxCostPerGame: maxCostPerGame.value === '' ? null : Number(maxCostPerGame.value),
      availableDays: selectedDays.value.length ? selectedDays.value : null,
      stadiumIds: selectedStadiums.value.length ? selectedStadiums.value : null,
      maxPrecipProb: maxPrecipProb.value === '' ? null : Number(maxPrecipProb.value),
    })
    const { data } = await client.post(`/viewing-plans/${created.planId}/generate`,
      { stadiumIds: selectedStadiums.value.length ? selectedStadiums.value : null })
    result.value = data
  } catch (e) {
    error.value = e.response?.data?.message || '편성에 실패했어요'
  } finally { loading.value = false }
}
</script>

<template>
  <div class="card">
    <h2>① 이번 시즌 목표</h2>
    <div class="field">
      <label>관람 목표 경기 수</label>
      <input type="number" v-model="targetCount" min="1" max="144" />
    </div>
    <div class="row" style="gap:8px">
      <div class="field" style="flex:1; margin:0">
        <label>총 예산 (원)</label>
        <input type="number" v-model="budgetTotal" min="0" placeholder="선택" />
      </div>
      <div class="field" style="flex:1; margin:0">
        <label>경기당 상한 (원)</label>
        <input type="number" v-model="maxCostPerGame" min="0" placeholder="선택" />
      </div>
    </div>
  </div>

  <div class="card">
    <h2>② 제약 조건</h2>
    <div class="field">
      <label>관람 가능 요일 <span class="muted">(미선택 시 전체)</span></label>
      <div class="row" style="flex-wrap:wrap; gap:6px; justify-content:flex-start">
        <button v-for="d in DAYS" :key="d.v" class="btn small"
                :class="{ ghost: !selectedDays.includes(d.v) }"
                @click="toggle(selectedDays, d.v)">{{ d.l }}</button>
      </div>
    </div>
    <div class="field">
      <label>이동 가능 구장 <span class="muted">(미선택 시 전체)</span></label>
      <div class="row" style="flex-wrap:wrap; gap:6px; justify-content:flex-start">
        <button v-for="s in stadiums" :key="s.id" class="btn small"
                :class="{ ghost: !selectedStadiums.includes(s.id) }"
                @click="toggle(selectedStadiums, s.id)">{{ s.name }}</button>
      </div>
    </div>
    <div class="field" style="margin:0">
      <label>허용 강수 확률 (%) <span class="muted">(미입력 시 제한 없음)</span></label>
      <input type="number" v-model="maxPrecipProb" min="0" max="100" placeholder="예: 60" />
    </div>
  </div>

  <button class="btn" :disabled="loading" @click="generate">
    {{ loading ? '편성 중…' : '일정 편성하기' }}
  </button>
  <div class="card" v-if="error" style="border-color:#f0c0c0">{{ error }}</div>

  <template v-if="result">
    <div class="card wide">
      <h2>편성 결과</h2>
      <div class="big">{{ result.fulfilledCount }}<span style="font-size:15px"> / {{ result.targetCount }}경기</span></div>
      <div class="muted" style="margin-top:4px">
        달성률 {{ achievement }}% · 예상 비용 {{ result.estimatedCost.toLocaleString() }}원
      </div>
    </div>

    <div class="card">
      <h2>제안된 일정</h2>
      <div v-if="!result.proposed.length" class="muted">조건을 만족하는 경기가 없어요</div>
      <div v-for="p in result.proposed" :key="p.gameId" class="list-item" style="align-items:center">
        <div style="flex:1">
          <div class="mid">
            {{ p.matchup }}
            <span v-if="p.cheerTeamGame" class="chip" style="margin-left:4px">응원팀</span>
          </div>
          <div class="muted">
            {{ p.gameDate }}({{ p.dayOfWeek }}) {{ p.startTime }} · {{ p.stadium }}
            <span v-if="p.precipProbability != null"> · 강수 {{ p.precipProbability }}%</span>
          </div>
        </div>
        <span class="muted">{{ p.estimatedCost.toLocaleString() }}원</span>
      </div>
    </div>

    <div class="card" v-if="result.filteredOut.length">
      <h2>제약별 제외 경기</h2>
      <div class="muted" style="margin:-6px 0 8px">어떤 조건이 결과를 좁혔는지 보여줍니다</div>
      <div v-for="f in result.filteredOut" :key="f.constraint" class="list-item"
           style="align-items:center; padding:8px 0">
        <div style="flex:1">{{ f.label }}</div>
        <span class="muted">{{ f.excluded }}경기 제외</span>
      </div>
    </div>

    <div class="card" v-if="result.unsatisfied.length" style="background:var(--brand-soft); border-color:#d7e3ef">
      <h2>목표를 채우려면</h2>
      <div v-for="u in result.unsatisfied" :key="u.constraint" style="margin-bottom:6px">
        · {{ u.message }}
      </div>
    </div>
  </template>
</template>

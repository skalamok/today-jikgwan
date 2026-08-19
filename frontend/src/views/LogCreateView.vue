<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import client from '../api/client'
import { readTakenAt } from '../lib/exif'

const router = useRouter()
const date = ref('2026-08-15')
const games = ref([])
const gameId = ref(null)
const cheerTeamId = ref(null)
const neutral = ref(false)
const zones = ref([])
const stadiumZoneId = ref(null)
const zoneRating = ref(0)
const memo = ref('')
/* REQ-F-209 함께 간 사람. 회원이 아닐 수도 있어 이름만 적는 줄도 허용한다 */
const companions = ref([])
function addCompanion() { companions.value.push({ name: '' }) }
function removeCompanion(i) { companions.value.splice(i, 1) }
const gameRating = ref(0)
const ticketCost = ref('')
const foodCost = ref('')
const transportCost = ref('')
const visibility = ref('PRIVATE')
const files = ref([])
const toast = ref('')
const saving = ref(false)

const selectedGame = computed(() => games.value.find((g) => g.id === gameId.value))
// 화면설계서 ⑪: ②③④⑤ 가 모두 입력되어야 저장 버튼이 활성화된다
const canSubmit = computed(() =>
  !!gameId.value && (neutral.value || !!cheerTeamId.value) &&
  !!stadiumZoneId.value && zoneRating.value > 0 && !saving.value)

async function loadGames() {
  const { data } = await client.get('/games', { params: { date: date.value } })
  // 아직 시작하지 않은 경기는 선택 목록에서 제외한다
  games.value = data.content.filter((g) => g.status !== 'SCHEDULED')
  // 날짜를 바꾸면 이 함수가 두 번 불릴 수 있다(직접 호출 + date watch).
  // 고른 경기가 새 목록에도 있으면 그대로 둔다. 그러지 않으면 뒤늦게 온 쪽이 선택을 지운다
  const keep = gameId.value
  gameId.value = games.value.some((g) => g.id === keep) ? keep : null
  if (gameId.value === null) { stadiumZoneId.value = null; zones.value = [] }
}
onMounted(loadGames)
watch(date, loadGames)

watch(gameId, async () => {
  cheerTeamId.value = null; stadiumZoneId.value = null
  if (!selectedGame.value) return
  const list = await client.get('/stadiums')
  const stadium = list.data.find((s) => s.name === selectedGame.value.stadium)
  if (!stadium) return
  const { data } = await client.get(`/stadiums/${stadium.id}`)
  zones.value = data.zones
})

/*
 * REQ-F-204 첫 사진의 촬영 일시로 그날 경기를 골라 준다.
 *
 * 서버는 업로드 즉시 위치를 포함한 메타데이터를 지우므로(REQ-NF-007) 촬영 시각도
 * 서버에서는 못 읽는다. 지우기 전에 브라우저에서 읽어 후보를 좁히는 데만 쓴다.
 * 읽지 못하면 아무 일도 없었던 것처럼 수동 선택으로 둔다.
 */
const suggested = ref(null)

async function pickFiles(e) {
  files.value = Array.from(e.target.files)
  suggested.value = null
  if (!files.value.length) return

  const takenAt = await readTakenAt(files.value[0])
  if (!takenAt) return

  const { data } = await client.post('/games/suggest', { takenAt })
  if (!data.length) return

  date.value = takenAt.slice(0, 10)
  await loadGames()
  // 후보 수가 아니라 목록에 실제로 오른 수를 알린다.
  // 아직 결과가 나오지 않은 경기는 고를 수 없어 목록에서 빠진다
  suggested.value = { takenAt, count: games.value.length }
  // 하나뿐이면 골라 둔다. 여럿이면 사람이 고른다
  if (games.value.length === 1) gameId.value = games.value[0].id
}

async function submit() {
  saving.value = true; toast.value = ''
  try {
    const body = {
      gameId: gameId.value,
      cheerTeamId: neutral.value ? null : cheerTeamId.value,
      stadiumZoneId: stadiumZoneId.value,
      zoneRating: zoneRating.value,
      memo: memo.value || null,
      gameRating: gameRating.value || null,
      ticketCost: ticketCost.value === '' ? null : Number(ticketCost.value),
      foodCost: foodCost.value === '' ? null : Number(foodCost.value),
      transportCost: transportCost.value === '' ? null : Number(transportCost.value),
      visibility: visibility.value,
      companions: companions.value
        .map((c) => ({ name: (c.name || '').trim() }))
        .filter((c) => c.name),
    }
    const { data } = await client.post('/attendance-logs', body)

    if (files.value.length) {
      const form = new FormData()
      files.value.forEach((f) => form.append('files', f))
      await client.post(`/attendance-logs/${data.id}/photos`, form)
    }
    router.replace('/logs')
  } catch (e) {
    const code = e.response?.data?.code
    toast.value = code === 'DUPLICATE_ATTENDANCE_LOG'
      ? '이미 이 경기 기록이 있어요'
      : (e.response?.data?.message || '저장에 실패했어요')
  } finally { saving.value = false }
}
</script>

<template>
  <div class="card accent wide">
    <div class="muted">직관 기록</div>
    <div class="big" style="font-size:20px; margin-top:4px">그날을 남겨보세요</div>
    <div class="muted" style="margin-top:6px">
      경기 · 응원팀 · 좌석 구역 · 구역 평가는 필수입니다
    </div>
  </div>

  <div class="card">
    <div class="field">
      <label>① 사진</label>
      <input type="file" accept="image/jpeg,image/png" multiple @change="pickFiles" />
      <p v-if="suggested" class="found">
        📷 사진을 {{ suggested.takenAt.slice(0, 10) }} 에 찍으셨네요
        <template v-if="suggested.count === 1"> — 그날 경기를 골라 뒀어요</template>
        <template v-else-if="suggested.count"> — 그날 경기 {{ suggested.count }}건을 아래에 올려 뒀어요</template>
        <template v-else> — 그날은 아직 결과가 나온 경기가 없어요</template>
      </p>
      <div class="muted" style="margin-top:8px">
        {{ files.length }}/10 · 업로드 시 위치 정보는 자동으로 제거됩니다
      </div>
    </div>
  </div>

  <div class="card">
    <div class="field">
      <label>② 어느 경기였나요? <span class="req">*</span></label>
      <input type="date" v-model="date" style="margin-bottom:8px" />
      <select v-model="gameId">
        <option :value="null">경기를 선택하세요</option>
        <option v-for="g in games" :key="g.id" :value="g.id">
          {{ g.stadium }} · {{ g.homeTeam }} {{ g.homeScore }} : {{ g.awayScore }} {{ g.awayTeam }}
        </option>
      </select>
    </div>

    <div class="field" v-if="selectedGame">
      <label>③ 어느 팀 응원하셨어요? <span class="req">*</span></label>
      <div class="pick">
        <button :class="{ on: !neutral && cheerTeamId === selectedGame.homeTeamId }"
                @click="neutral = false; cheerTeamId = selectedGame.homeTeamId">
          {{ selectedGame.homeTeam }}
        </button>
        <button :class="{ on: !neutral && cheerTeamId === selectedGame.awayTeamId }"
                @click="neutral = false; cheerTeamId = selectedGame.awayTeamId">
          {{ selectedGame.awayTeam }}
        </button>
        <button :class="{ on: neutral }" @click="neutral = true; cheerTeamId = null">중립</button>
      </div>
      <div class="muted" style="margin-top:6px" v-if="neutral">중립 관람은 승패 집계에서 제외됩니다</div>
    </div>

    <div class="field" v-if="zones.length">
      <label>④ 좌석 구역 <span class="req">*</span></label>
      <select v-model="stadiumZoneId">
        <option :value="null">구역을 선택하세요</option>
        <option v-for="z in zones" :key="z.zoneId" :value="z.zoneId">{{ z.name }}</option>
      </select>
    </div>

    <div class="field" v-if="stadiumZoneId">
      <label>⑤ 이 구역 어땠나요? <span class="req">*</span></label>
      <div class="stars">
        <button v-for="n in 5" :key="n" :class="{ on: n <= zoneRating }" @click="zoneRating = n">★</button>
      </div>
      <div class="muted" style="margin-top:6px">다음에 올 사람이 자리를 고르는 데 쓰여요</div>
    </div>
  </div>

  <div class="card">
    <div class="field">
      <label>⑥ 오늘의 한마디</label>
      <textarea v-model="memo" maxlength="1000" placeholder="그날의 기억을 남겨보세요"></textarea>
      <div class="muted" style="text-align:right">{{ memo.length }}/1000</div>
    </div>
    <div class="field">
      <label>⑦ 함께 간 사람</label>
      <div v-for="(c, i) in companions" :key="i" class="row" style="gap:6px; margin-bottom:6px">
        <input v-model="c.name" maxlength="30" placeholder="이름" style="flex:1" />
        <button type="button" class="chip" @click="removeCompanion(i)">×</button>
      </div>
      <button type="button" class="chip" @click="addCompanion">＋ 사람 추가</button>
      <p class="note">서비스를 쓰지 않는 사람도 이름만으로 남길 수 있어요</p>
    </div>
    <div class="field">
      <label>⑧ 비용</label>
      <div class="row" style="gap:8px">
        <input v-model="ticketCost" type="number" min="0" placeholder="티켓" />
        <input v-model="foodCost" type="number" min="0" placeholder="먹거리" />
        <input v-model="transportCost" type="number" min="0" placeholder="교통" />
      </div>
    </div>
    <div class="field">
      <label>⑨ 경기 평점</label>
      <div class="stars">
        <button v-for="n in 5" :key="n" :class="{ on: n <= gameRating }" @click="gameRating = n">★</button>
      </div>
    </div>
    <div class="field" style="margin:0">
      <label>⑩ 공개 범위</label>
      <select v-model="visibility">
        <option value="PRIVATE">비공개</option>
        <option value="PUBLIC">공개 (구장 후기에 노출)</option>
      </select>
      <div class="muted" style="margin-top:6px">비공개여도 구역 점수는 익명으로 집계에 반영됩니다</div>
    </div>
  </div>

  <button class="btn" :disabled="!canSubmit" @click="submit">
    {{ saving ? '저장 중…' : '저장하기' }}
  </button>
  <div class="toast" v-if="toast">{{ toast }}</div>
</template>

<style scoped>
.found { font-size: 13px; color: #16355c; background: #eef1f5;
         border-radius: 8px; padding: 8px 10px; margin: 8px 0 0; }
.pick { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.pick button {
  padding: 13px 8px; border: 1.5px solid var(--line); border-radius: 11px;
  background: #fff; font-size: 14px; font-weight: 700; color: var(--ink-2);
  cursor: pointer; transition: all .15s;
}
.pick button:hover { border-color: var(--brand-2); }
.pick button.on {
  background: var(--brand); border-color: var(--brand); color: #fff;
  box-shadow: 0 2px 8px rgba(22,53,92,.20);
}
</style>

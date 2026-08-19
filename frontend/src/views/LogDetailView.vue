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

/* REQ-F-212 기록 수정.
   경기는 바꾸지 않는다. 경기를 옮기는 것은 사실상 다른 기록이라 삭제 후 다시 쓰는 쪽이 낫다.
   여기서는 앉은 자리와 감상처럼 나중에 고치고 싶어지는 것만 연다. */
const editing = ref(false)
const zones = ref([])
const teams = ref([])
const form = ref({})
const saving = ref(false)
const toast = ref('')

async function openEdit() {
  const l = log.value
  form.value = {
    gameId: l.gameId,
    cheerTeamId: l.cheerTeamId ?? null,
    stadiumZoneId: l.stadiumZoneId,
    zoneRating: l.zoneRating,
    gameRating: l.gameRating ?? null,
    memo: l.memo ?? '',
    ticketCost: l.ticketCost ?? null,
    foodCost: l.foodCost ?? null,
    transportCost: l.transportCost ?? null,
    visibility: l.visibility,
    // 상세는 표시용 이름만 주므로 수정 폼도 이름으로 다룬다.
    // 회원을 다시 고르는 것은 이 화면의 일이 아니다
    companions: (l.companions || []).map((n) => ({ name: n })),
  }
  if (!zones.value.length) {
    const list = await client.get('/stadiums')
    const stadium = list.data.find((s) => s.name === l.stadiumName)
    if (stadium) zones.value = (await client.get(`/stadiums/${stadium.id}`)).data.zones
  }
  if (!teams.value.length) {
    // 그 경기에 나온 두 팀만 고를 수 있다 (REQ-F-202)
    const g = (await client.get(`/games/${l.gameId}`)).data
    teams.value = [
      { id: g.homeTeamId, name: g.homeTeam },
      { id: g.awayTeamId, name: g.awayTeam },
    ].filter((t) => t.id)
  }
  editing.value = true
}

async function save() {
  saving.value = true; toast.value = ''
  try {
    const body = { ...form.value }
    if (body.memo === '') body.memo = null
    body.companions = (form.value.companions || [])
      .map((c) => ({ name: (c.name || '').trim() }))
      .filter((c) => c.name)
    log.value = (await client.put(`/attendance-logs/${route.params.id}`, body)).data
    editing.value = false
  } catch (e) {
    toast.value = e.response?.data?.message || '수정하지 못했어요'
  } finally { saving.value = false }
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

    <div class="card" v-if="log.companions && log.companions.length">
      <h2>함께 간 사람</h2>
      <div class="kv" v-for="(n, i) in log.companions" :key="i"><span>{{ n }}</span></div>
    </div>

    <div class="card" v-if="log.totalCost">
      <h2>비용</h2>
      <div class="kv" v-if="log.ticketCost"><span>티켓</span><b>{{ log.ticketCost.toLocaleString() }}원</b></div>
      <div class="kv" v-if="log.foodCost"><span>먹거리</span><b>{{ log.foodCost.toLocaleString() }}원</b></div>
      <div class="kv" v-if="log.transportCost"><span>교통</span><b>{{ log.transportCost.toLocaleString() }}원</b></div>
      <div class="kv total"><span>합계</span><b>{{ log.totalCost.toLocaleString() }}원</b></div>
    </div>

    <div class="acts">
      <button class="btn" @click="openEdit" v-if="!editing">기록 수정</button>
      <button class="btn ghost" @click="removeLog">기록 삭제</button>
    </div>

    <div class="card" v-if="editing">
      <h2>기록 수정</h2>
      <p class="hint">경기와 사진은 여기서 바꾸지 않는다. 경기를 옮기려면 삭제 후 다시 쓴다.</p>

      <div class="field">
        <label>응원팀</label>
        <select v-model="form.cheerTeamId">
          <option :value="null">중립 관람 (승패 집계 제외)</option>
          <option v-for="t in teams" :key="t.id" :value="t.id">{{ t.name }}</option>
        </select>
      </div>

      <div class="field">
        <label>좌석 구역</label>
        <select v-model="form.stadiumZoneId">
          <option v-for="z in zones" :key="z.zoneId" :value="z.zoneId">{{ z.name }}</option>
        </select>
      </div>

      <div class="field">
        <label>구역 만족도 <span class="req">필수</span></label>
        <div class="stars">
          <button v-for="n in 5" :key="n" type="button"
                  :class="{ on: form.zoneRating >= n }" @click="form.zoneRating = n">★</button>
        </div>
      </div>

      <div class="field">
        <label>경기 평점</label>
        <div class="stars">
          <button v-for="n in 5" :key="n" type="button"
                  :class="{ on: form.gameRating >= n }" @click="form.gameRating = n">★</button>
        </div>
      </div>

      <div class="field">
        <label>그날의 한마디</label>
        <textarea v-model="form.memo" maxlength="1000" rows="3"
                  placeholder="그날의 기억을 남겨보세요"></textarea>
      </div>

      <div class="field">
        <label>함께 간 사람</label>
        <div v-for="(c, i) in form.companions" :key="i" class="row2">
          <input v-model="c.name" maxlength="30" placeholder="이름" />
          <button type="button" class="mini" @click="form.companions.splice(i, 1)">×</button>
        </div>
        <button type="button" class="mini wide" @click="form.companions.push({ name: '' })">
          ＋ 사람 추가
        </button>
      </div>

      <div class="field">
        <label>비용</label>
        <div class="costs">
          <input v-model.number="form.ticketCost" type="number" min="0" placeholder="티켓" />
          <input v-model.number="form.foodCost" type="number" min="0" placeholder="먹거리" />
          <input v-model.number="form.transportCost" type="number" min="0" placeholder="교통" />
        </div>
      </div>

      <div class="field">
        <label>공개 범위</label>
        <select v-model="form.visibility">
          <option value="PRIVATE">비공개</option>
          <option value="PUBLIC">공개</option>
        </select>
      </div>

      <p class="err" v-if="toast">{{ toast }}</p>
      <div class="acts">
        <button class="btn" :disabled="saving || !form.zoneRating" @click="save">
          {{ saving ? '저장 중…' : '저장' }}
        </button>
        <button class="btn ghost" @click="editing = false">취소</button>
      </div>
    </div>
  </template>

  <div v-if="viewer" class="viewer" @click="viewer = null">
    <img :src="viewer.originalUrl" />
  </div>
</template>

<style scoped>
.acts { display: flex; gap: 8px; }
.acts .btn { flex: 1; }
.hint { font-size: 12px; color: #777; margin: -4px 0 12px; }
.field { margin-bottom: 14px; }
.field label { display: block; font-size: 13px; font-weight: 700; margin-bottom: 6px; }
.field .req { color: #c0392b; font-weight: 400; font-size: 11px; margin-left: 4px; }
.field select, .field textarea, .field input {
  width: 100%; padding: 9px 10px; border: 1px solid #d8dde3; border-radius: 8px;
  font-size: 14px; font-family: inherit; box-sizing: border-box;
}
.stars { display: flex; gap: 4px; }
.stars button {
  border: 0; background: none; font-size: 24px; color: #d8dde3; cursor: pointer; padding: 0;
}
.stars button.on { color: #f5a623; }
.row2 { display: flex; gap: 6px; margin-bottom: 6px; }
.row2 input { flex: 1; }
.mini { border: 1px solid #d8dde3; background: #fff; border-radius: 8px; padding: 8px 12px;
        font-size: 13px; cursor: pointer; font-family: inherit; }
.mini.wide { width: 100%; }
.costs { display: grid; grid-template-columns: repeat(3, 1fr); gap: 6px; }
.err { color: #c0392b; font-size: 13px; margin: 0 0 10px; }
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

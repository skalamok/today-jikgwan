<script setup>
import { ref, computed, onMounted } from 'vue'
import client from '../api/client'
import TeamMark from '../components/TeamMark.vue'

// SCR-ADM-001 (REQ-F-601 ~ 604, REQ-F-607)
const tab = ref('review')
const loading = ref(true)
const error = ref('')
const denied = ref(false)

// ── 제보 검토 ────────────────────────────────────────────────
const rows = ref([])
const openId = ref(null)
const form = ref({})
const revisions = ref([])
const saving = ref(false)

const STATUS_LABEL = {
  SCHEDULED: '예정', IN_PROGRESS: '진행 중', FINISHED: '종료',
  CANCELED: '취소', SUSPENDED: '서스펜디드',
}

async function loadReview() {
  loading.value = true
  try {
    rows.value = (await client.get('/admin/games/unconfirmed')).data
  } catch (e) {
    if (e.response?.status === 403) denied.value = true
    else error.value = e.response?.data?.message || '불러오지 못했어요.'
  } finally { loading.value = false }
}

async function open(row) {
  if (openId.value === row.game.id) { openId.value = null; return }
  openId.value = row.game.id
  // 최다 일치 제보를 기본값으로 채워 둔다. 운영자가 대부분 그대로 확정하기 때문이다.
  const top = row.reports[0]
  form.value = {
    status: 'FINISHED',
    homeScore: top ? top.homeScore : null,
    awayScore: top ? top.awayScore : null,
    reason: '',
  }
  revisions.value = (await client.get(`/admin/games/${row.game.id}/revisions`)).data
}

const canRevise = computed(() => {
  const f = form.value
  if (!f.reason?.trim()) return false
  if (f.status === 'FINISHED') return f.homeScore !== null && f.awayScore !== null
  return true
})

async function revise(gameId) {
  saving.value = true; error.value = ''
  try {
    const { data } = await client.post(`/admin/games/${gameId}/revisions`, {
      status: form.value.status,
      homeScore: form.value.status === 'FINISHED' ? Number(form.value.homeScore) : null,
      awayScore: form.value.status === 'FINISHED' ? Number(form.value.awayScore) : null,
      reason: form.value.reason,
    })
    openId.value = null
    await loadReview()
    notice.value = `정정했어요. ${data.recalculatedUsers}명의 전적을 다시 계산했습니다.`
  } catch (e) {
    error.value = e.response?.data?.message || '정정하지 못했어요.'
  } finally { saving.value = false }
}

const notice = ref('')

// ── 경기 등록 ────────────────────────────────────────────────
const teams = ref([])
const stadiums = ref([])
const reg = ref({ startAt: '', stadiumId: '', homeTeamId: '', awayTeamId: '' })
const regSaving = ref(false)

const canRegister = computed(() => {
  const r = reg.value
  return r.startAt && r.stadiumId && r.homeTeamId && r.awayTeamId
      && r.homeTeamId !== r.awayTeamId && !regSaving.value
})

async function register() {
  regSaving.value = true; error.value = ''
  try {
    const startAt = new Date(reg.value.startAt)
    await client.post('/admin/games', {
      seasonYear: startAt.getFullYear(),
      startAt: startAt.toISOString(),
      stadiumId: Number(reg.value.stadiumId),
      homeTeamId: Number(reg.value.homeTeamId),
      awayTeamId: Number(reg.value.awayTeamId),
    })
    notice.value = '경기를 등록했어요.'
    reg.value = { startAt: '', stadiumId: '', homeTeamId: '', awayTeamId: '' }
  } catch (e) {
    error.value = e.response?.data?.message || '등록하지 못했어요.'
  } finally { regSaving.value = false }
}

// ── 구장 · 구역 관리 (REQ-F-605) ────────────────────────────
const zoneStadiumId = ref('')
const zones = ref([])
const newZone = ref('')
const zoneBusy = ref(false)

async function loadZones() {
  if (!zoneStadiumId.value) { zones.value = []; return }
  zones.value = (await client.get(`/admin/stadiums/${zoneStadiumId.value}/zones`)).data
}

async function addZone() {
  if (!newZone.value.trim()) return
  zoneBusy.value = true; error.value = ''
  try {
    await client.post(`/admin/stadiums/${zoneStadiumId.value}/zones`, { name: newZone.value.trim() })
    newZone.value = ''
    await loadZones()
  } catch (e) {
    error.value = e.response?.data?.message || '추가하지 못했어요.'
  } finally { zoneBusy.value = false }
}

async function toggleZone(zone) {
  zoneBusy.value = true; error.value = ''
  try {
    await client.patch(`/admin/zones/${zone.id}`, { active: !zone.active })
    await loadZones()
  } catch (e) {
    error.value = e.response?.data?.message || '변경하지 못했어요.'
  } finally { zoneBusy.value = false }
}

async function deleteZone(zone) {
  if (!confirm(`'${zone.name}' 구역을 지울까요? 되돌릴 수 없어요.`)) return
  zoneBusy.value = true; error.value = ''
  try {
    await client.delete(`/admin/zones/${zone.id}`)
    await loadZones()
  } catch (e) {
    error.value = e.response?.data?.message || '지우지 못했어요.'
  } finally { zoneBusy.value = false }
}

async function renameZone(zone) {
  const name = prompt('구역 이름', zone.name)
  if (!name || name === zone.name) return
  zoneBusy.value = true; error.value = ''
  try {
    await client.patch(`/admin/zones/${zone.id}`, { name })
    await loadZones()
  } catch (e) {
    error.value = e.response?.data?.message || '변경하지 못했어요.'
  } finally { zoneBusy.value = false }
}

onMounted(async () => {
  const [t, s] = await Promise.all([client.get('/teams'), client.get('/stadiums')])
  teams.value = t.data; stadiums.value = s.data
  await loadReview()
})

function fmt(iso) {
  return new Date(iso).toLocaleString('ko-KR',
    { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}
</script>

<template>
  <div v-if="denied" class="card wide empty">
    <div class="empty-ico">🔒</div>
    운영자만 볼 수 있는 화면이에요
  </div>

  <template v-else>
  <div class="card wide">
    <div class="tabs" style="margin-top:0">
      <button :class="{ on: tab === 'review' }" @click="tab = 'review'">
        제보 검토<span v-if="rows.length" class="cnt">{{ rows.length }}</span>
      </button>
      <button :class="{ on: tab === 'register' }" @click="tab = 'register'">경기 등록</button>
      <button :class="{ on: tab === 'zones' }" @click="tab = 'zones'">구장 · 구역</button>
    </div>
    <div v-if="notice" class="notice">{{ notice }}</div>
    <div v-if="error" class="err">{{ error }}</div>
  </div>

  <!-- REQ-F-607 제보가 엇갈려 자동 확정되지 않은 경기 -->
  <template v-if="tab === 'review'">
    <div v-if="loading" class="card wide"><div class="skeleton" style="height:56px"></div></div>
    <div v-else-if="!rows.length" class="card wide empty">
      <div class="empty-ico">✅</div>
      검토할 경기가 없어요
    </div>
    <div v-else v-for="row in rows" :key="row.game.id" class="card wide">
      <div class="row head" @click="open(row)">
        <div style="flex:1; min-width:0">
          <div class="matchup">
            <TeamMark :name="row.game.awayTeam" size="sm" />
            <span class="mid">{{ row.game.awayTeam }}</span>
            <span class="muted">vs</span>
            <TeamMark :name="row.game.homeTeam" size="sm" />
            <span class="mid">{{ row.game.homeTeam }}</span>
          </div>
          <div class="muted" style="margin-top:5px; font-size:12px">
            {{ fmt(row.game.startAt) }} · {{ row.game.stadium }} ·
            {{ STATUS_LABEL[row.game.status] }} · 관람 기록 {{ row.attendeeCount }}건
          </div>
        </div>
        <div class="tally">
          <template v-if="row.reports.length">
            <span v-for="r in row.reports" :key="`${r.homeScore}-${r.awayScore}`" class="pill"
                  :class="{ top: r.count >= row.threshold }">
              {{ r.homeScore }}:{{ r.awayScore }} <b>{{ r.count }}</b>
            </span>
          </template>
          <span v-else class="muted" style="font-size:12px">제보 없음</span>
        </div>
      </div>

      <div v-if="openId === row.game.id" class="panel">
        <div class="muted" style="font-size:12px; margin-bottom:10px">
          확정 기준은 일치 제보 {{ row.threshold }}건입니다. 기준에 못 미쳐 자동 확정되지 않았어요.
        </div>

        <div class="grid2">
          <div class="field">
            <label>처리</label>
            <select v-model="form.status">
              <option value="FINISHED">종료 (스코어 확정)</option>
              <option value="CANCELED">취소 (우천 노게임 등)</option>
              <option value="SUSPENDED">서스펜디드</option>
            </select>
          </div>
          <div class="field" v-if="form.status === 'FINISHED'">
            <label>스코어 (홈 : 원정)</label>
            <div class="score-row">
              <input v-model.number="form.homeScore" type="number" min="0" />
              <span>:</span>
              <input v-model.number="form.awayScore" type="number" min="0" />
            </div>
          </div>
        </div>

        <div class="field">
          <label>정정 사유 <span class="req">*</span></label>
          <input v-model="form.reason" maxlength="255" placeholder="예: 우천 노게임 처리" />
        </div>

        <!-- REQ-F-603 이력은 추가 전용이라 지우거나 고칠 수 없다 -->
        <div v-if="revisions.length" class="history">
          <div class="mid" style="font-size:13px; margin-bottom:6px">정정 이력</div>
          <div v-for="r in revisions" :key="r.id" class="hrow">
            <span class="muted">{{ fmt(r.revisedAt) }}</span>
            <span>
              {{ STATUS_LABEL[r.beforeStatus] }} {{ r.beforeScore || '' }}
              → {{ STATUS_LABEL[r.afterStatus] }} {{ r.afterScore || '' }}
            </span>
            <span class="muted">{{ r.reason }} · {{ r.revisedBy }}</span>
          </div>
        </div>

        <div class="muted" style="font-size:12px; margin:10px 0">
          정정하면 이 경기를 기록한 {{ row.attendeeCount }}명의 전적을 다시 계산하고 알림을 보냅니다.
        </div>
        <button class="btn" :disabled="!canRevise || saving" @click="revise(row.game.id)">
          {{ saving ? '처리 중…' : '정정 확정' }}
        </button>
      </div>
    </div>
  </template>

  <!-- REQ-F-601 외부 소스를 확보하지 못했을 때의 기본 등록 수단 -->
  <div v-else-if="tab === 'register'" class="card wide">
    <div class="field">
      <label>경기 일시 <span class="req">*</span></label>
      <input v-model="reg.startAt" type="datetime-local" />
    </div>
    <div class="grid2">
      <div class="field">
        <label>구장 <span class="req">*</span></label>
        <select v-model="reg.stadiumId">
          <option value="">선택</option>
          <option v-for="s in stadiums" :key="s.id" :value="s.id">{{ s.name }}</option>
        </select>
      </div>
      <div class="field">
        <label>홈 <span class="req">*</span></label>
        <select v-model="reg.homeTeamId">
          <option value="">선택</option>
          <option v-for="t in teams" :key="t.id" :value="t.id">{{ t.shortName }}</option>
        </select>
      </div>
    </div>
    <div class="field">
      <label>원정 <span class="req">*</span></label>
      <select v-model="reg.awayTeamId">
        <option value="">선택</option>
        <option v-for="t in teams" :key="t.id" :value="t.id" :disabled="t.id === reg.homeTeamId">
          {{ t.shortName }}
        </option>
      </select>
    </div>
    <button class="btn" :disabled="!canRegister" @click="register">
      {{ regSaving ? '등록 중…' : '경기 등록' }}
    </button>
  </div>

  <!-- REQ-F-605 구역은 관람 기록이 참조하므로 지우지 않고 비활성으로만 돌린다 -->
  <div v-else class="card wide">
    <div class="field">
      <label>구장</label>
      <select v-model="zoneStadiumId" @change="loadZones">
        <option value="">선택</option>
        <option v-for="s in stadiums" :key="s.id" :value="s.id">{{ s.name }}</option>
      </select>
    </div>

    <template v-if="zoneStadiumId">
      <div v-if="!zones.length" class="muted" style="margin-bottom:12px">
        등록된 구역이 없어요
      </div>
      <div v-for="z in zones" :key="z.id" class="zone" :class="{ off: !z.active }">
        <div style="flex:1; min-width:0">
          <div class="mid">
            {{ z.name }}
            <span v-if="!z.active" class="off-tag">비활성</span>
          </div>
          <div class="muted" style="font-size:12px; margin-top:2px">
            관람 기록 {{ z.logCount }}건
          </div>
        </div>
        <button class="btn ghost small" :disabled="zoneBusy" @click="renameZone(z)">이름</button>
        <button class="btn ghost small" :disabled="zoneBusy" @click="toggleZone(z)">
          {{ z.active ? '비활성화' : '되살리기' }}
        </button>
        <!-- 기록이 딸린 구역은 지울 수 없다. 버튼 자체를 내보내지 않는다 -->
        <button v-if="!z.logCount" class="btn ghost small danger"
                :disabled="zoneBusy" @click="deleteZone(z)">삭제</button>
      </div>

      <div class="add-zone">
        <input v-model="newZone" maxlength="50" placeholder="새 구역 이름"
               @keyup.enter="addZone" />
        <button class="btn" style="width:auto; padding:0 18px"
                :disabled="!newZone.trim() || zoneBusy" @click="addZone">추가</button>
      </div>
      <div class="muted" style="font-size:12px; margin-top:10px">
        관람 기록이 있는 구역은 지울 수 없습니다. 과거 기록이 이 구역을 참조하고 있고
        구역별 만족도 집계의 단위이기 때문이에요. 그런 구역은 비활성으로 돌리면
        새 기록에서만 빠집니다. 아무도 쓴 적 없는 구역만 삭제 버튼이 나옵니다.
      </div>
    </template>
  </div>
  </template>
</template>

<style scoped>
.tabs { display: flex; gap: 8px; margin-top: 12px; }
.tabs button {
  flex: 1; height: 38px; border: 1px solid var(--line); border-radius: 9px;
  background: var(--card); color: var(--muted); font-weight: 700; font-size: 13px; cursor: pointer;
}
.tabs button.on { border-color: var(--accent); color: var(--text); background: var(--brand-soft); }
.cnt {
  margin-left: 6px; padding: 1px 6px; border-radius: 999px;
  background: var(--accent); color: #fff; font-size: 11px;
}
.notice {
  margin-top: 12px; padding: 9px 12px; border-radius: 9px;
  background: var(--brand-soft); font-size: 13px;
}
.head { cursor: pointer; align-items: flex-start; gap: 12px; }
.matchup { display: flex; align-items: center; gap: 8px; }
.score-row { display: flex; align-items: center; gap: 8px; }
.score-row input { width: 72px; }
.zone {
  display: flex; align-items: center; gap: 8px;
  padding: 11px 0; border-bottom: 1px solid var(--line);
}
.zone.off { opacity: .55; }
.off-tag {
  margin-left: 6px; padding: 1px 7px; border-radius: 999px;
  background: var(--card-soft); font-size: 11px; font-weight: 600; color: var(--muted);
}
.add-zone { display: flex; gap: 8px; margin-top: 14px; }
.add-zone input { flex: 1; }
.btn.danger { color: var(--danger, #c0392b); }
.tally { display: flex; flex-wrap: wrap; gap: 5px; justify-content: flex-end; max-width: 45%; }
.pill {
  padding: 3px 9px; border-radius: 999px; background: var(--card-soft);
  font-size: 12px; white-space: nowrap;
}
.pill.top { background: var(--brand-soft); }
.panel { margin-top: 14px; padding-top: 14px; border-top: 1px solid var(--line); }
.grid2 { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.history {
  margin-top: 4px; padding: 10px 12px; border-radius: 9px; background: var(--card-soft);
}
.hrow {
  display: grid; grid-template-columns: 110px 1fr; gap: 4px 10px;
  font-size: 12px; padding: 4px 0;
}
.hrow span:last-child { grid-column: 1 / -1; }
@media (max-width: 560px) {
  .grid2 { grid-template-columns: minmax(0, 1fr); }
  .tally { max-width: 100%; justify-content: flex-start; }
}
</style>

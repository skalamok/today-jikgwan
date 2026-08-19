<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import client from '../api/client'
import { logout } from '../store/auth'
import TeamPicker from '../components/TeamPicker.vue'
import TeamMark from '../components/TeamMark.vue'

const route = useRoute()
const router = useRouter()
const stats = ref(null)
const logs = ref([])
const blocks = ref([])
const badges = ref([])
const me = ref(null)
const editingTeam = ref(false)
const savingTeam = ref(false)
const pickedTeamId = ref(null)
const loading = ref(true)

onMounted(async () => {
  const [s, l, b, bd, m] = await Promise.all([
    client.get('/stats/me/summary'),
    client.get('/attendance-logs'),
    client.get('/users/me/blocks'),
    client.get('/users/me/badges'),
    client.get('/users/me'),
  ])
  stats.value = s.data; logs.value = l.data; blocks.value = b.data
  badges.value = bd.data; me.value = m.data
  pickedTeamId.value = m.data.favoriteTeamId
  // 소셜 가입 직후에는 응원팀부터 고르게 한다 (OAuthCallbackView 가 붙여 보내는 신호)
  if (route.query.welcome && !m.data.favoriteTeamId) editingTeam.value = true
  loading.value = false
})

async function saveTeam() {
  savingTeam.value = true
  try {
    const { data } = await client.patch('/users/me', { favoriteTeamId: pickedTeamId.value })
    me.value = data
    editingTeam.value = false
  } finally { savingTeam.value = false }
}

async function unblock(userId) {
  await client.delete(`/users/me/blocks/${userId}`)
  blocks.value = blocks.value.filter((b) => b.userId !== userId)
}

function doLogout() { logout(); router.replace('/login') }
</script>

<template>
  <div v-if="loading"><div class="card"><div class="skeleton" style="height:60px"></div></div></div>
  <template v-else>
    <div class="card wide profile">
      <div class="row" style="align-items:center; gap:14px">
        <TeamMark v-if="me.favoriteTeamShort" :name="me.favoriteTeamShort" size="lg" />
        <div v-else class="no-team">?</div>
        <div style="flex:1; min-width:0">
          <div class="big" style="font-size:19px">{{ me.nickname }}</div>
          <div class="muted" style="margin-top:3px">
            {{ me.favoriteTeam ? `${me.favoriteTeam} 팬` : '응원팀을 아직 고르지 않았어요' }}
          </div>
        </div>
        <button class="btn ghost small" @click="editingTeam = !editingTeam">
          {{ editingTeam ? '취소' : (me.favoriteTeamId ? '변경' : '선택') }}
        </button>
      </div>

      <div v-if="editingTeam" style="margin-top:14px">
        <TeamPicker v-model="pickedTeamId" />
        <div class="muted" style="font-size:12px; margin:10px 0">
          응원팀을 바꿔도 이미 남긴 기록의 승패는 그대로 유지됩니다.
        </div>
        <button class="btn" :disabled="!pickedTeamId || savingTeam" @click="saveTeam">
          {{ savingTeam ? '저장 중…' : '저장' }}
        </button>
      </div>

      <div v-if="me.socialAccounts?.length" class="row" style="margin-top:12px; gap:6px">
        <span class="muted" style="font-size:12px">연결된 계정</span>
        <span v-for="p in me.socialAccounts" :key="p" class="chip">{{ p }}</span>
      </div>
    </div>

    <div class="card accent wide">
      <h2>내 활동</h2>
      <div class="row" style="align-items:flex-end">
        <div>
          <div class="big">{{ logs.length }}<span style="font-size:16px">개 기록</span></div>
          <div class="mid" style="margin-top:6px; opacity:.92">
            {{ stats.games }}경기 {{ stats.wins }}승 {{ stats.draws }}무 {{ stats.losses }}패
          </div>
        </div>
        <div v-if="stats.totalCost" style="text-align:right">
          <div class="muted">누적 지출</div>
          <div class="mid" style="font-size:18px">{{ stats.totalCost.toLocaleString() }}원</div>
        </div>
      </div>
    </div>

    <div class="card wide">
      <h2>배지 {{ badges.filter(b => b.achieved).length }} / {{ badges.length }}</h2>
      <div class="badges">
        <div v-for="b in badges" :key="b.code" class="bg" :class="{ on: b.achieved }">
          <div class="em">{{ b.achieved ? '🏅' : '🔒' }}</div>
          <div class="nm">{{ b.name }}</div>
          <div class="ds">{{ b.description }}</div>
        </div>
      </div>
    </div>

    <div class="card">
      <h2>차단한 사용자</h2>
      <div v-if="!blocks.length" class="muted">차단한 사용자가 없어요</div>
      <div v-for="b in blocks" :key="b.userId" class="list-item" style="align-items:center; padding:9px 0">
        <div style="flex:1">{{ b.nickname }}</div>
        <button class="btn ghost small" @click="unblock(b.userId)">해제</button>
      </div>
    </div>

    <div class="card">
      <h2>계정</h2>
      <button class="btn ghost" @click="doLogout">로그아웃</button>
    </div>
  </template>


</template>
<style scoped>
.badges { display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 10px; }
.bg {
  padding: 14px 10px; border-radius: 12px; background: var(--card-soft);
  text-align: center; opacity: .5;
}
.bg.on { opacity: 1; background: var(--brand-soft); }
.em { font-size: 24px; }
.nm { font-size: 12.5px; font-weight: 800; margin-top: 6px; }
.ds { font-size: 10.5px; color: var(--muted); margin-top: 3px; line-height: 1.4; }

.no-team {
  width: 46px; height: 46px; border-radius: 50%; flex: none;
  display: grid; place-items: center;
  background: var(--card-soft); color: var(--muted); font-weight: 800;
}
.chip {
  padding: 3px 9px; border-radius: 999px; background: var(--card-soft);
  font-size: 11px; font-weight: 700; text-transform: capitalize;
}
</style>

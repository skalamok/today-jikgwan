<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import client from '../api/client'
import { logout } from '../store/auth'

const router = useRouter()
const stats = ref(null)
const logs = ref([])
const blocks = ref([])
const loading = ref(true)

onMounted(async () => {
  const [s, l, b] = await Promise.all([
    client.get('/stats/me/summary'),
    client.get('/attendance-logs'),
    client.get('/users/me/blocks'),
  ])
  stats.value = s.data; logs.value = l.data; blocks.value = b.data
  loading.value = false
})

async function unblock(userId) {
  await client.delete(`/users/me/blocks/${userId}`)
  blocks.value = blocks.value.filter((b) => b.userId !== userId)
}

function doLogout() { logout(); router.replace('/login') }
</script>

<template>
  <div v-if="loading"><div class="card"><div class="skeleton" style="height:60px"></div></div></div>
  <template v-else>
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

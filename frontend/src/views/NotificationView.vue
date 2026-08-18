<script setup>
import { ref, onMounted } from 'vue'
import client from '../api/client'

const items = ref([]); const unread = ref(0); const loading = ref(true)
const ICON = { BADGE_ACHIEVED: '🏅', COMPANION_CONFIRMED: '🤝', GAME_REVISED: '🔄' }

onMounted(async () => {
  const { data } = await client.get('/notifications')
  items.value = data.items; unread.value = data.unreadCount; loading.value = false
  if (data.unreadCount) {
    await client.put('/notifications/read')
  }
})
</script>

<template>
  <div class="card wide">
    <h2>알림</h2>
    <div v-if="loading"><div class="skeleton" style="height:40px"></div></div>
    <div v-else-if="!items.length" class="empty"><p>받은 알림이 없어요</p></div>
    <RouterLink v-else v-for="n in items" :key="n.id" :to="n.linkUrl || '/'"
                class="list-item" style="align-items:center">
      <div class="ico">{{ ICON[n.type] || '🔔' }}</div>
      <div style="flex:1; min-width:0">
        <div class="row">
          <span class="mid">{{ n.title }}</span>
          <span v-if="!n.read" class="dot"></span>
        </div>
        <div class="muted" style="margin-top:3px">{{ n.body }}</div>
        <div class="muted" style="font-size:11px; margin-top:3px">
          {{ n.createdAt.slice(5, 16).replace('T', ' ') }}
        </div>
      </div>
    </RouterLink>
  </div>
</template>

<style scoped>
.ico {
  width: 38px; height: 38px; border-radius: 11px; flex: none;
  display: flex; align-items: center; justify-content: center;
  background: var(--card-soft); font-size: 19px;
}
.dot { width: 7px; height: 7px; border-radius: 50%; background: var(--lose); flex: none; }
</style>

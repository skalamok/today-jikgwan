<script setup>
import { ref, onMounted } from 'vue'
import client from '../api/client'

const posts = ref([])
onMounted(async () => { posts.value = (await client.get('/companion-posts')).data })
</script>

<template>
  <div class="card">
    <h2>모집 중인 동행</h2>
    <div v-if="!posts.length" class="empty"><p>모집 중인 동행이 없어요</p></div>
    <RouterLink v-for="p in posts" :key="p.id" :to="`/companions/${p.id}`"
                class="list-item" style="align-items:center">
      <div style="flex:1">
        <div class="mid">{{ p.gameLabel }}</div>
        <div class="muted">{{ p.startAt.slice(0, 10) }} · {{ p.stadium }}</div>
      </div>
      <span class="chip">{{ p.confirmedCount }}/{{ p.capacity }}</span>
    </RouterLink>
  </div>
</template>

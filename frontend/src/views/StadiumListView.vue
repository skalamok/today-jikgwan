<script setup>
import { ref, onMounted } from 'vue'
import client from '../api/client'

const stadiums = ref([])
onMounted(async () => { stadiums.value = (await client.get('/stadiums')).data })
</script>

<template>
  <div class="card wide">
    <h2>전국 구장</h2>
    <RouterLink v-for="s in stadiums" :key="s.id" :to="`/stadiums/${s.id}`"
                class="list-item" style="align-items:center">
      <div style="flex:1">
        <div class="mid">{{ s.name }}</div>
        <div class="muted">{{ s.homeTeams }}</div>
      </div>
      <span class="muted">›</span>
    </RouterLink>
  </div>
</template>

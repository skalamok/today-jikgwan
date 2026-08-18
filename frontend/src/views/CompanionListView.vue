<script setup>
import { ref, onMounted } from 'vue'
import client from '../api/client'
import TeamMark from '../components/TeamMark.vue'

const posts = ref([])
onMounted(async () => { posts.value = (await client.get('/companion-posts')).data })
</script>

<template>
  <div class="card wide">
    <h2>모집 중인 동행</h2>
    <div v-if="!posts.length" class="empty">
      <div class="em">🤝</div>
      <p>모집 중인 동행이 없어요</p>
    </div>
    <RouterLink v-for="p in posts" :key="p.id" :to="`/companions/${p.id}`"
                class="list-item" style="align-items:center">
      <div style="flex:1; min-width:0">
        <div class="row" style="justify-content:flex-start; gap:7px">
          <TeamMark v-for="t in p.gameLabel.split(' vs ')" :key="t" :name="t" size="sm" />
        </div>
        <div class="muted" style="margin-top:4px">{{ p.startAt.slice(0, 10) }} · {{ p.stadium }}</div>
        <div class="dots">
          <i v-for="n in p.capacity" :key="n" :class="{ on: n <= p.confirmedCount }"></i>
          <span class="muted" style="margin-left:6px; font-size:11.5px">
            {{ p.confirmedCount }}/{{ p.capacity }}명
          </span>
        </div>
      </div>
      <span class="chip" :class="{ warn: p.status !== 'OPEN' }">
        {{ p.status === 'OPEN' ? '모집중' : '마감' }}
      </span>
    </RouterLink>
  </div>
</template>

<style scoped>
.dots { display: flex; align-items: center; gap: 4px; margin-top: 8px; }
.dots i {
  width: 9px; height: 9px; border-radius: 50%;
  background: var(--draw-bg); display: inline-block;
}
.dots i.on { background: var(--brand-2); }
</style>

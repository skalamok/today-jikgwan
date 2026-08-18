<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import client from '../api/client'

const route = useRoute()
const post = ref(null)
const toast = ref('')
const busy = ref(false)

async function load() { post.value = (await client.get(`/companion-posts/${route.params.id}`)).data }
onMounted(load)

// 화면설계서 SCR-MATE-002 상태 정의표에 따른 버튼 분기
const button = computed(() => {
  const p = post.value
  if (!p) return null
  if (p.myStatus === 'AUTHOR') return { label: '내가 등록한 모집', disabled: true }
  if (p.myStatus === 'CONFIRMED') return { label: '참여 취소', action: 'cancel' }
  if (p.status !== 'OPEN') return { label: '정원이 찼어요', disabled: true }
  return { label: '참여하기', action: 'apply' }
})

async function act(kind) {
  busy.value = true; toast.value = ''
  try {
    if (kind === 'apply') await client.post(`/companion-posts/${route.params.id}/applications`)
    else await client.delete(`/companion-posts/${route.params.id}/applications`)
    await load()
  } catch (e) {
    toast.value = e.response?.data?.message || '처리에 실패했어요'
    await load()
  } finally { busy.value = false }
}
</script>

<template>
  <div v-if="post">
    <div class="card">
      <span class="chip" :class="post.status === 'OPEN' ? '' : 'warn'">
        {{ { OPEN: '모집중', FULL: '마감', CLOSED: '종료', ENDED: '종료' }[post.status] }}
      </span>
      <div class="big" style="font-size:20px; margin-top:8px">{{ post.gameLabel }}</div>
      <div class="muted">{{ post.startAt.slice(0, 10) }} · {{ post.stadium }}</div>
    </div>

    <div class="card">
      <h2>정원 {{ post.capacity }}명 중 {{ post.confirmedCount }}명 확정</h2>
      <div style="font-size:20px; letter-spacing:4px">
        <span v-for="n in post.capacity" :key="n">{{ n <= post.confirmedCount ? '●' : '○' }}</span>
      </div>
    </div>

    <div class="card">
      <h2>{{ post.authorNickname }}</h2>
      <div v-if="post.intro">“{{ post.intro }}”</div>
    </div>

    <div class="card" v-if="post.confirmedMembers?.length">
      <h2>확정된 동행</h2>
      <div v-for="m in post.confirmedMembers" :key="m.seq" class="list-item" style="padding:8px 0">
        <div style="flex:1">{{ m.nickname }}</div>
        <span class="muted">{{ m.isAuthor ? '작성자' : m.seq + '번째' }}</span>
      </div>
    </div>

    <button class="btn" v-if="button" :disabled="button.disabled || busy" @click="act(button.action)">
      {{ busy ? '처리 중…' : button.label }}
    </button>
    <div class="toast" v-if="toast">{{ toast }}</div>
  </div>
</template>

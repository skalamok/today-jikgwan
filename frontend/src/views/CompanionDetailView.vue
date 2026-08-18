<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import client from '../api/client'

const comments = ref([])
const commentDraft = ref('')

const route = useRoute()
const post = ref(null)
const toast = ref('')
const busy = ref(false)

async function load() {
  post.value = (await client.get(`/companion-posts/${route.params.id}`)).data
  comments.value = (await client.get(`/companion-posts/${route.params.id}/comments`)).data
}
onMounted(load)

// REQ-F-510 공개 문의 댓글. 지원 전에도 질문할 수 있다
async function addComment() {
  const text = commentDraft.value.trim()
  if (!text) return
  const { data } = await client.post(`/companion-posts/${route.params.id}/comments`, { content: text })
  comments.value.push(data)
  commentDraft.value = ''
}

// REQ-F-508 신고
async function report() {
  const reason = window.prompt('신고 사유를 입력해주세요')
  if (!reason) return
  try {
    await client.post('/reports', { targetType: 'POST', targetId: Number(route.params.id), reason })
    toast.value = '신고가 접수됐어요'
  } catch (e) {
    toast.value = e.response?.data?.message || '신고에 실패했어요'
  }
}

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

    <div class="card" v-if="post.myStatus === 'CONFIRMED' || post.myStatus === 'AUTHOR'">
      <h2>동행 대화</h2>
      <div class="muted" style="margin:-6px 0 10px">확정된 동행자끼리만 볼 수 있어요</div>
      <RouterLink :to="`/companions/${$route.params.id}/chat`">
        <button class="btn ghost small">대화방 열기</button>
      </RouterLink>
    </div>

    <div class="card">
      <h2>문의</h2>
      <div class="muted" style="margin:-6px 0 10px">누구나 볼 수 있는 공개 댓글이에요</div>
      <div v-if="!comments.length" class="muted">아직 문의가 없어요</div>
      <div v-for="c in comments" :key="c.id" class="list-item" style="padding:9px 0">
        <div style="flex:1">
          <div>{{ c.content }}</div>
          <div class="muted" style="margin-top:3px">{{ c.nickname }} · {{ c.createdAt.slice(5, 16).replace('T', ' ') }}</div>
        </div>
      </div>
      <div class="row" style="gap:6px; margin-top:10px">
        <input v-model="commentDraft" placeholder="궁금한 점을 물어보세요" maxlength="500"
               @keyup.enter="addComment"
               style="flex:1; padding:10px; border:1px solid var(--line); border-radius:10px; font-size:14px" />
        <button class="btn small" :disabled="!commentDraft.trim()" @click="addComment">등록</button>
      </div>
    </div>

    <button class="btn" v-if="button" :disabled="button.disabled || busy" @click="act(button.action)">
      {{ busy ? '처리 중…' : button.label }}
    </button>
    <button class="btn ghost" style="margin-top:8px" @click="report">이 모집 신고하기</button>
    <div class="toast" v-if="toast">{{ toast }}</div>
  </div>
</template>

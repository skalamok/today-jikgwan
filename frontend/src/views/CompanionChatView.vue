<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import client from '../api/client'

const route = useRoute()
const messages = ref([])
const readOnly = ref(false)
const draft = ref('')
const error = ref('')
const sending = ref(false)
const listEl = ref(null)
let timer = null
let lastAt = null

async function loadAll() {
  try {
    const { data } = await client.get(`/companion-posts/${route.params.id}/messages`)
    messages.value = data.messages
    readOnly.value = data.readOnly
    if (data.messages.length) lastAt = data.messages[data.messages.length - 1].createdAt
    await client.put(`/companion-posts/${route.params.id}/messages/read`)
    scrollDown()
  } catch (e) {
    error.value = e.response?.data?.message || '대화를 불러오지 못했어요'
  }
}

// REQ-F-512 실시간 수신. WebSocket 대신 증분 조회로 대체할 수 있도록 after 를 사용한다
async function poll() {
  if (!lastAt || readOnly.value) return
  try {
    const { data } = await client.get(`/companion-posts/${route.params.id}/messages`,
      { params: { after: lastAt } })
    if (data.messages.length) {
      messages.value.push(...data.messages)
      lastAt = data.messages[data.messages.length - 1].createdAt
      await client.put(`/companion-posts/${route.params.id}/messages/read`)
      scrollDown()
    }
  } catch { /* 폴링 실패는 조용히 넘긴다 */ }
}

function scrollDown() {
  nextTick(() => { if (listEl.value) listEl.value.scrollTop = listEl.value.scrollHeight })
}

async function send() {
  const text = draft.value.trim()
  if (!text || sending.value) return
  sending.value = true
  try {
    const { data } = await client.post(`/companion-posts/${route.params.id}/messages`, { content: text })
    messages.value.push(data)
    lastAt = data.createdAt
    draft.value = ''
    scrollDown()
  } catch (e) {
    error.value = e.response?.data?.message || '전송에 실패했어요'
  } finally { sending.value = false }
}

function time(iso) { return iso.slice(11, 16) }

/*
 * REQ-F-512 실시간 수신.
 *
 * 연결한 뒤 첫 프레임으로 인증한다. 브라우저 WebSocket 은 헤더를 못 붙이는데
 * 토큰을 쿼리로 넘기면 접속 로그에 그대로 남기 때문이다.
 *
 * 붙지 않거나 끊기면 폴링으로 되돌아간다. 실시간이 안 된다고 대화를 못 읽어서는 안 된다.
 */
let socket = null

function startPolling() {
  if (timer) return
  timer = setInterval(poll, 5000)
}

function connect() {
  const token = localStorage.getItem('accessToken')
  if (!token || !window.WebSocket) { startPolling(); return }

  const url = `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws/companion-chat`
  try { socket = new WebSocket(url) } catch { startPolling(); return }

  socket.onopen = () => socket.send(JSON.stringify({ token, postId: Number(route.params.id) }))

  socket.onmessage = (e) => {
    const msg = JSON.parse(e.data)
    if (msg.type === 'ready') { clearInterval(timer); timer = null; return }
    if (msg.type !== 'message') return
    // 내가 보낸 것은 전송 응답으로 이미 붙었다
    if (messages.value.some((m) => m.id === msg.data.id)) return
    messages.value.push({ ...msg.data, mine: false })
    lastAt = msg.data.createdAt
    client.put(`/companion-posts/${route.params.id}/messages/read`).catch(() => {})
    scrollDown()
  }

  socket.onclose = () => { socket = null; startPolling() }
  socket.onerror = () => { startPolling() }
}

onMounted(async () => {
  await loadAll()
  startPolling()       // 먼저 폴링으로 띄우고, 연결이 붙으면 멈춘다
  if (!readOnly.value) connect()
})

onUnmounted(() => {
  clearInterval(timer)
  if (socket) { socket.onclose = null; socket.close() }
})
</script>

<template>
  <div class="card" v-if="error" style="border-color:#f0c0c0">{{ error }}</div>

  <div class="card wide" style="display:flex; flex-direction:column; height:60vh; min-height:360px">
    <h2 style="flex:none">메이트 대화</h2>
    <div ref="listEl" style="flex:1; overflow-y:auto; padding-right:4px">
      <div v-if="!messages.length" class="muted" style="text-align:center; padding:24px">
        아직 대화가 없어요. 만날 시간과 장소를 정해보세요
      </div>
      <div v-for="m in messages" :key="m.id"
           :style="{ display:'flex', justifyContent: m.isMine ? 'flex-end' : 'flex-start', marginBottom:'10px' }">
        <div style="max-width:74%">
          <div v-if="!m.isMine" class="muted" style="font-size:11px; margin-bottom:2px">{{ m.nickname }}</div>
          <div :style="{
                 padding:'9px 12px', borderRadius:'12px', fontSize:'14px', lineHeight:'1.45',
                 background: m.isMine ? 'var(--brand)' : '#eef1f5',
                 color: m.isMine ? '#fff' : 'var(--ink)' }">{{ m.content }}</div>
          <div class="muted" :style="{ fontSize:'10px', marginTop:'3px', textAlign: m.isMine ? 'right' : 'left' }">
            {{ time(m.createdAt) }}
          </div>
        </div>
      </div>
    </div>

    <div style="flex:none; margin-top:10px">
      <div v-if="readOnly" class="muted" style="text-align:center; padding:10px">
        경기가 끝나 종료된 대화방이에요
      </div>
      <div v-else class="row" style="gap:6px">
        <input v-model="draft" placeholder="메시지를 입력하세요" maxlength="1000"
               @keyup.enter="send"
               style="flex:1; padding:11px; border:1px solid var(--line); border-radius:10px; font-size:14px" />
        <button class="btn small" :disabled="sending || !draft.trim()" @click="send">전송</button>
      </div>
    </div>
  </div>

  <RouterLink :to="`/companions/${$route.params.id}`">
    <button class="btn ghost">모집 상세로</button>
  </RouterLink>
</template>

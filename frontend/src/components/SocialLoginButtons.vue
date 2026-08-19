<script setup>
import { ref, onMounted } from 'vue'
import client from '../api/client'

// 서버가 키를 가진 제공자만 내려준다. 키를 넣지 않으면 이 영역 자체가 그려지지 않는다.
const providers = ref([])
const pending = ref('')
const error = ref('')

const BRAND = {
  google: { label: '구글로 계속하기', bg: '#ffffff', fg: '#1f1f1f', border: '#dadce0' },
  naver: { label: '네이버로 계속하기', bg: '#03c75a', fg: '#ffffff', border: '#03c75a' },
  kakao: { label: '카카오로 계속하기', bg: '#fee500', fg: '#191600', border: '#fee500' },
}

onMounted(async () => {
  try {
    const { data } = await client.get('/auth/oauth/providers')
    providers.value = data.filter((p) => BRAND[p.provider])
  } catch {
    providers.value = []
  }
})

async function start(provider) {
  error.value = ''
  pending.value = provider
  try {
    const { data } = await client.get(`/auth/oauth/${provider}/authorize-url`)
    // state 는 콜백에서 대조해 다른 창에서 시작된 응답을 거른다
    sessionStorage.setItem(`oauthState:${provider}`, data.state)
    window.location.href = data.authorizeUrl
  } catch (e) {
    pending.value = ''
    error.value = e.response?.data?.message || '로그인 창을 열지 못했어요.'
  }
}
</script>

<template>
  <div v-if="providers.length" class="social">
    <div class="social-sep"><span>또는</span></div>
    <button v-for="p in providers" :key="p.provider" type="button" class="social-btn"
            :disabled="!!pending"
            :style="{ background: BRAND[p.provider].bg, color: BRAND[p.provider].fg,
                      borderColor: BRAND[p.provider].border }"
            @click="start(p.provider)">
      <span class="social-icon" :class="p.provider" aria-hidden="true"></span>
      {{ pending === p.provider ? '이동 중…' : BRAND[p.provider].label }}
    </button>
    <div class="err" v-if="error">{{ error }}</div>
  </div>
</template>

<style scoped>
.social { margin-top: 18px; display: flex; flex-direction: column; gap: 8px; }
.social-sep {
  display: flex; align-items: center; gap: 12px;
  color: var(--muted); font-size: 12px; margin-bottom: 4px;
}
.social-sep::before, .social-sep::after {
  content: ''; flex: 1; height: 1px; background: var(--line);
}
.social-btn {
  display: flex; align-items: center; justify-content: center; gap: 8px;
  width: 100%; height: 46px; border: 1px solid; border-radius: 10px;
  font-size: 14px; font-weight: 600; cursor: pointer;
  transition: filter .15s ease;
}
.social-btn:hover:not(:disabled) { filter: brightness(0.96); }
.social-btn:disabled { opacity: .6; cursor: default; }
.social-icon { width: 18px; height: 18px; background-repeat: no-repeat; background-size: contain; }
.social-icon.google {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 48 48'%3E%3Cpath fill='%23EA4335' d='M24 9.5c3.5 0 6.6 1.2 9 3.6l6.7-6.7C35.6 2.6 30.1 0 24 0 14.6 0 6.5 5.4 2.6 13.2l7.8 6.1C12.3 13.2 17.7 9.5 24 9.5z'/%3E%3Cpath fill='%234285F4' d='M46.1 24.6c0-1.6-.1-3.1-.4-4.6H24v9.1h12.4c-.5 2.9-2.2 5.3-4.7 6.9l7.6 5.9c4.4-4.1 6.8-10.1 6.8-17.3z'/%3E%3Cpath fill='%23FBBC05' d='M10.4 28.7c-.5-1.4-.8-2.9-.8-4.7s.3-3.3.8-4.7l-7.8-6.1C.9 16.4 0 20.1 0 24s.9 7.6 2.6 10.8l7.8-6.1z'/%3E%3Cpath fill='%2334A853' d='M24 48c6.5 0 11.9-2.1 15.9-5.8l-7.6-5.9c-2.1 1.4-4.8 2.3-8.3 2.3-6.3 0-11.7-3.7-13.6-9.1l-7.8 6.1C6.5 42.6 14.6 48 24 48z'/%3E%3C/svg%3E");
}
.social-icon.naver {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 20 20'%3E%3Cpath fill='%23fff' d='M11.6 10.7 8.2 5.5H5.3v9h3.1V9.3l3.4 5.2h2.9v-9h-3.1z'/%3E%3C/svg%3E");
}
.social-icon.kakao {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath fill='%23191600' d='M12 3C6.9 3 2.8 6.3 2.8 10.3c0 2.6 1.7 4.9 4.3 6.2l-1 3.7c-.1.3.2.6.5.4l4.4-2.9c.3 0 .7.1 1 .1 5.1 0 9.2-3.3 9.2-7.5S17.1 3 12 3z'/%3E%3C/svg%3E");
}
</style>

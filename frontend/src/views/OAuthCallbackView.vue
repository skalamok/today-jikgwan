<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import client from '../api/client'
import { setToken } from '../store/auth'

const route = useRoute()
const router = useRouter()
const error = ref('')

const LABEL = { google: '구글', naver: '네이버', kakao: '카카오' }

onMounted(async () => {
  const provider = String(route.params.provider || '')
  const { code, state, error: denied } = route.query

  if (denied) {
    // 동의 화면에서 사용자가 취소한 경우
    error.value = `${LABEL[provider] || '소셜'} 로그인을 취소했어요.`
    return
  }
  if (!code) {
    error.value = '인증 정보가 없어요. 다시 시도해 주세요.'
    return
  }

  const saved = sessionStorage.getItem(`oauthState:${provider}`)
  sessionStorage.removeItem(`oauthState:${provider}`)
  if (saved && state && saved !== state) {
    // 이 브라우저에서 시작한 요청이 아니다
    error.value = '인증 요청이 확인되지 않았어요. 처음부터 다시 시도해 주세요.'
    return
  }

  try {
    const { data } = await client.post(`/auth/oauth/${provider}/callback`, { code, state })
    setToken(data.accessToken)
    // 응원팀이 없으면 마이페이지로 보내 먼저 고르게 한다 (전적 판정 기준이 된다)
    const back = sessionStorage.getItem('redirectAfterLogin') || '/'
    sessionStorage.removeItem('redirectAfterLogin')
    router.replace(data.needsFavoriteTeam ? '/my?welcome=1' : back)
  } catch (e) {
    error.value = e.response?.data?.message || '소셜 로그인에 실패했어요.'
  }
})
</script>

<template>
  <div class="card" style="text-align:center; padding:48px 20px">
    <template v-if="error">
      <div style="font-size:34px">⚠️</div>
      <div class="big" style="margin-top:10px">{{ error }}</div>
      <button class="btn" style="margin-top:18px" @click="router.replace('/login')">로그인으로 돌아가기</button>
    </template>
    <template v-else>
      <div class="spinner"></div>
      <div class="muted" style="margin-top:14px">로그인 중이에요…</div>
    </template>
  </div>
</template>

<style scoped>
.spinner {
  width: 32px; height: 32px; margin: 0 auto;
  border: 3px solid var(--line); border-top-color: var(--accent);
  border-radius: 50%; animation: spin .8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../store/auth'

const router = useRouter()
const email = ref('')
const password = ref('')
const showPassword = ref(false)
const error = ref('')
const loading = ref(false)

const emailValid = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value))
// 화면설계서 ④: 두 필드가 유효한 형식으로 모두 입력되어야 활성화된다
const canSubmit = computed(() => emailValid.value && password.value.length >= 8 && !loading.value)

async function submit() {
  error.value = ''
  loading.value = true
  try {
    await login(email.value, password.value)
    const back = sessionStorage.getItem('redirectAfterLogin') || '/'
    sessionStorage.removeItem('redirectAfterLogin')
    router.replace(back)
  } catch (e) {
    // 이메일/비밀번호 중 무엇이 틀렸는지 구분하지 않는다
    error.value = e.response?.data?.message || '로그인에 실패했어요.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div>
    <div class="card accent" style="text-align:center; padding: 38px 20px;">
      <div style="font-size:44px">⚾</div>
      <div class="big" style="font-size:22px; margin-top:10px">오늘의직관</div>
      <div class="muted" style="margin-top:6px">내가 간 경기만의 전적이 쌓입니다</div>
    </div>

    <div class="card">
      <div class="field">
        <label>이메일</label>
        <input v-model.trim="email" type="email" placeholder="example@email.com" autocomplete="username" />
        <div class="err" v-if="email && !emailValid">이메일 형식이 올바르지 않습니다</div>
      </div>

      <div class="field">
        <label>비밀번호</label>
        <div style="position:relative">
          <input v-model="password" :type="showPassword ? 'text' : 'password'"
                 placeholder="비밀번호를 입력하세요" autocomplete="current-password"
                 @keyup.enter="canSubmit && submit()" />
          <button type="button" @click="showPassword = !showPassword"
                  style="position:absolute; right:10px; top:9px; border:0; background:none; font-size:18px; cursor:pointer">
            {{ showPassword ? '🙈' : '👁️' }}
          </button>
        </div>
        <div class="err" v-if="error">{{ error }}</div>
      </div>

      <button class="btn" :disabled="!canSubmit" @click="submit">
        {{ loading ? '로그인 중…' : '로그인' }}
      </button>

      <div class="row" style="margin-top:14px; justify-content:center; gap:16px">
        <RouterLink to="/signup" class="muted">회원가입</RouterLink>
        <span class="muted">·</span>
        <RouterLink to="/games" class="muted">둘러보기</RouterLink>
      </div>
    </div>
  </div>
</template>

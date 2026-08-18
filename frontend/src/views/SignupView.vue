<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import client from '../api/client'
import { login } from '../store/auth'

const router = useRouter()
const email = ref(''); const password = ref(''); const nickname = ref('')
const favoriteTeamId = ref(''); const teams = ref([])
const error = ref(''); const loading = ref(false)

const emailValid = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value))
const canSubmit = computed(() =>
  emailValid.value && password.value.length >= 8 && nickname.value.length >= 2 && !loading.value)

onMounted(async () => {
  // 응원팀 선택지는 구장 정보에서 팀명을 끌어온다
  const { data } = await client.get('/stadiums')
  teams.value = data.flatMap((s) => s.homeTeams.split(' · ').map((t) => ({ name: t, stadium: s.name })))
})

async function submit() {
  error.value = ''; loading.value = true
  try {
    await client.post('/auth/signup', {
      email: email.value, password: password.value, nickname: nickname.value,
    })
    await login(email.value, password.value)
    router.replace('/')
  } catch (e) {
    error.value = e.response?.data?.message || '가입에 실패했어요.'
  } finally { loading.value = false }
}
</script>

<template>
  <div class="card">
    <div class="field">
      <label>이메일 <span class="req">*</span></label>
      <input v-model.trim="email" type="email" placeholder="example@email.com" />
      <div class="err" v-if="email && !emailValid">이메일 형식이 올바르지 않습니다</div>
    </div>
    <div class="field">
      <label>비밀번호 <span class="req">*</span></label>
      <input v-model="password" type="password" placeholder="8자 이상" />
      <div class="err" v-if="password && password.length < 8">8자 이상 입력해 주세요</div>
    </div>
    <div class="field">
      <label>닉네임 <span class="req">*</span></label>
      <input v-model.trim="nickname" maxlength="30" placeholder="잠실단골" />
    </div>
    <div class="err" v-if="error" style="margin-bottom:10px">{{ error }}</div>
    <button class="btn" :disabled="!canSubmit" @click="submit">
      {{ loading ? '가입 중…' : '가입하기' }}
    </button>
    <div style="text-align:center; margin-top:14px">
      <RouterLink to="/login" class="muted">이미 계정이 있어요</RouterLink>
    </div>
  </div>
</template>

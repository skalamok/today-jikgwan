<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import client from '../api/client'
import { login } from '../store/auth'
import TeamPicker from '../components/TeamPicker.vue'
import SocialLoginButtons from '../components/SocialLoginButtons.vue'

const router = useRouter()
const email = ref(''); const password = ref(''); const nickname = ref('')
const favoriteTeamId = ref(null)
const error = ref(''); const loading = ref(false)

const emailValid = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value))
const canSubmit = computed(() =>
  emailValid.value && password.value.length >= 8 && nickname.value.length >= 2 && !loading.value)

async function submit() {
  error.value = ''; loading.value = true
  try {
    await client.post('/auth/signup', {
      email: email.value,
      password: password.value,
      nickname: nickname.value,
      favoriteTeamId: favoriteTeamId.value,
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
      <input v-model.trim="email" type="email" placeholder="example@email.com" autocomplete="username" />
      <div class="err" v-if="email && !emailValid">이메일 형식이 올바르지 않습니다</div>
    </div>
    <div class="field">
      <label>비밀번호 <span class="req">*</span></label>
      <input v-model="password" type="password" placeholder="8자 이상" autocomplete="new-password" />
      <div class="err" v-if="password && password.length < 8">8자 이상 입력해 주세요</div>
    </div>
    <div class="field">
      <label>닉네임 <span class="req">*</span></label>
      <input v-model.trim="nickname" maxlength="30" placeholder="잠실단골" />
    </div>

    <div class="field">
      <label>응원팀</label>
      <div class="muted" style="margin-bottom:8px; font-size:12px">
        기록을 남길 때 승패 판정 기준이 됩니다. 나중에 바꿔도 이미 쓴 기록은 그대로예요.
      </div>
      <TeamPicker v-model="favoriteTeamId" />
    </div>

    <div class="err" v-if="error" style="margin-bottom:10px">{{ error }}</div>
    <button class="btn" :disabled="!canSubmit" @click="submit">
      {{ loading ? '가입 중…' : '가입하기' }}
    </button>

    <SocialLoginButtons />

    <div style="text-align:center; margin-top:14px">
      <RouterLink to="/login" class="muted">이미 계정이 있어요</RouterLink>
    </div>
  </div>
</template>

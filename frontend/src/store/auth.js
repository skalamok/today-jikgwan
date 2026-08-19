import { reactive, computed } from 'vue'
import client from '../api/client'

const state = reactive({
  token: localStorage.getItem('accessToken') || null,
  me: null,
})

export const isLoggedIn = computed(() => !!state.token)
export const isAdmin = computed(() => state.me?.role === 'ADMIN')
export const auth = state

export async function login(email, password) {
  const { data } = await client.post('/auth/login', { email, password })
  state.token = data.accessToken
  localStorage.setItem('accessToken', data.accessToken)
}

/** 소셜 로그인 콜백처럼 서버가 이미 발급한 토큰을 받아 저장할 때 쓴다. */
export function setToken(token) {
  state.token = token
  localStorage.setItem('accessToken', token)
}

export async function signup(payload) {
  await client.post('/auth/signup', payload)
}

/** 역할에 따라 메뉴가 달라지므로 로그인 상태면 한 번 받아 둔다. */
export async function loadMe() {
  if (!state.token) { state.me = null; return }
  try { state.me = (await client.get('/users/me')).data } catch { state.me = null }
}

export function logout() {
  state.token = null
  state.me = null
  localStorage.removeItem('accessToken')
}

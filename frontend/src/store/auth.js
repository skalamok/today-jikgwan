import { reactive, computed } from 'vue'
import client from '../api/client'

const state = reactive({
  token: localStorage.getItem('accessToken') || null,
  me: null,
})

export const isLoggedIn = computed(() => !!state.token)
export const auth = state

export async function login(email, password) {
  const { data } = await client.post('/auth/login', { email, password })
  state.token = data.accessToken
  localStorage.setItem('accessToken', data.accessToken)
}

export async function signup(payload) {
  await client.post('/auth/signup', payload)
}

export function logout() {
  state.token = null
  state.me = null
  localStorage.removeItem('accessToken')
}

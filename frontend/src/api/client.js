import axios from 'axios'

const client = axios.create({ baseURL: '/api/v1' })

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 인증 만료 시 로그인 화면으로 보내고 복귀 경로를 보관한다 (화면설계서 0장 공통 규칙)
client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('accessToken')
      const here = window.location.pathname
      if (here !== '/login') {
        sessionStorage.setItem('redirectAfterLogin', here)
        window.location.href = '/login'
      }
    }
    return Promise.reject(err)
  },
)

export default client

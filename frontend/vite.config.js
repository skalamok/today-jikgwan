import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      // REQ-F-512 대화 실시간 수신. WebSocket 은 ws: true 를 줘야 넘어간다
      '/ws': { target: 'http://localhost:8080', ws: true },
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/uploads': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})

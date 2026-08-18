<script setup>
import { RouterView, useRoute } from 'vue-router'
import { computed, ref, watch } from 'vue'
import { isLoggedIn, auth } from './store/auth'
import client from './api/client'

const route = useRoute()

// 헤더의 미읽음 배지. 화면을 옮길 때마다 다시 센다
const unread = ref(0)
async function loadUnread() {
  if (!auth.token) { unread.value = 0; return }
  try { unread.value = (await client.get('/notifications')).data.unreadCount } catch { unread.value = 0 }
}
loadUnread()
watch(() => route.fullPath, loadUnread)
const screenId = computed(() => route.meta?.id || '')
const titles = {
  home: '오늘의직관', login: '로그인', signup: '회원가입', games: '경기',
  'log-new': '직관 기록', logs: '내 기록', stats: '내 전적',
  stadiums: '구장', stadium: '구장 상세', companions: '동행', companion: '동행 모집', 'companion-chat': '동행 대화',
  plan: '관람 계획', standings: '팀 순위', guide: '첫 직관 가이드', my: '마이페이지',
  'log-detail': '기록 상세', notifications: '알림',
}
const title = computed(() => titles[route.name] || '오늘의직관')
</script>

<template>
  <div class="app">
    <header class="topbar">
      <h1>{{ title }}</h1>
      <div class="row" style="gap:10px">
        <RouterLink to="/notifications" class="hicon" title="알림">
          🔔<span v-if="unread" class="badge">{{ unread }}</span>
        </RouterLink>
        <RouterLink to="/my" class="hicon" title="마이페이지">👤</RouterLink>
        <!-- 화면 ID를 노출해 설계서와 화면을 1:1로 대조할 수 있게 한다 -->
        <span class="scr">{{ screenId }}</span>
      </div>
    </header>

    <main class="content">
      <RouterView v-slot="{ Component }">
        <Transition name="fade" mode="out-in">
          <component :is="Component" :key="$route.fullPath" />
        </Transition>
      </RouterView>
    </main>

    <nav class="tabbar" v-if="$route.name !== 'login' && $route.name !== 'signup'">
      <RouterLink to="/" :class="{ active: $route.name === 'home' }"><span class="ico">🏠</span>홈</RouterLink>
      <RouterLink to="/games" :class="{ active: $route.name === 'games' }"><span class="ico">📅</span>경기</RouterLink>
      <RouterLink to="/logs" :class="{ active: ['logs','log-new'].includes($route.name) }"><span class="ico">📝</span>기록</RouterLink>
      <RouterLink to="/stats" :class="{ active: $route.name === 'stats' }"><span class="ico">📊</span>전적</RouterLink>
      <RouterLink to="/plan" :class="{ active: $route.name === 'plan' }"><span class="ico">🗓️</span>계획</RouterLink>
      <RouterLink to="/stadiums" :class="{ active: ['stadiums','stadium'].includes($route.name) }"><span class="ico">🏟️</span>구장</RouterLink>
    </nav>
  </div>
</template>

<style scoped>
.hicon { position: relative; font-size: 17px; line-height: 1; }
.badge {
  position: absolute; top: -5px; right: -7px; min-width: 15px; height: 15px;
  padding: 0 4px; border-radius: 999px; background: var(--lose); color: #fff;
  font-size: 9.5px; font-weight: 800; display: flex; align-items: center; justify-content: center;
}
</style>

import { createRouter, createWebHistory } from 'vue-router'
import { auth } from '../store/auth'

const routes = [
  { path: '/', name: 'home', component: () => import('../views/HomeView.vue'), meta: { id: 'SCR-MAIN-001' } },
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { id: 'SCR-AUTH-001', public: true } },
  { path: '/signup', name: 'signup', component: () => import('../views/SignupView.vue'), meta: { id: 'SCR-AUTH-002', public: true } },
  { path: '/games', name: 'games', component: () => import('../views/GamesView.vue'), meta: { id: 'SCR-GAME-001', public: true } },
  { path: '/logs/new', name: 'log-new', component: () => import('../views/LogCreateView.vue'), meta: { id: 'SCR-LOG-001' } },
  { path: '/logs', name: 'logs', component: () => import('../views/LogListView.vue'), meta: { id: 'SCR-LOG-002' } },
  { path: '/stats', name: 'stats', component: () => import('../views/StatsView.vue'), meta: { id: 'SCR-STAT-001' } },
  { path: '/stadiums', name: 'stadiums', component: () => import('../views/StadiumListView.vue'), meta: { id: 'SCR-PARK-001', public: true } },
  { path: '/stadiums/:id', name: 'stadium', component: () => import('../views/StadiumDetailView.vue'), meta: { id: 'SCR-PARK-002', public: true } },
  { path: '/plan', name: 'plan', component: () => import('../views/PlanView.vue'), meta: { id: 'SCR-PLAN-001' } },
  { path: '/companions', name: 'companions', component: () => import('../views/CompanionListView.vue'), meta: { id: 'SCR-MATE-001' } },
  { path: '/companions/:id', name: 'companion', component: () => import('../views/CompanionDetailView.vue'), meta: { id: 'SCR-MATE-002' } },
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  if (!to.meta.public && !auth.token) {
    sessionStorage.setItem('redirectAfterLogin', to.fullPath)
    return { name: 'login' }
  }
  return true
})

export default router

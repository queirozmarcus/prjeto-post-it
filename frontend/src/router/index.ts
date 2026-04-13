import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const routes = [
  {
    path: '/login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true },
  },
  {
    path: '/register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    component: () => import('@/views/HomeView.vue'),
    meta: { requiresAuth: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  const auth = useAuth()

  if (to.meta.requiresAuth && !auth.isLoggedIn.value) {
    // Tenta verificar sessão ativa antes de redirecionar (cookie pode já existir)
    await auth.checkAuth()
    if (!auth.isLoggedIn.value) return '/login'
  }

  // Rota pública com sessão ativa: redireciona para home
  if (to.meta.public && auth.isLoggedIn.value) return '/'
})

export default router

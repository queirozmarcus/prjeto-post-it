<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { StickyNote, Sparkles, AlertCircle, LogOut } from 'lucide-vue-next'
import PostitForm from '@/components/PostitForm.vue'
import PostitGrid from '@/components/PostitGrid.vue'
import { usePostits } from '@/composables/usePostits'
import { useAuth } from '@/composables/useAuth'
import type { PostitRequest } from '@/services/postitApi'

type CardSize = 'small' | 'medium' | 'large'

const router = useRouter()
const auth = useAuth()
const { postits, isLoading, error, isCreating, fetchPostits, createPostit, deletePostit } =
  usePostits()

const cardSize = ref<CardSize>('large')

const sizeOptions: { value: CardSize; label: string; title: string }[] = [
  { value: 'small', label: 'P', title: 'Notas pequenas' },
  { value: 'medium', label: 'M', title: 'Notas médias' },
  { value: 'large', label: 'G', title: 'Notas grandes' },
]

onMounted(async () => {
  await fetchPostits()
})

const handleCreatePostit = async (request: PostitRequest) => {
  await createPostit(request)
}

const handleDeletePostit = async (id: number) => {
  const confirmed = confirm('Tem certeza que deseja excluir esta nota?')
  if (confirmed) {
    await deletePostit(id)
  }
}

const handleLogout = async () => {
  await auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="app-wrapper">
    <!-- Background decorativo -->
    <div class="bg-blobs">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
    </div>

    <div class="container">
      <!-- Header -->
      <header class="app-header">
        <div class="logo">
          <div class="icon-box">
            <StickyNote :size="28" color="#fff" />
          </div>
          <h1>Post<span>it</span>.</h1>
        </div>

        <div class="header-right">
          <div class="status-badge">
            <Sparkles :size="14" />
            <span>{{ auth.user.value?.name ?? 'Carregando...' }}</span>
          </div>
          <button class="btn-logout" @click="handleLogout" title="Sair da conta">
            <LogOut :size="16" />
            <span>Sair</span>
          </button>
        </div>
      </header>

      <!-- Main content -->
      <main class="app-main">
        <!-- Error alert (global) -->
        <div v-if="error" class="error-alert">
          <AlertCircle :size="20" />
          <div class="error-content">
            <strong>Erro:</strong>
            <p>{{ error }}</p>
          </div>
        </div>

        <!-- Formulário para criar nota -->
        <PostitForm @submit="handleCreatePostit" @loading="(val: boolean) => (isCreating = val)" />

        <!-- Controle de tamanho das notas -->
        <div class="size-controls">
          <span class="size-label">Tamanho:</span>
          <div class="size-buttons">
            <button
              v-for="opt in sizeOptions"
              :key="opt.value"
              class="btn-size"
              :class="{ active: cardSize === opt.value }"
              :title="opt.title"
              @click="cardSize = opt.value"
            >
              {{ opt.label }}
            </button>
          </div>
        </div>

        <!-- Grid de notas -->
        <PostitGrid
          :postits="postits"
          :isLoading="isLoading"
          :cardSize="cardSize"
          @delete="handleDeletePostit"
        />
      </main>

      <!-- Footer -->
      <footer class="app-footer">
        <p>Built with ❤️ using Vue 3 + Spring Boot</p>
      </footer>
    </div>
  </div>
</template>

<style scoped>
:root {
  --primary: #6366f1;
  --primary-hover: #4f46e5;
  --bg-main: #0f172a;
  --card-bg: rgba(255, 255, 255, 0.05);
  --text-main: #f8fafc;
  --text-dim: #94a3b8;
}

.app-wrapper {
  min-height: 100vh;
  position: relative;
  padding: 2rem 1rem;
  background-color: var(--bg-main, #0f172a);
  color: var(--text-main, #f8fafc);
}

/* Background decorativo */
.bg-blobs {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: -1;
  overflow: hidden;
  pointer-events: none;
}

.blob {
  position: absolute;
  width: 500px;
  height: 500px;
  background: linear-gradient(
    135deg,
    rgba(99, 102, 241, 0.2) 0%,
    rgba(168, 85, 247, 0.2) 100%
  );
  filter: blur(80px);
  border-radius: 50%;
}

.blob-1 {
  top: -100px;
  right: -100px;
  animation: orbit 20s infinite linear;
}

.blob-2 {
  bottom: -100px;
  left: -100px;
  animation: orbit 25s infinite linear reverse;
}

@keyframes orbit {
  from {
    transform: rotate(0deg) translateX(50px) rotate(0deg);
  }
  to {
    transform: rotate(360deg) translateX(50px) rotate(-360deg);
  }
}

.container {
  max-width: 1100px;
  margin: 0 auto;
}

/* Header */
.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 3rem;
  animation: slideDown 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.icon-box {
  background: var(--primary, #6366f1);
  padding: 0.5rem;
  border-radius: 12px;
  box-shadow: 0 10px 15px -3px rgba(99, 102, 241, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo h1 {
  margin: 0;
  font-size: 1.75rem;
  font-weight: 800;
  letter-spacing: -1px;
}

.logo h1 span {
  color: var(--primary, #6366f1);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.status-badge {
  background: rgba(255, 255, 255, 0.1);
  padding: 0.4rem 0.8rem;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.4rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: var(--text-dim, #94a3b8);
}

.btn-logout {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: #fca5a5;
  padding: 0.4rem 0.85rem;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-logout:hover {
  background: rgba(239, 68, 68, 0.2);
  border-color: rgba(239, 68, 68, 0.5);
  color: #f87171;
}

/* Main content */
.app-main {
  animation: fadeIn 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

/* Controle de tamanho das notas */
.size-controls {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
}

.size-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--text-dim, #94a3b8);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.size-buttons {
  display: flex;
  gap: 0.25rem;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 0.25rem;
}

.btn-size {
  background: transparent;
  border: none;
  color: var(--text-dim, #94a3b8);
  width: 2rem;
  height: 2rem;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-size:hover {
  background: rgba(255, 255, 255, 0.1);
  color: var(--text-main, #f8fafc);
}

.btn-size.active {
  background: var(--primary, #6366f1);
  color: #ffffff;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.4);
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* Error alert */
.error-alert {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 12px;
  padding: 1.25rem;
  margin-bottom: 2rem;
  color: #fca5a5;
  animation: slideDown 0.3s ease;
}

.error-content {
  flex: 1;
}

.error-content strong {
  display: block;
  margin-bottom: 0.25rem;
  font-size: 0.95rem;
}

.error-content p {
  margin: 0;
  font-size: 0.9rem;
}

/* Footer */
.app-footer {
  text-align: center;
  margin-top: 4rem;
  padding-top: 2rem;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  color: var(--text-dim, #94a3b8);
  font-size: 0.9rem;
}

.app-footer p {
  margin: 0;
}

/* Responsividade */
@media (max-width: 768px) {
  .app-wrapper {
    padding: 1rem;
  }

  .app-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
    margin-bottom: 2rem;
  }

  .logo h1 {
    font-size: 1.5rem;
  }

  .container {
    padding: 0;
  }
}

@media (max-width: 640px) {
  .app-wrapper {
    padding: 0.75rem;
  }

  .logo {
    gap: 0.5rem;
  }

  .icon-box {
    padding: 0.375rem;
  }

  .logo h1 {
    font-size: 1.25rem;
  }

  .status-badge {
    font-size: 0.65rem;
  }

  .error-alert {
    flex-direction: column;
    gap: 0.75rem;
  }
}
</style>

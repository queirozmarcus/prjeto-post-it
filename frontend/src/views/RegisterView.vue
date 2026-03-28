<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { RouterLink } from 'vue-router'
import { StickyNote, UserPlus, AlertCircle } from 'lucide-vue-next'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const auth = useAuth()

const name = ref('')
const email = ref('')
const password = ref('')
const isLoading = ref(false)
const errorMessage = ref('')

const MIN_PASSWORD_LENGTH = 8

const passwordTooShort = computed(
  () => password.value.length > 0 && password.value.length < MIN_PASSWORD_LENGTH,
)

const handleRegister = async () => {
  errorMessage.value = ''

  if (!name.value.trim()) {
    errorMessage.value = 'Informe seu nome.'
    return
  }

  if (!email.value.trim()) {
    errorMessage.value = 'Informe seu email.'
    return
  }

  if (password.value.length < MIN_PASSWORD_LENGTH) {
    errorMessage.value = `A senha deve ter no mínimo ${MIN_PASSWORD_LENGTH} caracteres.`
    return
  }

  try {
    isLoading.value = true
    await auth.register(email.value.trim(), password.value, name.value.trim())
    router.push('/')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Erro ao criar conta.'
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div class="auth-wrapper">
    <!-- Background decorativo -->
    <div class="bg-blobs">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
    </div>

    <div class="auth-card">
      <!-- Logo -->
      <div class="auth-logo">
        <div class="icon-box">
          <StickyNote :size="28" color="#fff" />
        </div>
        <h1>Post<span>it</span>.</h1>
      </div>

      <h2 class="auth-title">Criar nova conta</h2>

      <!-- Mensagem de erro inline -->
      <div v-if="errorMessage" class="error-alert" role="alert">
        <AlertCircle :size="18" />
        <span>{{ errorMessage }}</span>
      </div>

      <!-- Formulário -->
      <form class="auth-form" @submit.prevent="handleRegister">
        <div class="form-group">
          <label for="name" class="form-label">Nome</label>
          <input
            id="name"
            v-model="name"
            type="text"
            class="form-input"
            placeholder="Seu nome"
            autocomplete="name"
            :disabled="isLoading"
            required
          />
        </div>

        <div class="form-group">
          <label for="email" class="form-label">Email</label>
          <input
            id="email"
            v-model="email"
            type="email"
            class="form-input"
            placeholder="seu@email.com"
            autocomplete="email"
            :disabled="isLoading"
            required
          />
        </div>

        <div class="form-group">
          <label for="password" class="form-label">Senha</label>
          <input
            id="password"
            v-model="password"
            type="password"
            class="form-input"
            :class="{ 'input-error': passwordTooShort }"
            placeholder="Mínimo 8 caracteres"
            autocomplete="new-password"
            :disabled="isLoading"
            required
          />
          <span v-if="passwordTooShort" class="field-hint field-hint--error">
            Senha muito curta. Mínimo {{ MIN_PASSWORD_LENGTH }} caracteres.
          </span>
          <span v-else class="field-hint">Mínimo {{ MIN_PASSWORD_LENGTH }} caracteres</span>
        </div>

        <button type="submit" class="btn-submit" :disabled="isLoading || passwordTooShort">
          <UserPlus v-if="!isLoading" :size="18" />
          <span>{{ isLoading ? 'Criando conta...' : 'Criar conta' }}</span>
        </button>
      </form>

      <p class="auth-link">
        Já tem conta?
        <RouterLink to="/login">Entrar</RouterLink>
      </p>
    </div>
  </div>
</template>

<style scoped>
.auth-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem 1rem;
  background-color: #0f172a;
  color: #f8fafc;
  position: relative;
}

.bg-blobs {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
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

.auth-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  padding: 2.5rem;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
  animation: fadeIn 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.auth-logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
}

.icon-box {
  background: #6366f1;
  padding: 0.5rem;
  border-radius: 12px;
  box-shadow: 0 10px 15px -3px rgba(99, 102, 241, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
}

.auth-logo h1 {
  margin: 0;
  font-size: 1.75rem;
  font-weight: 800;
  letter-spacing: -1px;
}

.auth-logo h1 span {
  color: #6366f1;
}

.auth-title {
  margin: 0 0 1.5rem;
  font-size: 1.1rem;
  font-weight: 600;
  color: #94a3b8;
}

.error-alert {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 10px;
  padding: 0.85rem 1rem;
  color: #fca5a5;
  font-size: 0.9rem;
  margin-bottom: 1.25rem;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.form-label {
  font-size: 0.8rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: #94a3b8;
}

.form-input {
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 0.85rem 1rem;
  color: #f8fafc;
  font-size: 1rem;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.form-input::placeholder {
  color: #475569;
}

.form-input:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1);
}

.form-input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.form-input.input-error {
  border-color: rgba(239, 68, 68, 0.5);
}

.form-input.input-error:focus {
  border-color: rgba(239, 68, 68, 0.7);
  box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.1);
}

.field-hint {
  font-size: 0.75rem;
  color: #475569;
}

.field-hint--error {
  color: #fca5a5;
}

.btn-submit {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.6rem;
  background: #6366f1;
  color: white;
  border: none;
  border-radius: 12px;
  padding: 0.9rem 1.5rem;
  font-size: 0.95rem;
  font-weight: 700;
  cursor: pointer;
  margin-top: 0.5rem;
  box-shadow: 0 4px 14px 0 rgba(99, 102, 241, 0.39);
  transition: all 0.2s ease;
}

.btn-submit:hover:not(:disabled) {
  background: #4f46e5;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.4);
}

.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  filter: grayscale(0.3);
}

.auth-link {
  margin: 1.5rem 0 0;
  text-align: center;
  font-size: 0.9rem;
  color: #94a3b8;
}

.auth-link a {
  color: #818cf8;
  text-decoration: none;
  font-weight: 600;
}

.auth-link a:hover {
  color: #a5b4fc;
  text-decoration: underline;
}

@media (max-width: 480px) {
  .auth-card {
    padding: 2rem 1.5rem;
    border-radius: 20px;
  }
}
</style>

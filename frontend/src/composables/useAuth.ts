import { ref } from 'vue'
import axios, { AxiosError } from 'axios'

interface AuthUser {
  email: string
  name: string
}

interface AuthApiResponse {
  email: string
  name: string
  token?: string | null
}

interface ProblemDetail {
  status?: number
  detail?: string
  title?: string
}

// Estado singleton — compartilhado entre todos os componentes que chamam useAuth()
const user = ref<AuthUser | null>(null)
const isLoggedIn = ref(false)

const JWT_STORAGE_KEY = 'jwt'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  withCredentials: true, // Mantém cookie httpOnly como fallback
  headers: {
    'Content-Type': 'application/json',
  },
})

// Injeta Bearer token em todas as requests quando disponível no localStorage
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(JWT_STORAGE_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function extractErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<ProblemDetail>
    const status = axiosError.response?.status

    if (status === 429) {
      return 'Muitas tentativas. Aguarde 1 minuto.'
    }
    if (status === 409) {
      return 'Este email já está cadastrado.'
    }
    if (status === 401 || status === 403) {
      // Não diferenciar email inexistente de senha errada (segurança)
      return 'Credenciais inválidas.'
    }
    const detail = axiosError.response?.data?.detail
    if (detail) return detail
  }
  return 'Erro inesperado. Tente novamente.'
}

export function useAuth() {
  /**
   * Verifica se há sessão ativa consultando GET /auth/me.
   * Falha silenciosa — não lança erro.
   */
  async function checkAuth(): Promise<void> {
    try {
      const response = await api.get<AuthUser>('/auth/me')
      user.value = response.data
      isLoggedIn.value = true
    } catch {
      user.value = null
      isLoggedIn.value = false
    }
  }

  /**
   * Realiza login com email e senha.
   * Lança erro com mensagem tratada para o componente exibir.
   */
  async function login(email: string, password: string): Promise<void> {
    try {
      const response = await api.post<AuthApiResponse>('/auth/login', { email, password })
      if (response.data.token) {
        localStorage.setItem(JWT_STORAGE_KEY, response.data.token)
      }
      await checkAuth()
    } catch (error) {
      throw new Error(extractErrorMessage(error))
    }
  }

  /**
   * Registra nova conta com nome, email e senha.
   * Lança erro com mensagem tratada para o componente exibir.
   */
  async function register(email: string, password: string, name: string): Promise<void> {
    try {
      const response = await api.post<AuthApiResponse>('/auth/register', { email, password, name })
      if (response.data.token) {
        localStorage.setItem(JWT_STORAGE_KEY, response.data.token)
      }
      await checkAuth()
    } catch (error) {
      throw new Error(extractErrorMessage(error))
    }
  }

  /**
   * Realiza logout — limpa o estado local mesmo se a request falhar.
   */
  async function logout(): Promise<void> {
    try {
      await api.post('/auth/logout')
    } catch {
      // Ignora erros de rede — o estado local é sempre limpo
    } finally {
      localStorage.removeItem(JWT_STORAGE_KEY)
      user.value = null
      isLoggedIn.value = false
    }
  }

  return { user, isLoggedIn, checkAuth, login, register, logout }
}

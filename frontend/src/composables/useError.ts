import { ref, computed, Ref } from 'vue';

export interface UseErrorReturn {
  error: Ref<string>;
  isError: import('vue').ComputedRef<boolean>;
  setError: (message: string, duration?: number) => void;
  clearError: () => void;
}

/**
 * Composable para gerenciar mensagens de erro
 * Suporta auto-limpeza após um período de tempo
 */
export function useError(): UseErrorReturn {
  const error = ref<string>('');
  let clearTimeout: ReturnType<typeof setTimeout> | null = null;

  const isError = computed(() => error.value.length > 0);

  /**
   * Define uma mensagem de erro
   * @param message - Mensagem a exibir
   * @param duration - Tempo em ms antes de limpar automaticamente (opcional)
   */
  const setError = (message: string, duration?: number) => {
    // Limpa timeout anterior se existir
    if (clearTimeout) {
      global.clearTimeout(clearTimeout);
    }

    error.value = message;

    // Auto-limpar se duration foi especificada
    if (duration && duration > 0) {
      clearTimeout = global.setTimeout(() => {
        error.value = '';
      }, duration);
    }
  };

  /**
   * Limpa a mensagem de erro
   */
  const clearError = () => {
    if (clearTimeout) {
      global.clearTimeout(clearTimeout);
    }
    error.value = '';
  };

  return {
    error,
    isError,
    setError,
    clearError,
  };
}

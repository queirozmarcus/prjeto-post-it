<script setup lang="ts">
import { ref, computed } from 'vue';
import { PlusCircle, AlertCircle } from 'lucide-vue-next';
import type { PostitRequest } from '../services/postitApi';

interface Props {
  // Função async que executa a criação — retorna true em sucesso, false em falha
  onSubmit: (payload: PostitRequest) => Promise<boolean>;
}

const props = defineProps<Props>();

const content = ref('');
const color = ref('#fef68a'); // Amarelo post-it clássico pastel
const isLoading = ref(false);
const error = ref('');

// Lista de cores pré-definidas para conveniência
const colorPresets = [
  { label: 'Amarelo', value: '#fef68a' },
  { label: 'Rosa', value: '#fca5a5' },
  { label: 'Azul', value: '#bfdbfe' },
  { label: 'Verde', value: '#bbf7d0' },
  { label: 'Roxo', value: '#e9d5ff' },
  { label: 'Laranja', value: '#fed7aa' },
  { label: 'Vermelho', value: '#fecaca' },
  { label: 'Cinza', value: '#e5e7eb' },
];

const contentLength = computed(() => content.value.length);
const isContentValid = computed(() => content.value.trim().length > 0);
const charLimit = 120;

const handleSubmit = async () => {
  if (!isContentValid.value) {
    error.value = 'O conteúdo da nota não pode estar vazio.';
    return;
  }

  if (contentLength.value > charLimit) {
    error.value = `A nota não pode ter mais de ${charLimit} caracteres.`;
    return;
  }

  isLoading.value = true;
  error.value = '';

  const payload: PostitRequest = {
    content: content.value.trim(),
    color: color.value,
  };

  // Aguarda o resultado real da operação para saber se deve limpar o form
  const success = await props.onSubmit(payload);

  isLoading.value = false;

  if (success) {
    content.value = '';
    color.value = '#fef68a';
  } else {
    // Mantém o conteúdo para o usuário poder corrigir e retentar
    error.value = 'Erro ao criar nota. Verifique sua conexão e tente novamente.';
  }
};

const handleKeydown = (event: KeyboardEvent) => {
  // Ctrl+Enter ou Cmd+Enter para enviar
  if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
    event.preventDefault();
    handleSubmit();
  }
};

const resetForm = () => {
  content.value = '';
  color.value = '#fef68a';
  error.value = '';
};
</script>

<template>
  <div class="postit-form-container">
    <div class="form-header">
      <span class="form-title">O que está na sua mente?</span>
      <div class="color-picker-section">
        <label for="color-picker" class="color-label">Cor:</label>
        <div class="color-inputs">
          <!-- Color Picker nativo -->
          <div class="color-picker-wrapper">
            <input
              id="color-picker"
              v-model="color"
              type="color"
              class="color-input"
              title="Escolha a cor"
              aria-label="Seletor de cor"
            />
          </div>

          <!-- Preset buttons -->
          <div class="color-presets">
            <button
              v-for="preset in colorPresets"
              :key="preset.value"
              class="preset-btn"
              :style="{ backgroundColor: preset.value }"
              :title="preset.label"
              :aria-label="preset.label"
              @click="color = preset.value"
              :class="{ active: color === preset.value }"
            ></button>
          </div>
        </div>
      </div>
    </div>

    <!-- Textarea para conteúdo -->
    <textarea
      v-model="content"
      placeholder="Digite sua nota aqui... (Ctrl + Enter para salvar)"
      class="form-textarea"
      :disabled="isLoading"
      @keydown="handleKeydown"
      :maxlength="charLimit"
      aria-label="Conteúdo da nota"
    ></textarea>

    <!-- Contador de caracteres -->
    <div class="char-counter" :class="{ warning: contentLength >= 100 }">
      {{ contentLength }} / {{ charLimit }}
    </div>

    <!-- Mensagem de erro -->
    <div v-if="error" class="error-message">
      <AlertCircle :size="16" />
      <span>{{ error }}</span>
    </div>

    <!-- Rodapé do formulário -->
    <div class="form-footer">
      <p class="form-hint">💡 Use cores diferentes para categorias. Dica: Ctrl+Enter para salvar.</p>
      <div class="form-actions">
        <button
          v-if="content"
          class="btn-reset"
          @click="resetForm"
          :disabled="isLoading"
          aria-label="Limpar formulário"
        >
          Limpar
        </button>
        <button
          class="btn-submit"
          @click="handleSubmit"
          :disabled="!isContentValid || isLoading"
          :aria-busy="isLoading"
        >
          <PlusCircle v-if="!isLoading" :size="18" />
          <span v-if="isLoading">Salvando...</span>
          <span v-else>Criar Nota</span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.postit-form-container {
  background: var(--card-bg, rgba(255, 255, 255, 0.05));
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  padding: 1.5rem;
  margin-bottom: 4rem;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
}

.form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.form-title {
  color: var(--text-dim, #94a3b8);
  font-size: 0.9rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.color-picker-section {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.color-label {
  color: var(--text-dim, #94a3b8);
  font-size: 0.8rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.color-inputs {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.color-picker-wrapper {
  position: relative;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid rgba(255, 255, 255, 0.3);
  cursor: pointer;
  transition: all 0.2s ease;
}

.color-picker-wrapper:hover {
  border-color: rgba(255, 255, 255, 0.6);
  box-shadow: 0 0 12px rgba(99, 102, 241, 0.3);
}

.color-input {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  cursor: pointer;
  border: none;
}

.color-presets {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.preset-btn {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.2s ease;
}

.preset-btn:hover {
  transform: scale(1.15);
  box-shadow: 0 0 12px rgba(0, 0, 0, 0.3);
}

.preset-btn.active {
  border-color: white;
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.2);
}

.form-textarea {
  width: 100%;
  height: 140px;
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  padding: 1.25rem;
  color: var(--text-main, #f8fafc);
  font-size: 1.1rem;
  font-family: 'Inter', sans-serif;
  resize: vertical;
  outline: none;
  transition: all 0.2s ease;
}

.form-textarea:focus {
  border-color: var(--primary, #6366f1);
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1);
}

.form-textarea:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.char-counter {
  text-align: right;
  font-size: 0.75rem;
  color: var(--text-dim, #94a3b8);
  margin-top: 0.5rem;
  transition: color 0.2s ease;
}

.char-counter.warning {
  color: #fbbf24;
  font-weight: 600;
}

.error-message {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 8px;
  padding: 0.75rem 1rem;
  color: #fca5a5;
  font-size: 0.9rem;
  margin-top: 1rem;
}

.form-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1.5rem;
  margin-top: 1.5rem;
}

.form-hint {
  font-size: 0.8rem;
  color: var(--text-dim, #94a3b8);
  margin: 0;
}

.form-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.btn-submit,
.btn-reset {
  border: none;
  padding: 0.8rem 1.5rem;
  border-radius: 12px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.6rem;
  transition: all 0.2s ease;
  font-size: 0.95rem;
}

.btn-submit {
  background: var(--primary, #6366f1);
  color: white;
  box-shadow: 0 4px 14px 0 rgba(99, 102, 241, 0.39);
}

.btn-submit:hover:not(:disabled) {
  background: var(--primary-hover, #4f46e5);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.23);
}

.btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  filter: grayscale(1);
}

.btn-reset {
  background: rgba(255, 255, 255, 0.1);
  color: var(--text-dim, #94a3b8);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.btn-reset:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.15);
  color: var(--text-main, #f8fafc);
}

.btn-reset:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Responsividade */
@media (max-width: 768px) {
  .form-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .color-picker-section {
    width: 100%;
    justify-content: flex-start;
  }

  .form-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .form-hint {
    text-align: center;
  }

  .form-actions {
    justify-content: flex-end;
  }
}

@media (max-width: 640px) {
  .postit-form-container {
    padding: 1rem;
  }

  .form-textarea {
    height: 100px;
    font-size: 1rem;
  }

  .color-presets {
    gap: 0.25rem;
  }

  .preset-btn {
    width: 24px;
    height: 24px;
  }

  .form-actions {
    flex-direction: column-reverse;
  }

  .btn-submit,
  .btn-reset {
    width: 100%;
    justify-content: center;
  }
}
</style>

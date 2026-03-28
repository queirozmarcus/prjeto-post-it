<script setup lang="ts">
import { computed } from 'vue';
import { Trash2 } from 'lucide-vue-next';
import type { Postit } from '../services/postitApi';

interface Props {
  postit: Postit;
}

interface Emits {
  (e: 'delete', id: number): void;
}

const props = defineProps<Props>();
defineEmits<Emits>();

// Calcula se o texto da nota é claro ou escuro para melhor contraste
const textColor = computed(() => {
  const color = props.postit.color;
  const hex = color.replace('#', '');
  const r = parseInt(hex.substring(0, 2), 16);
  const g = parseInt(hex.substring(2, 4), 16);
  const b = parseInt(hex.substring(4, 6), 16);

  // Luminância relativa (WCAG)
  const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
  return luminance > 0.5 ? '#1e293b' : '#ffffff';
});
</script>

<template>
  <div
    class="postit-card"
    :style="{
      backgroundColor: postit.color,
      color: textColor,
    }"
  >
    <!-- Pin no topo -->
    <div class="postit-pin"></div>

    <!-- Conteúdo da nota -->
    <div class="postit-content">
      <p class="postit-text">{{ postit.content }}</p>
    </div>

    <!-- Rodapé com ação de delete -->
    <div class="postit-footer">
      <button
        class="btn-delete"
        @click="$emit('delete', postit.id)"
        :title="`Excluir nota ${postit.id}`"
        aria-label="Excluir nota"
      >
        <Trash2 :size="16" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.postit-card {
  min-height: 260px;
  max-height: 360px;
  padding: 2rem 1.5rem 1.5rem;
  border-radius: 2px 2px 40px 2px;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-shadow: 5px 5px 15px rgba(0, 0, 0, 0.3),
    inset 0 -10px 20px rgba(0, 0, 0, 0.05);
  transform: rotate(var(--rotation, -1deg));
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* Rotações variadas para visual de post-it físico */
.postit-card:nth-child(1) {
  --rotation: -1deg;
}
.postit-card:nth-child(2) {
  --rotation: 1.5deg;
}
.postit-card:nth-child(3n) {
  --rotation: -2deg;
}
.postit-card:nth-child(4n) {
  --rotation: 1deg;
}

.postit-card:hover {
  transform: scale(1.05) rotate(0deg);
  z-index: 10;
  box-shadow: 10px 20px 30px rgba(0, 0, 0, 0.4);
}

/* Pin no topo */
.postit-pin {
  position: absolute;
  top: 10px;
  left: 50%;
  transform: translateX(-50%);
  width: 12px;
  height: 12px;
  background: rgba(0, 0, 0, 0.1);
  border-radius: 50%;
  box-shadow: inset 2px 2px 4px rgba(0, 0, 0, 0.2);
}

/* Conteúdo */
.postit-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow-y: auto;
}

.postit-text {
  font-family: 'Gochi Hand', cursive;
  font-size: 1.4rem;
  line-height: 1.4;
  margin: 0;
  word-wrap: break-word;
  white-space: pre-wrap;
  overflow-wrap: break-word;
}

/* Rodapé com botão delete */
.postit-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 0.5rem;
  opacity: 0;
  transform: translateY(10px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.postit-card:hover .postit-footer {
  opacity: 1;
  transform: translateY(0);
}

.btn-delete {
  background: rgba(0, 0, 0, 0.1);
  border: none;
  padding: 0.5rem;
  border-radius: 8px;
  cursor: pointer;
  color: inherit;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.btn-delete:hover {
  background: rgba(255, 0, 0, 0.2);
  color: #ef4444;
  transform: scale(1.1);
}

.btn-delete:active {
  transform: scale(0.95);
}

/* Scroll customizado para conteúdo longo */
.postit-content::-webkit-scrollbar {
  width: 4px;
}

.postit-content::-webkit-scrollbar-track {
  background: transparent;
}

.postit-content::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 4px;
}

.postit-content::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.2);
}
</style>

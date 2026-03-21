<script setup lang="ts">
import { computed } from 'vue';
import PostitCard from './PostitCard.vue';
import type { Postit } from '../services/postitApi';

interface Props {
  postits: Postit[];
  isLoading?: boolean;
}

interface Emits {
  (e: 'delete', id: number): void;
}

withDefaults(defineProps<Props>(), {
  isLoading: false,
});

defineEmits<Emits>();

const isEmpty = computed((props: Props) => props.postits.length === 0);
</script>

<template>
  <section class="postit-grid-section">
    <!-- Estado vazio -->
    <div v-if="isEmpty && !isLoading" class="empty-state">
      <div class="empty-icon">📝</div>
      <h2>Nenhuma nota ainda</h2>
      <p>Crie sua primeira nota acima para começar!</p>
    </div>

    <!-- Estado de carregamento -->
    <div v-else-if="isLoading" class="loading-state">
      <div class="spinner"></div>
      <p>Carregando notas...</p>
    </div>

    <!-- Grid de notas -->
    <TransitionGroup v-else name="list" class="postit-grid">
      <PostitCard
        v-for="postit in postits"
        :key="postit.id"
        :postit="postit"
        @delete="$emit('delete', $event)"
      />
    </TransitionGroup>
  </section>
</template>

<style scoped>
.postit-grid-section {
  min-height: 400px;
}

.postit-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 2rem;
  animation: fadeIn 0.5s ease-in;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* Estado vazio */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  text-align: center;
  color: var(--text-dim, #94a3b8);
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

.empty-state h2 {
  margin: 0 0 0.5rem 0;
  font-size: 1.5rem;
  color: var(--text-main, #f8fafc);
}

.empty-state p {
  margin: 0;
  font-size: 1rem;
}

/* Estado de carregamento */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  gap: 1rem;
  color: var(--text-dim, #94a3b8);
}

.spinner {
  width: 48px;
  height: 48px;
  border: 4px solid rgba(99, 102, 241, 0.2);
  border-top-color: var(--primary, #6366f1);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.loading-state p {
  font-size: 1rem;
  margin: 0;
}

/* Animações de transição de lista */
.list-enter-active,
.list-leave-active {
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

.list-enter-from {
  opacity: 0;
  transform: translateY(30px) scale(0.5);
}

.list-leave-to {
  opacity: 0;
  transform: scale(0.2) rotate(-45deg);
}

.list-move {
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

/* Responsividade */
@media (max-width: 768px) {
  .postit-grid {
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: 1.5rem;
  }
}

@media (max-width: 640px) {
  .postit-grid {
    grid-template-columns: 1fr;
    gap: 1rem;
  }

  .empty-state {
    min-height: 300px;
  }

  .empty-icon {
    font-size: 3rem;
  }

  .empty-state h2 {
    font-size: 1.25rem;
  }
}
</style>

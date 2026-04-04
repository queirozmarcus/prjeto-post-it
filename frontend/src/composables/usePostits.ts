import { ref, computed, Ref } from 'vue';
import { postitApi, type Postit, type PostitRequest } from '../services/postitApi';
import { extractErrorMessage } from '../utils/errorHandler';

export interface UsePostitsReturn {
  postits: Ref<Postit[]>;
  isLoading: Ref<boolean>;
  error: Ref<string>;
  isCreating: Ref<boolean>;
  isDeletingId: Ref<number | null>;

  // Métodos
  fetchPostits: () => Promise<void>;
  createPostit: (request: PostitRequest) => Promise<Postit | null>;
  deletePostit: (id: number) => Promise<boolean>;
  updatePostit: (id: number, request: PostitRequest) => Promise<Postit | null>;

  // Computed
  isEmpty: import('vue').ComputedRef<boolean>;
  itemCount: import('vue').ComputedRef<number>;
}

/**
 * Composable para gerenciar estado e operações de post-its
 * Encapsula lógica de API, loading states e error handling
 */
export function usePostits(): UsePostitsReturn {
  const postits = ref<Postit[]>([]);
  const isLoading = ref(false);
  const error = ref('');
  const isCreating = ref(false);
  const isDeletingId = ref<number | null>(null);

  /**
   * Busca todos os post-its da API
   */
  const fetchPostits = async () => {
    try {
      isLoading.value = true;
      error.value = '';
      postits.value = await postitApi.getAllPostits();
    } catch (err) {
      error.value = extractErrorMessage(err);
      console.error('Erro ao buscar postits:', err);
    } finally {
      isLoading.value = false;
    }
  };

  /**
   * Cria um novo post-it
   */
  const createPostit = async (request: PostitRequest): Promise<Postit | null> => {
    try {
      isCreating.value = true;
      error.value = '';

      const newPostit = await postitApi.createPostit(request);

      // Adiciona no início da lista (post-it mais recente primeiro)
      postits.value.unshift(newPostit);

      return newPostit;
    } catch (err) {
      error.value = extractErrorMessage(err);
      console.error('Erro ao criar postit:', err);
      return null;
    } finally {
      isCreating.value = false;
    }
  };

  /**
   * Deleta um post-it por ID
   */
  const deletePostit = async (id: number): Promise<boolean> => {
    try {
      isDeletingId.value = id;
      error.value = '';

      await postitApi.deletePostit(id);

      // Remove da lista
      postits.value = postits.value.filter((p) => p.id !== id);

      return true;
    } catch (err) {
      error.value = extractErrorMessage(err);
      console.error(`Erro ao deletar postit ${id}:`, err);
      return false;
    } finally {
      isDeletingId.value = null;
    }
  };

  /**
   * Atualiza um post-it existente
   */
  const updatePostit = async (id: number, request: PostitRequest): Promise<Postit | null> => {
    try {
      error.value = '';

      const updatedPostit = await postitApi.updatePostit(id, request);

      // Atualiza na lista
      const index = postits.value.findIndex((p) => p.id === id);
      if (index !== -1) {
        postits.value[index] = updatedPostit;
      }

      return updatedPostit;
    } catch (err) {
      error.value = extractErrorMessage(err);
      console.error(`Erro ao atualizar postit ${id}:`, err);
      return null;
    }
  };

  // Computed properties
  const isEmpty = computed(() => postits.value.length === 0);
  const itemCount = computed(() => postits.value.length);

  return {
    postits,
    isLoading,
    error,
    isCreating,
    isDeletingId,

    fetchPostits,
    createPostit,
    deletePostit,
    updatePostit,

    isEmpty,
    itemCount,
  };
}

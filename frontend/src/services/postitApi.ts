import axios, { AxiosInstance } from 'axios';

export interface Postit {
  id: number;
  content: string;
  color: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface PostitRequest {
  content: string;
  color: string;
}

class PostitApiService {
  private api: AxiosInstance;

  constructor() {
    this.api = axios.create({
      baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1',
      timeout: 5000,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Interceptor para logs em desenvolvimento
    if (import.meta.env.DEV) {
      this.api.interceptors.response.use(
        (response) => {
          console.debug('[API] Success:', response.config.method?.toUpperCase(), response.config.url);
          return response;
        },
        (error) => {
          console.error('[API] Error:', error.config?.method?.toUpperCase(), error.config?.url, error.response?.status);
          return Promise.reject(error);
        }
      );
    }
  }

  /**
   * Busca todos os post-its
   */
  async getAllPostits(): Promise<Postit[]> {
    try {
      const response = await this.api.get<Postit[]>('/postits');
      return response.data;
    } catch (error) {
      console.error('Erro ao buscar post-its:', error);
      throw error;
    }
  }

  /**
   * Busca um post-it por ID
   */
  async getPostitById(id: number): Promise<Postit> {
    try {
      const response = await this.api.get<Postit>(`/postits/${id}`);
      return response.data;
    } catch (error) {
      console.error(`Erro ao buscar post-it ${id}:`, error);
      throw error;
    }
  }

  /**
   * Cria um novo post-it
   */
  async createPostit(request: PostitRequest): Promise<Postit> {
    try {
      const response = await this.api.post<Postit>('/postits', request);
      return response.data;
    } catch (error) {
      console.error('Erro ao criar post-it:', error);
      throw error;
    }
  }

  /**
   * Atualiza um post-it existente
   */
  async updatePostit(id: number, request: PostitRequest): Promise<Postit> {
    try {
      const response = await this.api.put<Postit>(`/postits/${id}`, request);
      return response.data;
    } catch (error) {
      console.error(`Erro ao atualizar post-it ${id}:`, error);
      throw error;
    }
  }

  /**
   * Deleta um post-it por ID
   */
  async deletePostit(id: number): Promise<void> {
    try {
      await this.api.delete(`/postits/${id}`);
    } catch (error) {
      console.error(`Erro ao deletar post-it ${id}:`, error);
      throw error;
    }
  }
}

export const postitApi = new PostitApiService();

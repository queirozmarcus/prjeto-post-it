import { describe, it, expect } from 'vitest';
import { extractErrorMessage } from '../errorHandler';

describe('extractErrorMessage', () => {
  it('should extract detail from RFC 9457 Problem Details', () => {
    const error = {
      response: {
        status: 400,
        data: {
          type: 'https://api.postits.local/errors/validation',
          title: 'Erro de validação',
          detail: 'O conteúdo deve ter no máximo 120 caracteres',
        },
      },
    };

    expect(extractErrorMessage(error)).toBe('O conteúdo deve ter no máximo 120 caracteres');
  });

  it('should return generic message for network errors', () => {
    const error = {
      message: 'Network Error',
    };

    expect(extractErrorMessage(error)).toBe(
      'Erro de conexão. Verifique sua internet e tente novamente.'
    );
  });

  it('should return fallback message for 400 without detail', () => {
    const error = {
      response: {
        status: 400,
        data: {},
      },
    };

    expect(extractErrorMessage(error)).toBe('Dados inválidos. Verifique os campos e tente novamente.');
  });

  it('should return specific message for 401', () => {
    const error = {
      response: {
        status: 401,
        data: {},
      },
    };

    expect(extractErrorMessage(error)).toBe('Não autenticado. Faça login novamente.');
  });

  it('should return specific message for 403', () => {
    const error = {
      response: {
        status: 403,
        data: {},
      },
    };

    expect(extractErrorMessage(error)).toBe('Você não tem permissão para realizar esta ação.');
  });

  it('should return specific message for 404', () => {
    const error = {
      response: {
        status: 404,
        data: {},
      },
    };

    expect(extractErrorMessage(error)).toBe('Recurso não encontrado.');
  });

  it('should return specific message for 429', () => {
    const error = {
      response: {
        status: 429,
        data: {},
      },
    };

    expect(extractErrorMessage(error)).toBe(
      'Muitas tentativas. Aguarde alguns minutos e tente novamente.'
    );
  });

  it('should return specific message for 500', () => {
    const error = {
      response: {
        status: 500,
        data: {},
      },
    };

    expect(extractErrorMessage(error)).toBe('Erro interno do servidor. Tente novamente mais tarde.');
  });

  it('should return generic message for unknown status codes', () => {
    const error = {
      response: {
        status: 418,
        data: {},
      },
    };

    expect(extractErrorMessage(error)).toBe('Erro inesperado. Tente novamente.');
  });

  it('should extract complex validation error from Problem Details', () => {
    const error = {
      response: {
        status: 400,
        data: {
          type: 'https://api.postits.local/errors/validation',
          title: 'Erro de validação',
          detail: 'content: O conteúdo é obrigatório, color: A cor deve ser um hexadecimal válido',
        },
      },
    };

    expect(extractErrorMessage(error)).toBe(
      'content: O conteúdo é obrigatório, color: A cor deve ser um hexadecimal válido'
    );
  });
});

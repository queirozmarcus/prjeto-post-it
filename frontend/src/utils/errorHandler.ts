/**
 * Extrai mensagem de erro de uma resposta HTTP.
 * Suporta RFC 9457 (Problem Details) e fallback para mensagens genéricas.
 */
export function extractErrorMessage(error: any): string {
  // Erro de rede/timeout sem resposta
  if (!error.response) {
    return 'Erro de conexão. Verifique sua internet e tente novamente.';
  }

  const { status, data } = error.response;

  // RFC 9457 Problem Details
  if (data?.detail) {
    return data.detail;
  }

  // Fallback por status code
  switch (status) {
    case 400:
      return 'Dados inválidos. Verifique os campos e tente novamente.';
    case 401:
      return 'Não autenticado. Faça login novamente.';
    case 403:
      return 'Você não tem permissão para realizar esta ação.';
    case 404:
      return 'Recurso não encontrado.';
    case 409:
      return 'Conflito. O recurso já existe ou está em uso.';
    case 422:
      return 'Não foi possível processar a requisição. Verifique os dados.';
    case 429:
      return 'Muitas tentativas. Aguarde alguns minutos e tente novamente.';
    case 500:
      return 'Erro interno do servidor. Tente novamente mais tarde.';
    case 503:
      return 'Serviço temporariamente indisponível. Tente novamente em instantes.';
    default:
      return 'Erro inesperado. Tente novamente.';
  }
}

-- Índice composto para suportar paginação eficiente: WHERE user_id = ? ORDER BY created_at DESC
CREATE INDEX idx_postits_user_id_created_at ON postits(user_id, created_at DESC);

-- Cobertura para ordenação por updated_at
CREATE INDEX idx_postits_user_id_updated_at ON postits(user_id, updated_at DESC);

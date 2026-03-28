-- Adiciona user_id como nullable (dados existentes ficam sem owner)
-- NOT NULL será aplicado na V4 após todos os postits serem associados

ALTER TABLE postits
    ADD COLUMN user_id BIGINT,
    ADD CONSTRAINT fk_postits_user_id
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

CREATE INDEX idx_postits_user_id ON postits(user_id);

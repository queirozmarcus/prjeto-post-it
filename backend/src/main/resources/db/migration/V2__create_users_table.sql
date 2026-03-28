-- Tabela de usuários para autenticação local (email + senha)
-- Ownership de postits por usuário será adicionado em V3 via FK
CREATE TABLE users (
    -- Chave primária auto-incremental (consistente com tabela postits)
    id            BIGSERIAL PRIMARY KEY,

    -- Email único: identificador de login e chave de negócio
    email         VARCHAR(255) NOT NULL,

    -- Hash BCrypt da senha — nunca armazenar senha em texto puro
    password_hash VARCHAR(255) NOT NULL,

    -- Nome de exibição do usuário
    name          VARCHAR(100) NOT NULL,

    -- Timestamp de criação — imutável após insert
    created_at    TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Timestamp de última atualização — atualizado a cada UPDATE
    updated_at    TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Índice único em email: garante unicidade no nível do banco e suporta
-- lookup O(log n) no fluxo de autenticação (SELECT WHERE email = ?)
CREATE UNIQUE INDEX uq_users_email ON users(email);

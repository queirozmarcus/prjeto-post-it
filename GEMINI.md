# GEMINI.md - Contexto do Projeto prjeto-post-it

Este arquivo fornece orientações e contexto para o Gemini CLI operar no projeto `prjeto-post-it`.

## 🌍 Visão Geral do Projeto

O **prjeto-post-it** é um sistema de gerenciamento de notas adesivas (Post-its) de alta performance, projetado para execução local via Docker. O foco técnico está na robustez, seguindo padrões de indústria como Arquitetura Hexagonal, migrations versionadas e tratamento de erros padronizado.

### Principais Tecnologias
- **Backend:** Java 21 + Spring Boot 3.x (Arquitetura Hexagonal)
- **Banco de Dados:** PostgreSQL 16 (containerizado)
- **Frontend:** React (previsto)
- **Infraestrutura:** Docker & Docker Compose
- **Migrations:** Flyway
- **Padronização:** RFC 9457 para respostas de erro (Problem Details)

## 📂 Estado Atual: Bootstrap

O projeto encontra-se em fase de **bootstrap**. A raiz contém diversos scripts Python e Shell destinados a gerar a estrutura completa do backend e frontend.

### Estrutura de Diretórios Alvo (Pós-Bootstrap)
- `backend/`: Código fonte Java/Spring.
- `frontend/`: Código fonte React.
- `docker-compose.yml`: Orquestração de containers.

## 🚀 Comandos de Bootstrap

Para inicializar o projeto, utilize os seguintes scripts:

```bash
# Executa o processo completo de bootstrap (criação de diretórios e arquivos)
./EXECUTE_THIS_FULL_BOOTSTRAP.sh

# Ou via Makefile (se disponível/configurado)
make bootstrap

# Script Python alternativo para geração total
python3 COMPLETE_BOOTSTRAP.py
```

## 🛠️ Convenções de Desenvolvimento (Backend)

Os agentes e desenvolvedores devem seguir estas diretrizes ao implementar o backend:

- **Arquitetura Hexagonal:**
  - `domain`: Regras de negócio puras (Records/Classes sem frameworks).
  - `application.ports`: Interfaces de entrada (Inbound) e saída (Outbound).
  - `application.usecases`: Implementação da lógica de aplicação.
  - `infrastructure.adapters.in`: Controllers REST e adaptadores de entrada.
  - `infrastructure.adapters.out`: Repositórios (Persistence) e adaptadores de saída.
- **Testes:**
  - Utilizar **JUnit 5**, **AssertJ** e **Testcontainers** para testes de integração com banco real.
  - Padrão **Object Mother** para fixtures.
- **API:**
  - Endpoints em `/api/v1/postits`.
  - Content-Type de erro: `application/problem+json`.

## 📜 Documentação de Referência

- `REQUISITOS.md`: Detalhamento funcional e não funcional completo.
- `README_BOOTSTRAP.md`: Instruções rápidas sobre o estado de bootstrap.
- `PROXIMO_PASSO.txt`: Notas sobre as próximas tarefas pendentes.

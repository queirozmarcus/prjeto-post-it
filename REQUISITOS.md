Levantamento de Requisitos: Sistema de Controle de Post-it (Dockerized)

1. Objetivo do Projeto

O propósito deste projeto é estabelecer uma solução de gerenciamento de notas adesivas (Post-its) de alta performance para execução local, garantindo persistência de dados e facilidade de implantação via orquestração de containers. O sistema é direcionado a desenvolvedores que necessitam de uma ferramenta de organização ágil em seus ambientes de trabalho, priorizando a portabilidade total através do docker-compose. Como Arquiteto Sênior, este documento foca na robustez técnica, seguindo padrões de indústria como Arquitetura Hexagonal, migrations versionadas e tratamento de erros padronizado.

2. Requisitos Funcionais (RF)

O sistema deve ser construído sobre o paradigma de Arquitetura Hexagonal (Ports and Adapters). A lógica de negócio (Domain) deve ser isolada de frameworks, comunicando-se através de Inbound Ports (Interfaces de entrada) e Outbound Ports (Interfaces de saída).

* RF01 - Criação de Post-it: O sistema deve permitir a persistência de uma nova nota.
  * Fluxo: REST Controller (Adapter In) -> PostitService (Use Case/Inbound Port) -> PostitRepository (Outbound Port) -> PostgreSQL (Adapter Out).
  * Atributos: Texto (obrigatório), Categoria/Cor (Hexadecimal).
* RF02 - Listagem de Post-its: Recuperação de todas as notas ativas. O Use Case deve retornar objetos de domínio convertidos em DTOs na camada de saída.
* RF03 - Edição de Post-it: Alteração de conteúdo e metadados (cor/categoria). Deve validar a existência do ID antes da operação.
* RF04 - Exclusão de Post-it: Remoção física do registro no banco de dados.

3. Requisitos Não Funcionais (RNF)

ID	Categoria	Descrição	Critério de Sucesso
RNF01	Persistência	Uso de volumes nomeados no Docker para PostgreSQL.	Dados mantidos após docker-compose down. Volume: postit_data.
RNF02	Portabilidade	Orquestração completa via comando único.	Execução via docker-compose up -d sem intervenção manual.
RNF03	Disponibilidade	Healthchecks integrados para readiness do ecossistema.	API utiliza /actuator/health para sinalizar prontidão ao Docker.
RNF04	Performance	Latência reduzida em operações de leitura local.	Tempo de resposta para GET /api/v1/postits < 100ms.
RNF05	Evolução de DB	Versionamento de schema via Flyway.	Script V1__create_postit_table.sql executado no startup.

4. Especificação Técnica (Stack)

Baseada no "Dev Pack" corporativo para garantir manutenibilidade e escalabilidade:

* Backend:
  * Java 21 + Spring Boot 3.x: Utilizando record classes e virtual threads para eficiência.
  * Arquitetura: Hexagonal (Clean Architecture). Domínio livre de anotações @Entity onde possível, usando mapeadores para persistência.
  * Flyway: Migrations SQL seguindo o padrão V{n}__descricao.sql.
* Banco de Dados:
  * PostgreSQL 16: Instância containerizada com persistência em volume.
* Frontend:
  * React ou Vue.js: Interface baseada em componentes reutilizáveis, servida via Nginx no container.
* Infraestrutura:
  * Docker & Docker Compose: Redes isoladas e políticas de restart.

5. Endpoints da API (REST & RFC 9457)

A API deve seguir a semântica HTTP estrita. Erros devem obrigatoriamente retornar o Content-Type: application/problem+json.

Contratos de Sucesso

* POST /api/v1/postits -> 201 Created (Header Location incluído).
* GET /api/v1/postits -> 200 OK.
* PUT /api/v1/postits/{id} -> 200 OK ou 204 No Content.
* DELETE /api/v1/postits/{id} -> 204 No Content.

Validação e Erros (RFC 9457)

Payloads de POST e PUT não podem ter content nulo e o campo color deve validar padrão Hex (ex: #FFFFFF).

Exemplo de erro (404 Not Found):

{
  "type": "https://api.postits.local/errors/not-found",
  "title": "Post-it não encontrado",
  "status": 404,
  "detail": "Não foi possível encontrar uma nota com o ID: 999",
  "instance": "/api/v1/postits/999"
}


6. Estrutura do Docker Compose

Orquestração baseada em estados de saúde (healthchecks) para evitar falhas de conexão na subida do backend.

* Service: db
  * Image: postgres:16-alpine.
  * Healthcheck: test: ["CMD-SHELL", "pg_isready -U user -d postit_db"].
  * Volumes: postit_data:/var/lib/postgresql/data.
* Service: api
  * Build: Multi-stage Dockerfile (Maven Build -> JRE Run).
  * Depends_on: db com condition: service_healthy.
  * Env: SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/postit_db.
  * Ports: 8080:8080.
* Service: frontend
  * Ports: 3000:3000.
* Networks: postit_network (driver: bridge).

7. Critérios de Aceite

* [ ] O sistema sobe integralmente com docker-compose up -d.
* [ ] O log do container api confirma: Successfully applied 1 migration to schema (Flyway).
* [ ] A estrutura de pacotes respeita: domain, application.ports, application.usecases, infrastructure.adapters.
* [ ] Testes de integração utilizam Testcontainers para validar o repositório PostgreSQL.
* [ ] Testes unitários utilizam o padrão Object Mother para criação de fixtures de Post-it.
* [ ] Ao tentar atualizar um ID inexistente, a API retorna erro 404 formatado via RFC 9457.
* [ ] Dados persistem após docker-compose down && docker-compose up.

8. Instruções de Execução e Validação

Para validar a resiliência do ambiente e o status do banco de dados:

1. Subir serviços e verificar saúde:
2. Validar execução das migrations e startup do Spring:
3. Inspecionar logs do banco de dados (troubleshooting de conexão):
4. Teste de carga básico (Latência):

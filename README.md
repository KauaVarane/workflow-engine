# Workflow Engine

API REST em Java para criar e executar **workflows**: um gatilho (HTTP ou agendado) dispara uma sequência de ações, e cada execução gera logs. Projeto de portfólio.

## Tecnologias

- Java 21, Spring Boot 3.3.4, Maven
- Spring Data JPA, PostgreSQL, Flyway (migrações em `src/main/resources/db`)
- Spring Security com JWT (jjwt)
- Swagger / springdoc-openapi, Validation, Actuator, Lombok
- Docker e Docker Compose

## Como rodar (Docker Compose)

Requisito: Docker Desktop aberto.

```
docker compose up --build
```

Depois, abra o Swagger: http://localhost:8080/swagger-ui/index.html

O `docker-compose.yml` já traz uma senha de banco e uma chave JWT **somente de desenvolvimento local**. Para trocá-las, crie um arquivo `.env` na raiz (ele não vai para o Git):

```
DB_PASSWORD=sua-senha
JWT_SECRET=chave-em-base64-com-pelo-menos-32-bytes
```

Para gerar uma chave JWT no PowerShell:

```
$b = New-Object byte[] 32; (New-Object System.Security.Cryptography.RNGCryptoServiceProvider).GetBytes($b); [Convert]::ToBase64String($b)
```

O banco fica exposto apenas em `127.0.0.1:5433` (porta 5433 do seu computador).

## Como rodar sem Docker

Precisa de um PostgreSQL local com o banco `workflow_engine` e destas variáveis de ambiente definidas antes de iniciar:

- `JWT_SECRET` (Base64, pelo menos 32 bytes). Não há valor padrão: sem ela a aplicação não sobe.
- `SPRING_DATASOURCE_URL` (por exemplo `jdbc:postgresql://localhost:5432/workflow_engine`)
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

Depois: `./mvnw spring-boot:run` (no Windows: `.\mvnw spring-boot:run`).

## Autenticação

1. `POST /api/auth/register` com `{"username": "...", "password": "..."}` cria a conta.
2. `POST /api/auth/login` com o mesmo corpo devolve `{"token": "...", "username": "..."}`.
3. No Swagger, clique em **Authorize** e cole o token (formato Bearer).
4. Os demais endpoints exigem o token. São públicos: `/api/auth/**`, Swagger, `/actuator/health` e `/actuator/info`.

## Endpoints principais

**Workflows**
- `GET /api/workflows` e `POST /api/workflows`
- `GET`, `PUT` e `DELETE /api/workflows/{id}`
- `POST /api/workflows/{id}/execute`
- `POST /api/workflows/{id}/duplicate`
- `PATCH /api/workflows/{id}/active`

**Logs**
- `GET` e `DELETE /api/logs`
- `GET` e `DELETE /api/logs/{id}`
- `GET /api/logs/{id}/details`

**Dashboard**
- `GET /api/stats`

Exemplo de corpo para criar um workflow:

```
{
  "name": "Meu workflow",
  "description": "Exemplo",
  "triggerType": "HTTP",
  "active": true,
  "intervalMinutes": 0,
  "actions": [
    {
      "actionType": "LOG",
      "sequenceOrder": 1,
      "parameters": { "message": "Olá!" },
      "maxRetries": 0
    }
  ]
}
```

Tipos de ação: LOG, DATABASE, FILE, HTTP, DECISION, NOTIFICATION e WINDOWS_NOTIFICATION.

## Demonstração online (Render, plano gratuito)

https://workflow-engine-h3g5.onrender.com/swagger-ui/index.html

- O plano gratuito "dorme" após 15 minutos sem uso: a **primeira abertura leva cerca de 1 a 2 minutos**.
- O banco gratuito do Render expira e precisa ser recriado (as tabelas são recriadas pelo Flyway). Dados de teste podem sumir.
- Os agendamentos (scheduler) não rodam enquanto o serviço está dormindo.

## Limitações conhecidas

- O cadastro (`/api/auth/register`) é aberto: qualquer pessoa pode criar uma conta.
- `DELETE /api/logs` apaga os logs de **todos** os usuários.
- A ação **DATABASE** executa SQL. Ela fica desligada no Render (`WORKFLOW_DATABASE_ACTION_ENABLED=false`).
- A ação **FILE** é restrita a uma pasta própria (bloqueia `../` e caminhos absolutos), mas não limita o tamanho nem a quantidade de arquivos. No Render gratuito os arquivos somem ao reiniciar.
- É um projeto de portfólio: **não use dados reais**.
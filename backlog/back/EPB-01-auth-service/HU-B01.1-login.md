# HU-B01.1 — Login com e-mail e senha

**Épico:** [EPB-01](README.md) · **Unidade:** `auth-service` · **Estimativa:** 5 pts · **Depende de:** HU-B00.3, HU-B00.4

## História

Como **usuário da loja**, quero **entrar com meu e-mail e senha** para **receber um token e usar o sistema**.

## Requisitos funcionais

- RF01 — `POST /api/auth/login` recebe `{email, senha}`.
- RF02 — Busca o usuário pelo GSI `email-index` da tabela `users` e compara a senha com o hash BCrypt.
- RF03 — Em caso de sucesso, gera `token` (access, expira em 8 h) e `refreshToken` (expira em 7 dias), assinados em HS512 com a chave de `wc-jwt-secret`. Claims: `sub` (usuarioId), `papel`, `jti`, `iat`, `exp`, `typ` (`access`/`refresh`).
- RF04 — Responde `200` com `{token, refreshToken, usuario:{id, nome, email, papel}, expiresIn: 28800}`.
- RF05 — Cria a sessão no Redis (`session:{userId}:{sha256(token)}`, TTL 8 h) e atualiza `ultimoLogin` no usuário.

## Regras e requisitos não funcionais

- E-mail obrigatório e válido; senha obrigatória → `400` com `campos`.
- E-mail inexistente e senha errada respondem a **mesma** mensagem (`401`, "E-mail ou senha inválidos") para não revelar quais e-mails existem.
- Usuário com `ativo = false` → `401` com a mesma mensagem.
- Chave JWT lida do Secrets Manager na subida e mantida em memória.
- Tempo de resposta p95 ≤ 300 ms local.

## Critérios de aceite

```gherkin
Cenário: login com sucesso
  Dado o usuário "admin@wcautopecas.com.br" ativo com a senha correta
  Quando faço POST /api/auth/login
  Então recebo 200 com token, refreshToken, usuario.papel = "ADMIN" e expiresIn = 28800
  E existe uma chave "session:{id}:*" no Redis com TTL de até 8 horas

Cenário: senha errada
  Quando faço login com senha errada
  Então recebo 401 com message "E-mail ou senha inválidos"

Cenário: e-mail inexistente
  Quando faço login com e-mail não cadastrado
  Então recebo 401 com a mesma message do cenário anterior

Cenário: dados inválidos
  Quando envio email "abc" e senha vazia
  Então recebo 400 com campos "email" e "senha"
```

## Subtasks

- [ ] ST-B01.1.1 Entidade `Usuario` e repositório DynamoDB (Enhanced Client)
- [ ] ST-B01.1.2 `JwtService` (emitir e validar, HS512)
- [ ] ST-B01.1.3 `SessionStore` no Redis
- [ ] ST-B01.1.4 `AuthController` e `LoginService`
- [ ] ST-B01.1.5 DTOs com Bean Validation
- [ ] ST-B01.1.6 Entrada `auth-service` no `docker-compose.yml`

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B01.1.1 | Unitário | `JwtService` gera token com claims e expiração corretas | sucesso |
| TC-B01.1.2 | Unitário | `LoginService` com senha errada e usuário inexistente lança a mesma exceção | senha errada / inexistente |
| TC-B01.1.3 | Integração (LocalStack + Redis) | login de ponta a ponta grava sessão e `ultimoLogin` | sucesso |
| TC-B01.1.4 | API (RestAssured) | payload inválido → 400 com `campos` | dados inválidos |
| TC-B01.1.5 | Contrato | resposta valida contra o JSON Schema de `LoginResponse` usado pelo front | sucesso |

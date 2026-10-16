# HU-B01.6 — Papéis e autorização por operação

**Épico:** [EPB-01](README.md) · **Unidade:** `auth-service` + `wc-commons` · **Estimativa:** 3 pts · **Depende de:** HU-B01.3

## História

Como **dona da loja**, quero **que só administradores façam operações fiscais irreversíveis e ajustes de estoque** para **evitar erros de vendedores**.

## Requisitos funcionais

- RF01 — Enum `Papel` com `ADMIN` e `VENDEDOR` (o seed cria um de cada).
- RF02 — Anotação `@RequerPapel(ADMIN)` no `wc-commons`, avaliada pelo filtro de segurança.
- RF03 — Exigem `ADMIN`: `POST /api/notas/{id}/cancelamento`, `POST /api/notas/inutilizacao`, `POST /api/produtos/{id}/ajuste`, `DELETE /api/produtos/{id}`, `DELETE /api/clientes/{id}`.
- RF04 — Papel insuficiente responde `403` com message "Você não tem permissão para esta operação".

## Regras e requisitos não funcionais

- Decisão de produto: a arquitetura só cita `papel`. Esta matriz é o default do backlog; uma matriz configurável fica fora do escopo (ponto A10 do backlog).

## Critérios de aceite

```gherkin
Cenário: vendedor tenta inutilizar faixa
  Dado um token de usuário VENDEDOR
  Quando faço POST /api/notas/inutilizacao
  Então recebo 403 no formato padrão

Cenário: admin pode
  Dado um token ADMIN
  Quando faço a mesma chamada válida
  Então a requisição segue para a regra de negócio
```

## Subtasks

- [ ] ST-B01.6.1 Enum `Papel` e claim no token
- [ ] ST-B01.6.2 `@RequerPapel` e avaliação no filtro
- [ ] ST-B01.6.3 Documentar a matriz no README do `wc-commons`

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B01.6.1 | Unitário | `@RequerPapel(ADMIN)` com VENDEDOR → 403 | vendedor |
| TC-B01.6.2 | Unitário | com ADMIN → passa | admin |

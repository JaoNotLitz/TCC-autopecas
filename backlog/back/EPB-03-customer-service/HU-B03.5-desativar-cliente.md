# HU-B03.5 — Desativar cliente

**Épico:** [EPB-03](README.md) · **Unidade:** `customer-service` · **Estimativa:** 2 pts · **Depende de:** HU-B03.1, HU-B01.6

## História

Como **administradora**, quero **desativar um cliente sem apagar o histórico** para **manter as notas e vendas antigas consistentes**.

## Requisitos funcionais

- RF01 — `DELETE /api/clientes/{id}` (papel `ADMIN`) define `ativo = false` e responde `204`.
- RF02 — Desativar cliente já inativo responde `204` (idempotente) sem publicar evento de novo.
- RF03 — Invalida cache.

## Critérios de aceite

```gherkin
Cenário: desativar
  Dado um cliente ativo
  Quando um ADMIN chama DELETE /api/clientes/{id}
  Então recebo 204 e GET do cliente mostra ativo false

Cenário: vendedor não pode
  Quando um VENDEDOR chama o DELETE
  Então recebo 403
```

## Subtasks

- [ ] ST-B03.5.1 Endpoint DELETE com `@RequerPapel(ADMIN)`
- [ ] ST-B03.5.2 Idempotência

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B03.5.1 | Integração | desativa e lê | desativar |
| TC-B03.5.2 | API | VENDEDOR → 403 | permissão |
| TC-B03.5.3 | Integração | segundo DELETE não publica evento | RF02 |

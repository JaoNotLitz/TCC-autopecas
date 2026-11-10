# HU-B03.3 — Detalhar cliente com cache (extra)

**Épico:** [EPB-03](README.md) · **Unidade:** `customer-service` · **Estimativa:** 1 pt · **Depende de:** HU-B03.1

## História

Como **sales-service**, quero **consultar um cliente pelo id** para **validar a venda com NF-e e copiar nome e documento para a nota**.

## Requisitos funcionais

- RF01 — `GET /api/clientes/{id}` responde `200` com o `Cliente` (mesmo formato da listagem). O front atual não chama este endpoint; ele serve às chamadas internas (decisão A4).
- RF02 — Cache Redis `customer:{id}` com TTL 1 h, invalidado em PUT e DELETE.
- RF03 — Id inexistente → `404`.

## Critérios de aceite

```gherkin
Cenário: detalhe com cache
  Dado um cliente existente
  Quando chamo GET /api/clientes/{id} duas vezes
  Então a segunda resposta vem do Redis

Cenário: inexistente
  Quando chamo com um id que não existe
  Então recebo 404 no formato padrão
```

## Subtasks

- [ ] ST-B03.3.1 Endpoint e cache

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B03.3.1 | Integração | segunda leitura não consulta o DynamoDB (spy) | cache |
| TC-B03.3.2 | API | 404 | inexistente |

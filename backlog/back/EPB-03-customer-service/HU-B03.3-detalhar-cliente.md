# HU-B03.3 — Detalhar cliente com cache

**Épico:** [EPB-03](README.md) · **Unidade:** `customer-service` · **Estimativa:** 2 pts · **Depende de:** HU-B03.1

## História

Como **vendedor**, quero **ver todos os dados de um cliente** para **conferir endereço antes de emitir uma NF-e**.

## Requisitos funcionais

- RF01 — `GET /api/clientes/{id}` responde `200` com o formato completo da seção 5.2.
- RF02 — Cache Redis `customer:{id}` com TTL 1 h, invalidado em PUT e DELETE.
- RF03 — Id inexistente → `404`.
- RF04 — Campo calculado `enderecoCompleto: boolean` (true se `cep`, `logradouro`, `numero`, `bairro`, `cidade`, `uf` preenchidos), usado pelo front para avisar antes de emitir NF-e.

## Critérios de aceite

```gherkin
Cenário: detalhe com cache
  Dado um cliente existente
  Quando chamo GET /api/clientes/{id} duas vezes
  Então a segunda resposta vem do Redis

Cenário: inexistente
  Quando chamo com um UUID que não existe
  Então recebo 404 no formato padrão
```

## Subtasks

- [ ] ST-B03.3.1 Endpoint e cache
- [ ] ST-B03.3.2 Campo `enderecoCompleto`

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B03.3.1 | Integração | segunda leitura não consulta o DynamoDB (spy) | cache |
| TC-B03.3.2 | API | 404 | inexistente |
| TC-B03.3.3 | Unitário | `enderecoCompleto` com e sem bairro | RF04 |

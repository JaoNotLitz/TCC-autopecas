# HU-B03.2 — Listar e buscar clientes

**Épico:** [EPB-03](README.md) · **Unidade:** `customer-service` · **Estimativa:** 5 pts · **Depende de:** HU-B03.1

## História

Como **vendedor**, quero **buscar clientes por nome, e-mail ou documento e filtrar por tipo e cidade** para **encontrar rápido quem estou atendendo**.

## Requisitos funcionais

- RF01 — `GET /api/clientes` com filtros `tipo`, `busca`, `cidade`, `uf`, `ativo`, `documento`, `limit`, `lastKey`; resposta no envelope `{items, lastKey, count}` com o formato resumido da seção 5.2.
- RF02 — `documento` informado → query exata no `documento-index` (0 ou 1 item).
- RF03 — `busca` só com dígitos → query por documento; com texto → scan com filtro `contains` em `nomeBusca` (nome normalizado: minúsculo, sem acento) e e-mail.
- RF04 — `ativo` → query no `status-index`; sem `ativo`, lista ativos e inativos.
- RF05 — Cache Redis de 5 min para a primeira página de buscas comuns (chave pelo hash dos filtros), invalidado em qualquer escrita de cliente.

## Regras e requisitos não funcionais

- `count` é a quantidade da página, não o total.
- Scan com filtro pode devolver página com menos itens que `limit` e ainda ter `lastKey`; o serviço continua lendo até completar `limit` ou acabar a tabela (no máximo 5 leituras por chamada).
- `tipo` fora de `PF|PJ` ou `uf` inválida → `400`.

## Critérios de aceite

```gherkin
Cenário: busca exata por documento (PDV)
  Dado um cliente com documento 12345678909
  Quando chamo GET /api/clientes?documento=12345678909
  Então recebo items com 1 cliente e lastKey null

Cenário: documento não encontrado
  Quando chamo GET /api/clientes?documento=00000000191
  Então recebo 200 com items vazio

Cenário: busca por nome sem acento
  Dado um cliente "João Peças"
  Quando busco "joao"
  Então ele aparece no resultado

Cenário: paginação
  Dado 45 clientes
  Quando percorro com limit=20 usando lastKey
  Então recebo 45 clientes sem repetição
```

## Subtasks

- [ ] ST-B03.2.1 Atributo `nomeBusca` normalizado na gravação
- [ ] ST-B03.2.2 Estratégia de consulta por filtro (GSI × scan)
- [ ] ST-B03.2.3 Laço de preenchimento de página
- [ ] ST-B03.2.4 Cache e invalidação

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B03.2.1 | Integração | `documento=` encontra e não encontra | documento |
| TC-B03.2.2 | Integração | busca sem acento | nome |
| TC-B03.2.3 | Integração | 45 itens sem repetição | paginação |
| TC-B03.2.4 | Unitário | escolha de estratégia por combinação de filtros | RF02–RF04 |
| TC-B03.2.5 | Integração (Redis) | escrita invalida cache | RF05 |

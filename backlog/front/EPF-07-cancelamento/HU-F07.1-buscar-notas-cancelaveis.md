# HU-F07.1 — Buscar notas canceláveis

**Épico:** [EPF-07](README.md) · **Estimativa:** 2 pts · **Depende de:** HU-B07.2

## História

Como **administradora**, quero **encontrar a nota a cancelar e ver quanto tempo ainda tenho** para **não perder o prazo de 24 horas**.

## Requisitos funcionais

- RF01 — Lista `GET /api/notas?status=autorizada&de={agora − 24h}` paginada; o campo de busca filtra por número ou cliente nas notas carregadas (poucas, pelo limite de 24 h).
- RF02 — Cada linha mostra "Prazo: Xh Ymin restantes" calculado de `autorizadaEm`; menos de 1 h em destaque.
- RF03 — `?notaId=` (vindo do painel) já seleciona a nota.
- RF04 — Remover a lista mockada (`notas` com 4 itens).

## Critérios de aceite

```gherkin
Cenário: prazo visível
  Dado uma nota autorizada há 23 horas
  Então a linha mostra "1h restante" em destaque

Cenário: nota antiga
  Dado uma nota autorizada há 2 dias
  Então ela não aparece na lista
```

## Subtasks

- [ ] ST-F07.1.1 Lista com prazo calculado

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F07.1.1 | Unitário (relógio fixo) | cálculo do prazo | prazo |
| TC-F07.1.2 | Unitário | parâmetro `de` enviado | antiga |

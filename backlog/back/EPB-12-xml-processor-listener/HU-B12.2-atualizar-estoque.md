# HU-B12.2 — Atualizar estoque dos itens vinculados

**Épico:** [EPB-12](README.md) · **Unidade:** `XmlProcessorListener` · **Estimativa:** 2 pts · **Depende de:** HU-B12.1, HU-B05.2

## História

Como **estoquista**, quero **que as quantidades do XML entrem no estoque** para **as peças ficarem disponíveis para venda**.

## Requisitos funcionais

- RF01 — Publica `increase` só para itens vinculados (`referenciaType = entry`).
- RF02 — Itens não vinculados não movimentam estoque; ficam listados no detalhe da entrada.

## Critérios de aceite

```gherkin
Cenário: estoque somado
  Dado PF-001 com estoqueAtual 10 e um XML com 50 unidades dele
  Quando o XML é processado
  Então PF-001 fica com 60

Cenário: item pendente não soma
  Então o item com vinculado false não gera mensagem increase
```

## Subtasks

- [ ] ST-B12.2.1 Publicação por item vinculado

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B12.2.1 | Integração (Compose) | estoque somado | somado |
| TC-B12.2.2 | Integração | sem increase para pendente | pendente |

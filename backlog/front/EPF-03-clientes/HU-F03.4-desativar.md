# HU-F03.4 — Desativar cliente

**Épico:** [EPF-03](README.md) · **Estimativa:** 1 pt · **Depende de:** HU-F00.6, HU-B03.5

## História

Como **administradora**, quero **desativar um cliente com confirmação** para **não fazer isso sem querer**.

## Requisitos funcionais

- RF01 — Botão "Desativar" (só ADMIN) abre confirmação "Desativar {nome}? O histórico será mantido".
- RF02 — Confirma → `DELETE /api/clientes/{id}`; a linha passa a "inativo" (ou some, se o filtro for Ativos).

## Critérios de aceite

```gherkin
Cenário: desativar
  Quando confirmo a desativação
  Então vejo toast de sucesso e o cliente não aparece mais no filtro Ativos
```

## Subtasks

- [ ] ST-F03.4.1 Ação com confirmação

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F03.4.1 | Unitário | cancelar não chama DELETE; confirmar chama | desativar |

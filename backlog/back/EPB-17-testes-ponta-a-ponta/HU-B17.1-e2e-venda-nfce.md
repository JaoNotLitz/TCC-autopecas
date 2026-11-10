# HU-B17.1 — E2E: venda NFC-e até o DANFE

**Épico:** [EPB-17](README.md) · **Unidade:** `e2e-tests` · **Estimativa:** 5 pts · **Depende de:** EPB-01 a EPB-09, EPB-13

## História

Como **banca do TCC**, quero **ver uma venda virar nota autorizada com PDF e estoque baixado** para **confirmar que o fluxo principal funciona de ponta a ponta**.

## Requisitos funcionais

- RF01 — Teste faz login, consulta o estoque do PF-001, cria venda NFC-e finalizada de 2 unidades pelo gateway.
- RF02 — Aguarda (Awaitility, até 60 s) `GET /api/notas?vendaId={id}` trazer a nota da venda com `status: "autorizada"` (mesmo caminho que o PDV do front usa).
- RF03 — Confere: `estoqueAtual` diminuiu 2 e `reservado` voltou ao valor anterior; `GET /api/notas/{id}/xml` baixa um `nfeProc`; `GET /api/notas/{id}/pdf` baixa um PDF que contém a chave.
- RF04 — Repete para NF-e com cliente PJ e confere o `400` de NF-e sem `clienteId`.

## Critérios de aceite

```gherkin
Cenário: fluxo feliz NFC-e
  Dado a stack no ar com seed
  Quando o teste cria uma venda NFC-e
  Então em até 60 segundos a nota está autorizada, com XML e PDF, e o estoque baixado
```

## Subtasks

- [ ] ST-B17.1.1 Base do módulo (Testcontainers `ComposeContainer`, cliente RestAssured autenticado)
- [ ] ST-B17.1.2 Cenário NFC-e
- [ ] ST-B17.1.3 Cenário NF-e e NF-e sem cliente

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B17.1.1 | E2E | NFC-e | fluxo feliz |
| TC-B17.1.2 | E2E | NF-e PJ | RF04 |
| TC-B17.1.3 | E2E | NF-e sem cliente → 400 | RF04 |

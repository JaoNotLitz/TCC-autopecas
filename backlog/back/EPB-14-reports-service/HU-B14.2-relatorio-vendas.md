# HU-B14.2 — Relatório de vendas

**Épico:** [EPB-14](README.md) · **Unidade:** `reports-service` · **Estimativa:** 3 pts · **Depende de:** HU-B06.1

## História

Como **dona da loja**, quero **exportar as vendas de um período** para **analisar em planilha**.

## Requisitos funcionais

- RF01 — `GET /api/relatorios/vendas?de&ate&formato=csv|xlsx&clienteId&vendedorId`.
- RF02 — Colunas da seção 5.8: `Numero, Data, Cliente, Tipo_Documento, Metodo_Pagamento, Subtotal, Desconto, Total, Nota_Fiscal, Status`.
- RF03 — CSV em UTF-8 com BOM, separador `;` e decimal com vírgula (abre certo no Excel pt-BR); XLSX com Apache POI e cabeçalho em negrito.
- RF04 — `Content-Disposition: attachment; filename="vendas_{de}_{ate}.{ext}"`.

## Regras e requisitos não funcionais

- `de` e `ate` obrigatórios, `de ≤ ate`, período máximo de 366 dias → `400`.

## Critérios de aceite

```gherkin
Cenário: CSV de setembro
  Quando peço o relatório de 01/09 a 30/09 em csv
  Então recebo um CSV com uma linha por venda do período e o cabeçalho esperado

Cenário: sem período
  Quando omito "de"
  Então recebo 400
```

## Subtasks

- [ ] ST-B14.2.1 Gerador CSV e XLSX genérico (reaproveitado em 14.3 e 14.4)
- [ ] ST-B14.2.2 Endpoint de vendas

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B14.2.1 | Unitário | CSV com acentos, `;` e vírgula decimal | CSV |
| TC-B14.2.2 | Integração | XLSX abre com POI e tem N linhas | RF03 |
| TC-B14.2.3 | API | validações de período | sem período |

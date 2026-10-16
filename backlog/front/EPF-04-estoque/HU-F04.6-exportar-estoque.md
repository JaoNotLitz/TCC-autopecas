# HU-F04.6 — Exportar relatório de estoque

**Épico:** [EPF-04](README.md) · **Estimativa:** 1 pt · **Depende de:** HU-B14.3

## História

Como **gerente**, quero **baixar a posição do estoque em planilha** para **planejar compras**.

## Requisitos funcionais

- RF01 — Botão "Exportar" com opções CSV e Excel; chama `GET /api/relatorios/estoque?formato=…` com os filtros atuais (`status`, `categoria`) via `requestBlob()` e dispara o download com o nome do `Content-Disposition`.

## Critérios de aceite

```gherkin
Cenário: exportar críticos
  Dado o filtro Crítico
  Quando exporto em Excel
  Então baixo um .xlsx só com os críticos
```

## Subtasks

- [ ] ST-F04.6.1 Utilitário `baixarArquivo(blob, contentDisposition)` em `shared/` (reaproveitado em XML, PDF, relatórios)

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F04.6.1 | Unitário | nome do arquivo a partir do header | exportar |

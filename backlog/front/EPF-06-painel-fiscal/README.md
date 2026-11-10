# EPF-06 — Painel fiscal

**Área:** `pages/painel-fiscal`, `services/notas.service.ts`, `services/documentos.service.ts` · **Rota:** `/fiscal`
**Sprint sugerida:** 5 · **Back:** EPB-07, EPB-13, EPB-14

## Objetivo

Painel com as notas reais: filtros, estatísticas do dia, download de XML/PDF, reenvio de rejeitadas, lote para o contador e exportação. Os 6 botões sem handler (Emitir, Enviar lote, Exportar, Filtrar, XML, PDF) passam a funcionar.

## Histórias

| ID | Título | Pts |
|---|---|---|
| [HU-F06.1](HU-F06.1-listar-filtrar-notas.md) | Listar e filtrar notas | 3 |
| [HU-F06.2](HU-F06.2-download-xml-pdf.md) | Baixar XML e PDF | 2 |
| [HU-F06.3](HU-F06.3-detalhe-reenvio.md) | Detalhe da nota e reenvio de rejeitada | 3 |
| [HU-F06.4](HU-F06.4-lote-contador.md) | Enviar lote ao contador | 3 |
| [HU-F06.5](HU-F06.5-exportar-relatorio.md) | Exportar relatório fiscal | 1 |
| [HU-F06.6](HU-F06.6-emitir-nfe.md) | Botão Emitir NF-e | 1 |

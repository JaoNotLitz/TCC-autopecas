# EPB-11 — entry-service (API)

**Unidade entregue:** microsserviço `entry-service` (API REST, porta 8085). O processamento assíncrono do XML é o épico [EPB-12](../EPB-12-xml-processor-listener/README.md).
**Tipo:** API · **Sprint sugerida:** 6 · **Arquitetura:** seções 4.6 e 5.6

## Objetivo

Registrar notas de entrada de fornecedores (manual ou a partir do XML com conferência) e somar as quantidades ao estoque.

## Dependências de infraestrutura

DynamoDB `entry_notes` (GSIs `fornecedor-index`, `data-index`, `chave-index`), `suppliers` · S3 `wc-xml-storage` · SQS `entry-events-queue`, `inventory-updates-queue` (produtor) · HTTP interno para `product-service` (busca por código) e `fiscal-service` (`POST /internal/notas/entrada`).

## Critérios de aceite do épico

- A tela Nota de Entrada do front registra pelo formulário (`POST /api/notas/entrada`) e preenche o formulário a partir do XML (`POST /api/notas/entrada/importar-xml`, campo `arquivo`).
- A nota registrada aparece em `GET /api/notas` como `tipo: "ENTRADA"`.
- Itens registrados aumentam o estoque via eventos `increase`.

## Histórias

| ID | Título | Pts |
|---|---|---|
| [HU-B11.1](HU-B11.1-registrar-entrada-manual.md) | Registrar nota de entrada | 5 |
| [HU-B11.2](HU-B11.2-previa-xml.md) | Importar XML de entrada (prévia) | 5 |
| [HU-B11.3](HU-B11.3-importar-xml-assincrono.md) | Importar XML para processamento assíncrono (extensão) | 3 |
| [HU-B11.4](HU-B11.4-listar-detalhar-entradas.md) | Listar e detalhar entradas (extra) | 2 |

# HU-B05.5 — Alerta de estoque crítico

**Épico:** [EPB-05](README.md) · **Unidade:** `InventoryUpdateListener` · **Estimativa:** 2 pts · **Depende de:** HU-B05.4

## História

Como **comprador**, quero **ser avisado quando uma peça fica em nível crítico** para **repor antes de faltar**.

## Requisitos funcionais

- RF01 — Quando o status **muda** para `critico` (antes era outro), publica no `low-stock-alerts-topic` a mensagem da seção 7.1 com atributo `status = critico`.
- RF02 — Mudança para `baixo` publica com `status = baixo` (só o webhook futuro recebe; a `email-queue` filtra por crítico).
- RF03 — Permanecer em crítico não publica de novo.

## Critérios de aceite

```gherkin
Cenário: entrou em crítico
  Dado um produto em "atencao"
  Quando uma baixa o leva para "critico"
  Então uma mensagem low_stock_alert é publicada no tópico

Cenário: continua crítico
  Dado um produto em "critico"
  Quando outra baixa acontece
  Então nenhuma mensagem nova é publicada
```

## Subtasks

- [ ] ST-B05.5.1 Detecção de transição
- [ ] ST-B05.5.2 Publicação com atributos de filtro

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B05.5.1 | Integração | assina uma fila de teste no tópico e confere | entrou em crítico |
| TC-B05.5.2 | Unitário | sem transição, sem publicação | continua crítico |

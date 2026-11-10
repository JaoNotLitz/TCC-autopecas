# EPF-00 — Fundação de integração

**Área:** infraestrutura do app Angular 22 (`core/`, `services/`, `shared/`, `environments/`).
**Sprint sugerida:** 1 · **Referência:** `MAPEAMENTO-CLASSES.md` seções 3, 7, 8 e 9

## Objetivo

Deixar pronto tudo que as telas reaproveitam para falar com o back local: URL do gateway, interceptors de autenticação e erro, tipos do contrato, formatadores, paginação por cursor e feedback visual. Ao fim deste épico nenhuma tela nova precisa escrever `fetch`, `alert()` ou máscara à mão.

## Critérios de aceite do épico

- `ng serve` aponta para o API Gateway do LocalStack sem erro de CORS.
- Os tipos de `types/index.ts` batem com os JSON Schemas de contrato do back.
- Nenhuma chamada a `alert()` no código.

## Histórias

| ID | Título | Pts |
|---|---|---|
| [HU-F00.1](HU-F00.1-ambiente-local.md) | Apontar o front para a stack local | 2 |
| [HU-F00.2](HU-F00.2-interceptors-auth-erro.md) | Interceptors de autenticação e erro | 5 |
| [HU-F00.3](HU-F00.3-tipos-contrato.md) | Tipos do contrato e conversores | 3 |
| [HU-F00.4](HU-F00.4-formatadores-validadores.md) | Pipes de formatação e validadores | 3 |
| [HU-F00.5](HU-F00.5-paginacao-cursor.md) | Paginação por cursor ("Carregar mais") | 3 |
| [HU-F00.6](HU-F00.6-feedback-ui.md) | Toasts, confirmação e estados de tela | 3 |

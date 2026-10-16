# HU-B16.4 — Painel de saúde da stack

**Épico:** [EPB-16](README.md) · **Unidade:** `make status` / script `infra/local/status.sh` · **Estimativa:** 2 pts · **Depende de:** HU-B00.5

## História

Como **pessoa que vai apresentar o TCC**, quero **um comando que mostra se tudo está no ar** para **conferir a stack antes da banca**.

## Requisitos funcionais

- RF01 — `make status` lista cada serviço com o resultado de `/actuator/health`, o tamanho de cada fila (visíveis e em voo), da DLQ e o status do `sefaz-mock`.
- RF02 — Código de saída diferente de 0 se algo estiver `DOWN` (usado no CI).

## Critérios de aceite

```gherkin
Cenário: tudo no ar
  Quando executo make status
  Então vejo todos UP e filas com seus tamanhos, e o código de saída é 0
```

## Subtasks

- [ ] ST-B16.4.1 Script de status

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B16.4.1 | Smoke (CI) | stack no ar → 0; serviço parado → ≠ 0 | tudo no ar |

# HU-B17.4 — E2E: falhas, DLQ e redrive

**Épico:** [EPB-17](README.md) · **Unidade:** `e2e-tests` · **Estimativa:** 5 pts · **Depende de:** EPB-16

## História

Como **banca do TCC**, quero **ver o sistema se recuperar de uma SEFAZ fora do ar** para **avaliar a resiliência da arquitetura orientada a eventos**.

## Requisitos funcionais

- RF01 — Configura o `sefaz-mock` (admin) para responder 500, cria uma venda e confirma que após 3 tentativas a mensagem está na DLQ e houve alerta.
- RF02 — Volta o mock ao normal, roda o `wc-dlq-redrive` e confirma a nota autorizada.
- RF03 — Rejeição 2xx forçada pelo admin do mock; com o mock de volta ao normal, o reenvio (`POST /api/notas/{id}/reenviar`) termina autorizado.
- RF04 — Perfil de teste com tempos de backoff reduzidos (segundos em vez de minutos).

## Critérios de aceite

```gherkin
Cenário: recuperação após queda
  Dado a SEFAZ simulada fora do ar
  Quando uma venda é feita
  Então a mensagem vai para a DLQ e um alerta é gerado
  E depois do redrive com a SEFAZ de volta a nota fica autorizada
```

## Subtasks

- [ ] ST-B17.4.1 Controle do mock pelo admin do WireMock
- [ ] ST-B17.4.2 Cenários de queda, redrive e reenvio

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B17.4.1 | E2E | queda + redrive | recuperação |
| TC-B17.4.2 | E2E | rejeição + reenvio | RF03 |

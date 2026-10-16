# HU-B07.4 — Reenviar nota rejeitada

**Épico:** [EPB-07](README.md) · **Unidade:** `fiscal-service` · **Estimativa:** 2 pts · **Depende de:** HU-B09.4

## História

Como **responsável fiscal**, quero **reenviar uma nota rejeitada depois de corrigir a causa** para **não precisar refazer a venda**.

## Requisitos funcionais

- RF01 — `POST /api/notas/{id}/reenviar` só para `status = rejeitada`; senão `422`.
- RF02 — Zera `tentativasEnvio`, volta `status` para `processando` e publica **novo job `generate_xml`** (o XML é regerado para refletir dados corrigidos, como endereço do cliente), que segue para a `sefaz-queue`.
- RF03 — Responde `200` com `{message: "Nota reenviada para processamento", notaId}`.

## Regras e requisitos não funcionais

- A arquitetura diz "publica novamente na `sefaz-queue`"; o backlog regera o XML porque a rejeição costuma vir de dado errado. Se a rejeição for só de comunicação, o efeito é o mesmo.
- Atualização condicional (`status = rejeitada`) evita reenvio duplo.

## Critérios de aceite

```gherkin
Cenário: reenviar
  Dado uma nota rejeitada
  Quando chamo POST /api/notas/{id}/reenviar
  Então recebo 200, a nota fica "processando" e um job generate_xml é publicado

Cenário: nota autorizada
  Quando chamo o reenvio para uma nota autorizada
  Então recebo 422
```

## Subtasks

- [ ] ST-B07.4.1 Endpoint com atualização condicional

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B07.4.1 | Integração | reenvio e job publicado | reenviar |
| TC-B07.4.2 | API | status errado → 422 | autorizada |

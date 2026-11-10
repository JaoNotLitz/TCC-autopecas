# HU-B06.2 — Validar cliente para NF-e

**Épico:** [EPB-06](README.md) · **Unidade:** `sales-service` · **Estimativa:** 2 pts · **Depende de:** HU-B06.1, HU-B03.3

## História

Como **vendedor**, quero **ser avisado na hora se a NF-e está sem cliente válido** para **não gerar uma nota que vai ser rejeitada**.

## Requisitos funcionais

- RF01 — `tipoDocumento = nfe` exige `clienteId`; ausente → `400` com message "NF-e exige cliente identificado".
- RF02 — Consulta `GET http://customer-service:8081/api/clientes/{id}` repassando o token (decisão A4).
- RF03 — Cliente inexistente → `404`; inativo → `422` "Cliente inativo".
- RF04 — `nfce` aceita `clienteId` null; se informado, o cliente precisa existir e estar ativo.
- RF05 — O back não exige endereço: o cadastro do front só tem `cidade` e `uf` (ver seção 5.10 da arquitetura). O simulador da SEFAZ aceita a NF-e assim.

## Regras e requisitos não funcionais

- Timeout de 2 s e 1 retentativa na chamada; customer-service fora → `503` "Serviço de clientes indisponível".

## Critérios de aceite

```gherkin
Cenário: NF-e sem cliente
  Quando envio venda nfe com clienteId null
  Então recebo 400 com message "NF-e exige cliente identificado"

Cenário: NF-e com cliente ativo
  Dado um cliente ativo só com cidade e uf
  Quando envio venda nfe para ele
  Então a validação de cliente passa

Cenário: NFC-e consumidor final
  Quando envio venda nfce com clienteId null
  Então a validação de cliente passa
```

## Subtasks

- [ ] ST-B06.2.1 `CustomerClient` (RestClient + timeout + repasse do token)
- [ ] ST-B06.2.2 `ValidadorClienteNfe`

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B06.2.1 | Unitário (WireMock) | cliente ativo, inexistente, inativo | cenários |
| TC-B06.2.2 | Unitário (WireMock) | timeout → 503 | RNF |
| TC-B06.2.3 | Integração (Compose) | chamada real ao customer-service | NF-e |

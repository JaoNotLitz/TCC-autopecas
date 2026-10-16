# HU-B06.2 — Validar cliente para NF-e

**Épico:** [EPB-06](README.md) · **Unidade:** `sales-service` · **Estimativa:** 3 pts · **Depende de:** HU-B06.1, HU-B03.3

## História

Como **vendedor**, quero **ser avisado na hora se o cliente não tem endereço completo para NF-e** para **completar o cadastro antes de a nota ser rejeitada**.

## Requisitos funcionais

- RF01 — `tipoDocumento = nfe` exige `clienteId`; ausente → `422` com `campos[{campo: "clienteId"}]`.
- RF02 — Consulta `GET http://customer-service:8081/api/clientes/{id}` repassando o token (decisão A4).
- RF03 — Cliente inexistente ou inativo → `422` com `campos clienteId`.
- RF04 — Endereço incompleto → `422` com message "Cliente sem endereço completo para NF-e" e um item em `campos` para cada campo faltante (`cep`, `logradouro`, `numero`, `bairro`, `cidade`, `uf`).
- RF05 — `nfce` aceita `clienteId` null; se informado, o cliente precisa existir e estar ativo.

## Regras e requisitos não funcionais

- Timeout de 2 s e 1 retentativa na chamada; customer-service fora → `503` "Serviço de clientes indisponível".

## Critérios de aceite

```gherkin
Cenário: NF-e sem cliente
  Quando envio venda nfe com clienteId null
  Então recebo 422 com campos "clienteId"

Cenário: NF-e com cliente sem bairro e CEP
  Dado um cliente sem bairro e sem cep
  Quando envio venda nfe para ele
  Então recebo 422 com campos "cep" e "bairro"

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
| TC-B06.2.1 | Unitário (WireMock) | cliente completo, incompleto, inexistente, inativo | cenários |
| TC-B06.2.2 | Unitário (WireMock) | timeout → 503 | RNF |
| TC-B06.2.3 | Integração (Compose) | chamada real ao customer-service | NF-e |

# HU-B02.4 — Repasse de contexto e `correlationId`

**Épico:** [EPB-02](README.md) · **Unidade:** API Gateway + `wc-commons` · **Estimativa:** 2 pts · **Depende de:** HU-B02.2

## História

Como **pessoa investigando um erro**, quero **seguir uma requisição por todos os serviços e filas** para **achar onde ela falhou**.

## Requisitos funcionais

- RF01 — O gateway repassa `X-Usuario-Id` e `X-Papel` (do context do authorizer) e gera `X-Correlation-Id` quando o cliente não envia.
- RF02 — O `wc-commons` coloca o `correlationId` no MDC, devolve no header da resposta e o copia nos atributos de toda mensagem SQS/SNS publicada.
- RF03 — Listeners leem o atributo e restauram o MDC antes de processar.

## Regras e requisitos não funcionais

- O serviço **não confia** em `X-Usuario-Id` sozinho: o filtro JWT continua sendo a fonte da identidade; o header serve só para log.

## Critérios de aceite

```gherkin
Cenário: correlationId atravessa a venda até a nota
  Dado uma venda NFC-e finalizada com X-Correlation-Id "abc-123"
  Então os logs do sales-service, fiscal-service (3 listeners) e product-service contêm correlationId "abc-123"

Cenário: geração automática
  Quando chamo a API sem X-Correlation-Id
  Então a resposta traz um X-Correlation-Id gerado
```

## Subtasks

- [ ] ST-B02.4.1 Mapeamento de headers na integração
- [ ] ST-B02.4.2 Filtro de MDC e header de resposta no `wc-commons`
- [ ] ST-B02.4.3 Propagação nos atributos de mensagem

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B02.4.1 | Unitário | filtro gera e devolve o header | geração |
| TC-B02.4.2 | Integração | publica mensagem e confere o atributo | RF02 |
| TC-B02.4.3 | E2E (EPB-17) | busca o id nos logs dos serviços | atravessa |

# HU-B09.1 — Simulador da SEFAZ (WireMock)

**Épico:** [EPB-09](README.md) · **Unidade:** container `sefaz-mock` · **Estimativa:** 5 pts · **Depende de:** HU-B00.1

## História

Como **desenvolvedora e banca do TCC**, quero **uma SEFAZ falsa que responde como a real** para **demonstrar e testar o fluxo fiscal sem certificado nem internet**.

## Requisitos funcionais

- RF01 — Container WireMock em `infra/local/sefaz-mock` com mapeamentos SOAP 1.2 para `NFeAutorizacao4`, `NFeRetAutorizacao4`, `NFeRecepcaoEvento4`, `NFeInutilizacao4` e `NFeStatusServico4`.
- RF02 — Respostas escolhidas por regra no conteúdo do XML enviado (response templating):
  - padrão → `cStat 100` com `nProt` e `dhRecbto` gerados;
  - valor total terminado em `,13` → `cStat 103` (lote recebido) e a consulta do recibo devolve `100`;
  - valor total terminado em `,04` → `cStat 204` (duplicidade) ou outro código `2xx` documentado na tabela do README do mock;
  - valor total terminado em `,99` → atraso de 70 s (timeout);
  - valor total terminado em `,50` → HTTP 500.
  - evento `110111` → `cStat 135`; inutilização → `cStat 102`.
- RF03 — Endpoint `/__admin` exposto só na rede interna para os testes trocarem cenários dinamicamente.
- RF04 — README do mock com a tabela de gatilhos para usar na apresentação.

## Regras e requisitos não funcionais

- O mock **não** valida assinatura (isso é coberto nos testes da HU-B08.3), mas confere que o XML tem `Signature` e devolve `cStat 297` se não tiver.

## Critérios de aceite

```gherkin
Cenário: autorização padrão
  Quando envio um lote com uma NF-e de total 179,80
  Então o mock responde cStat 100 com nProt

Cenário: gatilho de rejeição
  Quando envio uma nota com total 10,04
  Então o mock responde cStat 204

Cenário: sem assinatura
  Quando envio XML sem Signature
  Então o mock responde cStat 297
```

## Subtasks

- [ ] ST-B09.1.1 Mapeamentos WireMock por serviço
- [ ] ST-B09.1.2 Templates de resposta com `nProt`, `dhRecbto`, `chNFe` ecoando a chave
- [ ] ST-B09.1.3 Regras por sufixo de valor
- [ ] ST-B09.1.4 Entrada no Compose e README de gatilhos

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B09.1.1 | Integração (Testcontainers WireMock) | cada gatilho devolve o cStat esperado | todos |
| TC-B09.1.2 | Integração | XML sem assinatura → 297 | sem assinatura |

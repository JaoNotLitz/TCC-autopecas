# HU-B12.1 — Processar XML e registrar entrada

**Épico:** [EPB-12](README.md) · **Unidade:** `XmlProcessorListener` · **Estimativa:** 5 pts · **Depende de:** HU-B11.3

## História

Como **estoquista**, quero **que o XML importado vire uma entrada registrada sozinho** para **economizar digitação**.

## Requisitos funcionais

- RF01 — Listener (3 concorrentes, visibility 60 s) lê `{entryId, s3Key}` e baixa o XML.
- RF02 — Usa o `NfeEntradaParser` (HU-B11.2) e valida os dados extraídos (mesmas regras da entrada manual).
- RF03 — Vincula itens por código no product-service. Todos vinculados → `status = registrada`; algum sem produto → `status = com_pendencias`, item com `produtoId = null` e `vinculado = false` (decisão A5).
- RF04 — `dataEntrada` = data do processamento (a arquitetura diz "configurada pelo usuário"; na importação direta não há tela, então usa a data do dia).

## Critérios de aceite

```gherkin
Cenário: XML com todos os itens cadastrados
  Quando o XML válido do seed é processado
  Então a entrada fica "registrada" com todos os itens vinculados

Cenário: item desconhecido
  Quando o XML com código inexistente é processado
  Então a entrada fica "com_pendencias" e o item aparece com vinculado false
```

## Subtasks

- [ ] ST-B12.1.1 Listener e download do S3
- [ ] ST-B12.1.2 Vinculação e cálculo do status

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B12.1.1 | Integração (LocalStack + WireMock) | XML válido | cadastrados |
| TC-B12.1.2 | Integração | XML com item desconhecido | desconhecido |

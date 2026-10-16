# HU-B08.2 — Calcular chave de acesso

**Épico:** [EPB-08](README.md) · **Unidade:** `FiscalProcessorListener` · **Estimativa:** 2 pts · **Depende de:** HU-B08.1

## História

Como **sistema fiscal**, quero **gerar a chave de 44 dígitos com o dígito verificador correto** para **identificar a nota de forma única na SEFAZ**.

## Requisitos funcionais

- RF01 — Chave = `cUF(2) + AAMM(4) + CNPJ(14) + mod(2) + serie(3) + nNF(9) + tpEmis(1) + cNF(8) + cDV(1)`.
- RF02 — `cNF` aleatório de 8 dígitos, diferente de `nNF`.
- RF03 — `cDV` por módulo 11 com pesos 2 a 9 da direita para a esquerda (resto 0 ou 1 → DV 0).
- RF04 — Grava `chaveAcesso` na nota e usa `Id="NFe{chave}"` em `infNFe`.

## Critérios de aceite

```gherkin
Cenário: DV conhecido
  Dado os 43 primeiros dígitos de uma chave de exemplo do Manual de Orientação do Contribuinte
  Então o DV calculado é igual ao do manual

Cenário: chave válida no validador
  Então a chave gerada passa no validador @ChaveAcesso do wc-commons
```

## Subtasks

- [ ] ST-B08.2.1 `ChaveAcessoGenerator`

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B08.2.1 | Unitário (parametrizado) | 5 chaves reais públicas de exemplo | DV |
| TC-B08.2.2 | Unitário (property-based) | 1.000 chaves geradas passam no validador | válida |

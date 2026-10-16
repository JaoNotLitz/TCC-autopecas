# HU-B00.4 — Dados de seed e certificado A1 de teste

**Épico:** [EPB-00](README.md) · **Unidade:** `infra/local/seed` · **Estimativa:** 3 pts · **Depende de:** HU-B00.2

## História

Como **desenvolvedora ou banca do TCC**, quero **dados de exemplo já carregados** para **testar telas e fluxos logo após subir a stack**.

## Requisitos funcionais

- RF01 — `make seed` carrega: usuário `admin@wcautopecas.com.br` (papel `ADMIN`, senha definida em `.env`, gravada com BCrypt), usuário `vendedor@wcautopecas.com.br` (papel `VENDEDOR`).
- RF02 — 20 produtos de autopeças com NCM, CFOP e dados fiscais válidos, e registros em `inventory` cobrindo os quatro status (`ok`, `atencao`, `baixo`, `critico`).
- RF03 — 10 clientes (PF e PJ, com CPF/CNPJ válidos), sendo pelo menos 2 sem endereço (para testar o `422` da NF-e).
- RF04 — Certificado A1 **autoassinado de teste** (`.pfx` + senha) gerado por script com OpenSSL e gravado em `wc-sefaz-certificate` (Base64 + senha). Também gera uma versão **expirada** em `wc-sefaz-certificate-expirado` para testes de erro.
- RF05 — Dados do emitente (CNPJ, IE, razão social, endereço, CRT) em `infra/local/seed/emitente.json`, carregados como configuração do fiscal-service.
- RF06 — 3 XMLs de NF-e de fornecedor de exemplo em `infra/local/seed/xml/` (um válido, um com código de produto inexistente, um malformado).

## Regras e requisitos não funcionais

- Seed idempotente (usa `PutItem` com IDs fixos).
- Nenhum dado pessoal real; CPFs/CNPJs gerados por algoritmo.

## Critérios de aceite

```gherkin
Cenário: login com usuário do seed
  Dado a stack no ar e "make seed" executado
  Quando faço POST /api/auth/login com o admin do seed
  Então recebo 200 com token

Cenário: estoque cobre todos os status
  Quando consulto GET /api/produtos?status=critico
  Então recebo pelo menos 1 produto

Cenário: certificado disponível
  Quando leio o secret "wc-sefaz-certificate"
  Então consigo abrir o .pfx com a senha e ele está dentro da validade
```

## Subtasks

- [ ] ST-B00.4.1 JSONs de seed (usuários, produtos, inventário, clientes)
- [ ] ST-B00.4.2 Script `seed.sh` com `awslocal dynamodb batch-write-item`
- [ ] ST-B00.4.3 Script de geração do certificado A1 de teste (válido e expirado)
- [ ] ST-B00.4.4 `emitente.json`
- [ ] ST-B00.4.5 XMLs de exemplo de fornecedor

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B00.4.1 | Smoke | contagem de itens por tabela após seed | seed |
| TC-B00.4.2 | Integração | carrega o `.pfx` do secret com `KeyStore.getInstance("PKCS12")` | certificado |
| TC-B00.4.3 | Smoke | rodar `make seed` duas vezes mantém as mesmas contagens | idempotência |

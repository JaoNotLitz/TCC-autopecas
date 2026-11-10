# HU-B00.6 — Pipeline de CI com Testcontainers

**Épico:** [EPB-00](README.md) · **Unidade:** `.github/workflows/backend.yml` · **Estimativa:** 3 pts · **Depende de:** HU-B00.3

## História

Como **desenvolvedora**, quero **que cada PR rode build e testes automaticamente** para **não quebrar a `main`**.

## Requisitos funcionais

- RF01 — Workflow disparado em `pull_request` e `push` na `main`, filtrando por `backend/**` e `infra/**`.
- RF02 — Etapas: `mvn -B verify` (unitários + integração com Testcontainers LocalStack/Redis), relatório JaCoCo, build das imagens Docker.
- RF03 — Falha se cobertura de domínio/serviço < 80%.
- RF04 — Job de smoke que sobe o Compose, roda `verify.sh` (HU-B00.2) e derruba.
- RF05 — Cache do repositório Maven.

## Critérios de aceite

```gherkin
Cenário: PR com teste quebrado
  Dado um PR em que um teste unitário falha
  Então o check "backend" fica vermelho e mostra o teste que falhou

Cenário: PR verde
  Dado um PR com testes e cobertura ok
  Então os jobs build, testes e smoke ficam verdes
```

## Subtasks

- [ ] ST-B00.6.1 Workflow com matriz por módulo
- [ ] ST-B00.6.2 JaCoCo com limite de cobertura
- [ ] ST-B00.6.3 Job de smoke do Compose
- [ ] ST-B00.6.4 Badge no README

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B00.6.1 | CI | PR de teste com falha proposital fica vermelho | teste quebrado |
| TC-B00.6.2 | CI | PR limpo fica verde | PR verde |

# HU-B16.3 — Logs estruturados e métricas

**Épico:** [EPB-16](README.md) · **Unidade:** todos os serviços (`wc-commons`) · **Estimativa:** 3 pts · **Depende de:** HU-B02.4

## História

Como **desenvolvedora**, quero **logs e métricas padronizados em todos os serviços** para **diagnosticar problemas e mostrar números na apresentação do TCC**.

## Requisitos funcionais

- RF01 — Todo log em JSON com `timestamp`, `level`, `service`, `correlationId`, `usuarioId` (quando houver), `message`, `exception`.
- RF02 — Métricas Micrometer expostas em `/actuator/prometheus`: tempo de processamento por listener, mensagens processadas/falhas por fila, notas por status, chamadas SEFAZ por `cStat`.
- RF03 — (Opcional) Perfil `observabilidade` no Compose com Prometheus + Grafana e um dashboard pronto.

## Critérios de aceite

```gherkin
Cenário: métrica de autorização
  Dado 5 notas autorizadas e 1 rejeitada
  Então /actuator/prometheus do fiscal-service mostra sefaz_retorno_total{cstat="100"} 5 e {cstat="204"} 1
```

## Subtasks

- [ ] ST-B16.3.1 Encoder JSON padronizado
- [ ] ST-B16.3.2 Métricas customizadas nos listeners
- [ ] ST-B16.3.3 Perfil Prometheus/Grafana

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B16.3.1 | Unitário | layout do log tem os campos | RF01 |
| TC-B16.3.2 | Integração | contadores após processamento | métrica |

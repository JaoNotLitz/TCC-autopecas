# HU-B09.6 — Ambiente e endpoints configuráveis

**Épico:** [EPB-09](README.md) · **Unidade:** `fiscal-service` · **Estimativa:** 2 pts · **Depende de:** HU-B09.2

## História

Como **desenvolvedora**, quero **trocar entre o simulador e a SEFAZ de homologação por configuração** para **poder testar com a SEFAZ real no futuro sem mudar código**.

## Requisitos funcionais

- RF01 — Propriedades `sefaz.ambiente` (`homologacao`|`producao`), `sefaz.uf` e um mapa de URLs por serviço; perfil `local` aponta todas para `http://sefaz-mock:8080`.
- RF02 — Na subida, chama `NFeStatusServico4`; se não responder `107`, loga aviso e o health indicator `sefaz` fica `DOWN` (sem derrubar o serviço).
- RF03 — `producao` só é aceito se a variável `SEFAZ_PRODUCAO_CONFIRMADO=true` existir (proteção contra emissão real acidental).

## Critérios de aceite

```gherkin
Cenário: local usa o mock
  Dado o perfil local
  Então todas as chamadas SOAP vão para sefaz-mock

Cenário: produção sem confirmação
  Dado sefaz.ambiente=producao sem SEFAZ_PRODUCAO_CONFIRMADO
  Então o serviço não sobe e loga o motivo
```

## Subtasks

- [ ] ST-B09.6.1 `SefazProperties` e mapa de URLs
- [ ] ST-B09.6.2 Health indicator de status do serviço
- [ ] ST-B09.6.3 Trava de produção

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B09.6.1 | Unitário | resolução de URL por perfil | local |
| TC-B09.6.2 | Unitário | trava de produção | produção |
| TC-B09.6.3 | Integração | health `sefaz` com mock parado | RF02 |

# HU-F00.2 — Interceptors de autenticação e erro

**Épico:** [EPF-00](README.md) · **Estimativa:** 5 pts · **Depende de:** HU-F00.1, HU-B01.4

## História

Como **usuário**, quero **que o sistema renove meu acesso sozinho e me mostre mensagens de erro claras** para **não perder o que estou fazendo nem ver erros técnicos**.

## Requisitos funcionais

- RF01 — Migrar `services/http.ts` para `HttpClient` com interceptors funcionais (`withInterceptors`).
- RF02 — `authInterceptor`: adiciona `Authorization: Bearer` (token de `sessionStorage['wc_token']`) e `X-Correlation-Id` (UUID por requisição); não adiciona em `/auth/login` e `/auth/refresh`.
- RF03 — `refreshInterceptor`: em `401` de rota protegida, chama `POST /auth/refresh` **uma vez** (requisições simultâneas aguardam o mesmo refresh), repete a original; se o refresh falhar, limpa a sessão e navega para `/login?expirada=1`.
- RF04 — `errorInterceptor`: converte a resposta no `ApiError {status, erro, message, campos[]}` da seção 5.0; status 0 vira "Sem conexão com o servidor"; `403` vira "Você não tem permissão para esta operação"; `5xx` mostra toast genérico com o `correlationId` para suporte.
- RF05 — Refresh token guardado em `sessionStorage['wc_refresh']` (mesma política do token: nunca `localStorage`).
- RF06 — Helper `aplicarErrosNoForm(form, apiError)` que marca `campos[]` nos controles do Reactive Form (aceita caminhos como `itens[0].quantidade` e `nota.dataEntrada`).

## Critérios de aceite

```gherkin
Cenário: token expirado durante o uso
  Dado um access token expirado e um refresh válido
  Quando a tela de clientes carrega
  Então a lista aparece sem o usuário perceber e os tokens novos ficam no sessionStorage

Cenário: refresh também expirado
  Então o usuário vai para /login com a mensagem "Sua sessão expirou"

Cenário: erro de validação vira erro no campo
  Dado um 409 com campos[{campo: "documento"}]
  Então o campo Documento do formulário mostra a mensagem do back

Cenário: duas chamadas com 401 ao mesmo tempo
  Então só uma chamada de refresh é feita
```

## Subtasks

- [ ] ST-F00.2.1 `authInterceptor`
- [ ] ST-F00.2.2 `refreshInterceptor` com fila de espera
- [ ] ST-F00.2.3 `errorInterceptor` e `ApiError`
- [ ] ST-F00.2.4 `aplicarErrosNoForm`
- [ ] ST-F00.2.5 Migrar serviços para `HttpClient`

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-F00.2.1 | Unitário (`HttpTestingController`) | header presente e ausente nas rotas públicas | RF02 |
| TC-F00.2.2 | Unitário | 401 → refresh → repete | token expirado |
| TC-F00.2.3 | Unitário | duas 401 simultâneas → 1 refresh | simultâneas |
| TC-F00.2.4 | Unitário | refresh falha → logout e navegação | refresh expirado |
| TC-F00.2.5 | Unitário | `aplicarErrosNoForm` com caminhos aninhados | erro no campo |

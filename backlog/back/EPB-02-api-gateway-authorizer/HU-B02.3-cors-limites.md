# HU-B02.3 — CORS, limite de payload e throttling

**Épico:** [EPB-02](README.md) · **Unidade:** API Gateway · **Estimativa:** 2 pts · **Depende de:** HU-B02.2

## História

Como **front-end rodando em `localhost:5173`**, quero **que o navegador aceite as respostas da API** para **as chamadas não serem bloqueadas por CORS**.

## Requisitos funcionais

- RF01 — CORS liberado para a origem configurada (`http://localhost:5173` no local): métodos `GET, POST, PUT, DELETE, OPTIONS`, headers `Authorization, Content-Type, X-Correlation-Id`, expõe `Content-Disposition`.
- RF02 — `OPTIONS` responde sem passar pelo authorizer.
- RF03 — Usage plan com throttling 1000 req/s e burst 100 (configurado mesmo que o LocalStack não aplique; documentar).
- RF04 — Serviços rejeitam corpo acima de 6 MB com `413` no formato padrão (a importação de XML tem limite próprio de 5 MB).

## Critérios de aceite

```gherkin
Cenário: preflight
  Quando o navegador envia OPTIONS /api/clientes com Origin http://localhost:5173
  Então recebo 200 com Access-Control-Allow-Origin igual à origem

Cenário: origem não permitida
  Quando envio Origin http://evil.local
  Então a resposta não traz Access-Control-Allow-Origin
```

## Subtasks

- [ ] ST-B02.3.1 Métodos `OPTIONS` mock em cada recurso
- [ ] ST-B02.3.2 Headers CORS nas Gateway Responses (inclusive 401)
- [ ] ST-B02.3.3 Usage plan

## Testes

| ID | Tipo | Cenário | Critério |
|---|---|---|---|
| TC-B02.3.1 | API | preflight → headers corretos | preflight |
| TC-B02.3.2 | API | origem estranha → sem header | não permitida |
| TC-B02.3.3 | E2E (Playwright) | front React (`npm run dev`, porta 5173 do Vite) faz login sem erro de CORS | RF01 |

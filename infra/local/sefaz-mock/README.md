# Simulador da SEFAZ (WireMock)

Responde aos web services SOAP 1.2 da NF-e 4.00 no lugar da SEFAZ de homologação, para o
backend rodar 100% local. Sobe junto com o Compose (`wc-sefaz-mock`, porta 8089 no host) e é
o mesmo usado pelos testes de integração do fiscal-service (Testcontainers).

O fiscal-service chama `${SEFAZ_URL}/ws/{Servico}` (padrão `http://sefaz-mock:8080`).

## Cenários

O cenário é escolhido pelo **centavo do valor total da nota** (`<vNF>`), assim dá para
provocar cada caso pela tela de vendas, sem mexer no código.

| Serviço | Gatilho | Resposta |
|---|---|---|
| NFeAutorizacao4 | XML sem `<Signature>` | protNFe 297, "Assinatura difere do padrão" |
| NFeAutorizacao4 | total terminando em **,50** | HTTP 500 (SEFAZ fora do ar: retry, depois DLQ e alerta high) |
| NFeAutorizacao4 | total terminando em **,99** | demora 70 s (estoura o timeout de 60 s) |
| NFeAutorizacao4 | total terminando em **,04** | protNFe 204, "Duplicidade de NF-e" (nota rejeitada) |
| NFeAutorizacao4 | total terminando em **,13** | lote 103 com recibo; a consulta do recibo autoriza |
| NFeAutorizacao4 | qualquer outro | lote 104 + protNFe 100 com protocolo (nota autorizada) |
| NFeRetAutorizacao4 | qualquer | protNFe 100 |
| NFeRecepcaoEvento4 | sem `<Signature>` | 297 |
| NFeRecepcaoEvento4 | justificativa com **"rejeitar"** | 218, "NF-e já está cancelada" (nota volta para autorizada) |
| NFeRecepcaoEvento4 | qualquer outro | 128 + 135 (cancelamento homologado) |
| NFeInutilizacao4 | sem `<Signature>` | 297 |
| NFeInutilizacao4 | justificativa com **"rejeitar"** | 256, "parte da faixa já está utilizada" |
| NFeInutilizacao4 | qualquer outro | 102 (inutilização homologada) |
| NFeStatusServico4 | qualquer | 107 (serviço em operação) |

A chave de acesso (`chNFe`) da resposta é copiada do `Id="NFe..."` do XML enviado.

## Testando na mão

```bash
curl -s localhost:8089/__admin/mappings | jq '.mappings[].name'
curl -s -X POST localhost:8089/ws/NFeStatusServico4 -H 'Content-Type: application/soap+xml' -d '<x/>'
curl -s localhost:8089/__admin/requests | jq '.requests[0].request.url'   # últimas chamadas recebidas
```

Novos cenários: um arquivo JSON em `mappings/` (formato de stub do WireMock 3) e
`docker compose restart sefaz-mock`.

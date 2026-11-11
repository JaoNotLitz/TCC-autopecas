package br.com.wcautopecas.e2e;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Controle do sefaz-mock pelo admin do WireMock (ST-B17.4.1). Os stubs criados aqui têm prioridade 1,
 * acima dos cenários fixos de {@code infra/local/sefaz-mock/mappings} (prioridade 2 em diante), e são
 * removidos no fim de cada teste; nada é gravado nos arquivos de mapeamento.
 */
public final class SefazMock implements AutoCloseable {

    private final String stubId;

    private SefazMock(String stubId) {
        this.stubId = stubId;
    }

    /** NFeAutorizacao4 responde HTTP 500 para qualquer nota (SEFAZ fora do ar). */
    public static SefazMock foraDoAr() {
        return criar(Map.of("status", 500,
                "headers", Map.of("Content-Type", "application/soap+xml; charset=utf-8"),
                "body", "<erro>SEFAZ indisponivel (teste e2e)</erro>"));
    }

    /** NFeAutorizacao4 responde 2xx com a rejeição 204 (o mesmo corpo do cenário ",04"). */
    public static SefazMock rejeitando() {
        String corpo;
        try (var in = SefazMock.class.getResourceAsStream("/sefaz/rejeicao-204.xml")) {
            corpo = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        return criar(Map.of("status", 200,
                "headers", Map.of("Content-Type", "application/soap+xml; charset=utf-8"),
                "transformers", java.util.List.of("response-template"),
                "body", corpo));
    }

    private static SefazMock criar(Map<String, Object> resposta) {
        Map<String, Object> stub = Map.of(
                "name", "e2e-temporario",
                "priority", 1,
                "request", Map.of("method", "POST", "urlPath", "/ws/NFeAutorizacao4"),
                "response", resposta);
        String id = given().baseUri(Stack.SEFAZ_MOCK).contentType(ContentType.JSON).body(stub)
                .post("/__admin/mappings").then().statusCode(201).extract().path("id");
        return new SefazMock(id);
    }

    /** Volta o simulador ao normal. */
    @Override
    public void close() {
        given().baseUri(Stack.SEFAZ_MOCK).delete("/__admin/mappings/{id}", stubId);
    }
}

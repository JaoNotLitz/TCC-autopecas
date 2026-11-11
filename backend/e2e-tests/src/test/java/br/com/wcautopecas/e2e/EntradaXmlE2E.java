package br.com.wcautopecas.e2e;

import static br.com.wcautopecas.e2e.Stack.aguardarEstoque;
import static br.com.wcautopecas.e2e.Stack.api;
import static br.com.wcautopecas.e2e.Stack.estoque;
import static br.com.wcautopecas.e2e.Stack.json;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-B17.3: o XML do fornecedor vira estoque (prévia + confirmação, como na tela de entrada). */
@DisplayName("HU-B17.3 Entrada por XML até o estoque")
class EntradaXmlE2E {

    private static final long PF_001 = 1;
    /** A chave do XML de exemplo; o teste troca o número para cada execução ter uma nota nova. */
    private static final String CHAVE_EXEMPLO = "35260911222333000181550010000123451123456784";

    @Test
    @DisplayName("TC-B17.3.1 Prévia do XML e confirmação somam o estoque e criam a nota ENTRADA")
    void previaEConfirmacao() throws Exception {
        long numero = 100_000 + (System.currentTimeMillis() / 1000) % 900_000_000L;
        String chave = chave(numero);
        String xml = xml(numero, chave);
        long antes = estoque(PF_001);

        // Prévia: devolve o EntradaInput preenchido, com os itens já ligados ao catálogo pelo código.
        JsonNode previa = json(enviar("/notas/entrada/importar-xml", xml).then().statusCode(200).extract()
                .asString());
        assertThat(previa.path("chaveAcesso").asText()).isEqualTo(chave);
        assertThat(previa.path("numero").asText()).isEqualTo(String.valueOf(numero));
        assertThat(previa.path("fornecedor").path("cnpj").asText()).isEqualTo("11222333000181");
        JsonNode pf001 = item(previa, "PF-001");
        assertThat(pf001.path("produtoId").asLong()).isEqualTo(PF_001);
        assertThat(pf001.path("quantidade").asLong()).isEqualTo(100);
        assertThat(item(previa, "XYZ-999").path("produtoId").isNull()).as("item fora do catálogo vem sem produto")
                .isTrue();

        // Confirmação: o usuário deixa só os itens ligados ao catálogo (o front pede o vínculo dos outros).
        ObjectNode confirmacao = (ObjectNode) previa.deepCopy();
        ArrayNode itens = confirmacao.putArray("itens");
        previa.path("itens").forEach(i -> {
            if (!i.path("produtoId").isNull()) {
                itens.add(i);
            }
        });
        JsonNode nota = json(api().body(Stack.JSON.writeValueAsString(confirmacao)).post("/notas/entrada").then()
                .statusCode(201).extract().asString());
        assertThat(nota.path("tipo").asText()).isEqualTo("ENTRADA");
        assertThat(nota.path("chaveAcesso").asText()).isEqualTo(chave);

        aguardarEstoque(PF_001, antes + 100);
        JsonNode entradas = json(api().queryParam("tipo", "ENTRADA").get("/notas").then().statusCode(200).extract()
                .asString());
        assertThat(Stack.algum(entradas, n -> chave.equals(n.path("chaveAcesso").asText()))).isTrue();

        // A mesma nota de novo é recusada pela chave.
        api().body(Stack.JSON.writeValueAsString(confirmacao)).post("/notas/entrada").then().statusCode(409);
    }

    /**
     * Corpo multipart montado à mão, com Content-Length: o multipart do RestAssured vai em chunked e o
     * proxy do API Gateway do LocalStack não repassa esse corpo (o navegador manda com tamanho).
     */
    private static io.restassured.response.Response enviar(String caminho, String xml) {
        String limite = "wc-e2e-" + System.nanoTime();
        return Stack.semCorpo().contentType("multipart/form-data; boundary=" + limite)
                .body(multipart(limite, "arquivo", "nfe.xml", xml)).post(caminho);
    }

    private static byte[] multipart(String limite, String campo, String arquivo, String conteudo) {
        String corpo = "--" + limite + "\r\n"
                + "Content-Disposition: form-data; name=\"" + campo + "\"; filename=\"" + arquivo + "\"\r\n"
                + "Content-Type: text/xml\r\n\r\n"
                + conteudo + "\r\n--" + limite + "--\r\n";
        return corpo.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("TC-B17.3.2 Importação assíncrona com item desconhecido: com_pendencias e estoque só do vinculado")
    void importacaoComPendencias() throws Exception {
        long numero = 200_000_000L + (System.currentTimeMillis() / 1000) % 100_000_000L;
        String chave = chave(numero);
        long antes = estoque(PF_001);

        JsonNode aceito = json(enviar("/notas/entrada/importar-xml/lote", xml(numero, chave)).then().statusCode(202)
                .extract().asString());
        long id = aceito.path("uploadId").asLong();
        assertThat(aceito.path("s3Key").asText()).endsWith(chave + ".xml");

        JsonNode entrada = aguardarEntrada(id, "com_pendencias");
        assertThat(item(entrada, "PF-001").path("vinculado").asBoolean()).isTrue();
        assertThat(item(entrada, "XYZ-999").path("vinculado").asBoolean()).isFalse();
        assertThat(item(entrada, "XYZ-999").path("produtoId").isNull()).isTrue();
        aguardarEstoque(PF_001, antes + 100);
    }

    @Test
    @DisplayName("TC-B17.3.3 Importação assíncrona de XML malformado: entrada em erro e mensagem na DLQ")
    void importacaoMalformada() {
        String quebrado = "<?xml version=\"1.0\"?><nfeProc xmlns=\"http://www.portalfiscal.inf.br/nfe\"><NFe>";
        long id = json(enviar("/notas/entrada/importar-xml/lote", quebrado).then().statusCode(202).extract()
                .asString()).path("uploadId").asLong();

        JsonNode entrada = aguardarEntrada(id, "erro");
        assertThat(entrada.path("erro").asText()).isEqualTo("XML inválido ou não é NF-e");

        JsonNode dlq = Stack.lambda("wc-dlq-redrive", java.util.Map.of("origem", "entry-events-queue", "max", 50,
                "dryRun", true));
        assertThat(Stack.algum(dlq.path("mensagens"), m -> m.path("corpo").asText()
                .matches("(?s).*\"entryId\"\\s*:\\s*" + id + "\\b.*"))).isTrue();
    }

    private static JsonNode aguardarEntrada(long id, String status) {
        java.util.concurrent.atomic.AtomicReference<JsonNode> ultima = new java.util.concurrent.atomic.AtomicReference<>();
        org.awaitility.Awaitility.await("entrada " + id + " " + status).atMost(Stack.PRAZO).until(() -> {
            ultima.set(json(api().get("/notas/entrada/{id}", id).then().statusCode(200).extract().asString()));
            return status.equals(ultima.get().path("status").asText());
        });
        return ultima.get();
    }

    private static JsonNode item(JsonNode entrada, String codigo) {
        for (JsonNode i : entrada.path("itens")) {
            if (codigo.equals(i.path("codigo").asText())) {
                return i;
            }
        }
        throw new AssertionError("item " + codigo + " não veio na prévia: " + entrada);
    }

    /** O XML de exemplo com número, chave e data de emissão desta execução. */
    private static String xml(long numero, String chave) throws Exception {
        String xml;
        try (var in = EntradaXmlE2E.class.getResourceAsStream("/xml/nfe-fornecedor.xml")) {
            xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        return xml.replace(CHAVE_EXEMPLO, chave)
                .replace("<nNF>12345</nNF>", "<nNF>" + numero + "</nNF>")
                .replaceAll("<dhEmi>[^<]*</dhEmi>", "<dhEmi>" + LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo")) + "T08:00:00-03:00</dhEmi>");
    }

    /** Chave do exemplo com outro nNF (posições 26 a 34) e o dígito verificador recalculado. */
    static String chave(long numero) {
        String base = CHAVE_EXEMPLO.substring(0, 25) + String.format("%09d", numero) + CHAVE_EXEMPLO.substring(34, 43);
        int soma = 0;
        int peso = 2;
        for (int i = base.length() - 1; i >= 0; i--) {
            soma += (base.charAt(i) - '0') * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }
        int resto = soma % 11;
        return base + (resto < 2 ? 0 : 11 - resto);
    }
}

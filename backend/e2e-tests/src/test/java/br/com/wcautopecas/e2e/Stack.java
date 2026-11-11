package br.com.wcautopecas.e2e;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;

/**
 * Acesso à stack do Compose já no ar ({@code make up}). Tudo de negócio passa pelo API Gateway do
 * LocalStack, como o front faz; só o que o front não enxerga (reservado do estoque, e-mails do SES,
 * Lambdas de operação e o admin do sefaz-mock) é lido direto dos containers.
 */
public final class Stack {

    public static final String GATEWAY = env("GATEWAY_URL",
            "http://localhost:4566/restapis/wcapi/local/_user_request_/api");
    public static final String LOCALSTACK = env("LOCALSTACK_URL", "http://localhost:4566");
    public static final String SEFAZ_MOCK = env("SEFAZ_MOCK_URL", "http://localhost:8089");
    public static final String PRODUCT = env("PRODUCT_SERVICE_URL", "http://localhost:8082");
    public static final String OPS = env("OPS_DESTINATARIOS", "ops@wcautopecas.local");

    /** Prazo do fluxo principal (RF02 da HU-B17.1). */
    public static final Duration PRAZO = Duration.ofSeconds(60);

    static final ObjectMapper JSON = new ObjectMapper();
    private static String token;

    static {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    private Stack() {
    }

    static String env(String nome, String padrao) {
        String v = System.getenv(nome);
        return v == null || v.isBlank() ? padrao : v;
    }

    /** Login do admin do seed pelo gateway; o token vale para a execução inteira. */
    public static synchronized String token() {
        if (token == null) {
            Response r = given().baseUri(GATEWAY).contentType(ContentType.JSON)
                    .body(Map.of("email", "admin@wcautopecas.com.br", "senha", env("SEED_ADMIN_SENHA", "admin123")))
                    .post("/auth/login");
            assertThat(r.statusCode()).as("login pelo gateway (a stack está no ar? make up)").isEqualTo(200);
            token = r.path("token");
        }
        return token;
    }

    public static RequestSpecification api() {
        return semCorpo().contentType(ContentType.JSON);
    }

    /** Sem Content-Type fixo (upload multipart). */
    public static RequestSpecification semCorpo() {
        return given().baseUri(GATEWAY).header("Authorization", "Bearer " + token());
    }

    // ---------- produtos ----------

    public static long estoque(long produtoId) {
        return api().get("/produtos/{id}", produtoId).then().statusCode(200).extract().<Number>path("estoqueAtual")
                .longValue();
    }

    /** O reservado não aparece para o front; vem do endpoint interno do product-service. */
    public static long reservado(long produtoId) {
        return given().baseUri(PRODUCT).header("Authorization", "Bearer " + token()).get("/internal/produtos/{id}", produtoId).then().statusCode(200).extract()
                .<Number>path("reservado").longValue();
    }

    public static BigDecimal preco(long produtoId) {
        return new BigDecimal(api().get("/produtos/{id}", produtoId).then().statusCode(200).extract()
                .path("precoVenda").toString());
    }

    /** Repõe o estoque pelo ajuste (assíncrono) quando ficou baixo de execuções anteriores. */
    public static void garantirEstoque(long produtoId, long minimo) {
        if (estoque(produtoId) >= minimo) {
            return;
        }
        long novo = minimo + 50;
        api().body(Map.of("quantidadeNova", novo, "motivo", "Reposição para os testes ponta a ponta"))
                .post("/produtos/{id}/ajuste", produtoId).then().statusCode(202);
        await("ajuste do produto " + produtoId).atMost(PRAZO).until(() -> estoque(produtoId) == novo);
    }

    /** Espera o estoque chegar ao valor esperado (as baixas e entradas são assíncronas). */
    public static void aguardarEstoque(long produtoId, long esperado) {
        await("estoque do produto " + produtoId + " = " + esperado).atMost(PRAZO)
                .until(() -> estoque(produtoId) == esperado);
    }

    // ---------- vendas e notas ----------

    /** Venda finalizada de um produto, no formato do PDV do front. */
    public static long venda(String tipoDocumento, Long clienteId, long produtoId, long quantidade,
            BigDecimal valorUnitario) {
        return criarVenda(tipoDocumento, clienteId, produtoId, quantidade, valorUnitario).then().statusCode(201)
                .extract().<Number>path("id").longValue();
    }

    public static Response criarVenda(String tipoDocumento, Long clienteId, long produtoId, long quantidade,
            BigDecimal valorUnitario) {
        Map<String, Object> corpo = new java.util.HashMap<>();
        corpo.put("tipoDocumento", tipoDocumento);
        corpo.put("modalidade", "fisica");
        corpo.put("clienteId", clienteId);
        corpo.put("metodoPagamento", "pix");
        corpo.put("bandeira", null);
        corpo.put("destinoDinheiro", null);
        corpo.put("desconto", 0);
        corpo.put("status", "finalizada");
        corpo.put("itens", List.of(Map.of("produtoId", produtoId, "quantidade", quantidade,
                "valorUnitario", valorUnitario)));
        return api().body(corpo).post("/vendas");
    }

    /** A nota da venda pelo mesmo caminho que o PDV usa: {@code GET /api/notas?vendaId=}. */
    public static JsonNode notaDaVenda(long vendaId) {
        JsonNode lista = json(api().queryParam("vendaId", vendaId).get("/notas").then().statusCode(200).extract()
                .asString());
        return lista.isEmpty() ? null : lista.get(0);
    }

    public static JsonNode nota(long id) {
        return json(api().get("/notas/{id}", id).then().statusCode(200).extract().asString());
    }

    /** Espera a nota da venda existir e chegar ao status; devolve a nota. */
    public static JsonNode aguardarNotaDaVenda(long vendaId, String status, Duration prazo) {
        AtomicReference<JsonNode> ultima = new AtomicReference<>();
        await("nota da venda " + vendaId + " " + status).atMost(prazo).pollInterval(Duration.ofSeconds(1))
                .until(() -> {
                    ultima.set(notaDaVenda(vendaId));
                    return ultima.get() != null && status.equals(ultima.get().path("status").asText());
                });
        return ultima.get();
    }

    public static JsonNode aguardarNota(long notaId, String status) {
        AtomicReference<JsonNode> ultima = new AtomicReference<>();
        await("nota " + notaId + " " + status).atMost(PRAZO).pollInterval(Duration.ofSeconds(1)).until(() -> {
            ultima.set(nota(notaId));
            return status.equals(ultima.get().path("status").asText());
        });
        return ultima.get();
    }

    /** Primeiro cliente do seed com o tipo pedido (PF ou PJ). */
    public static long cliente(String tipo) {
        JsonNode lista = json(api().get("/clientes").then().statusCode(200).extract().asString());
        for (JsonNode c : lista) {
            if (tipo.equals(c.path("tipo").asText())) {
                return c.path("id").asLong();
            }
        }
        throw new AssertionError("nenhum cliente " + tipo + " no seed");
    }

    /**
     * Valor unitário com o preço de tabela, desviando dos centavos que o sefaz-mock usa como gatilho
     * (,50 ,99 ,04 ,13 no total; veja infra/local/sefaz-mock/README.md).
     */
    public static BigDecimal valorSemGatilho(long produtoId, long quantidade) {
        BigDecimal v = preco(produtoId);
        while (gatilho(v.multiply(BigDecimal.valueOf(quantidade)))) {
            v = v.add(new BigDecimal("0.01"));
        }
        return v;
    }

    private static boolean gatilho(BigDecimal total) {
        int centavos = total.setScale(2, java.math.RoundingMode.HALF_UP).remainder(BigDecimal.ONE).movePointRight(2)
                .intValue();
        return centavos == 50 || centavos == 99 || centavos == 4 || centavos == 13;
    }

    // ---------- operação ----------

    /** E-mails que o SES local guardou (o notification-service manda por ele). */
    public static String emailsSes() {
        return given().baseUri(LOCALSTACK).get("/_aws/ses").then().statusCode(200).extract().asString();
    }

    /** Invoca uma Lambda do LocalStack (wc-dlq-redrive, wc-dlq-monitor) e devolve a resposta em JSON. */
    public static JsonNode lambda(String funcao, Map<String, Object> entrada) {
        String payload;
        try {
            payload = JSON.writeValueAsString(entrada);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        try (LambdaClient c = LambdaClient.builder().endpointOverride(URI.create(LOCALSTACK))
                .region(Region.of(env("AWS_REGION", "sa-east-1")))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .httpClient(UrlConnectionHttpClient.builder().socketTimeout(Duration.ofSeconds(60)).build())
                .build()) {
            var r = c.invoke(b -> b.functionName(funcao)
                    .payload(SdkBytes.fromUtf8String(payload)));
            assertThat(r.functionError()).as("erro na Lambda " + funcao + ": " + r.payload().asUtf8String()).isNull();
            return json(r.payload().asUtf8String());
        }
    }

    public static JsonNode json(String s) {
        try {
            return JSON.readTree(s);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    public static <T> boolean algum(Iterable<T> itens, Predicate<T> p) {
        for (T t : itens) {
            if (p.test(t)) {
                return true;
            }
        }
        return false;
    }
}

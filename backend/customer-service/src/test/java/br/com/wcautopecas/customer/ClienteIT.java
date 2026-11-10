package br.com.wcautopecas.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import br.com.wcautopecas.commons.seguranca.JwtService;
import br.com.wcautopecas.commons.seguranca.Papel;
import br.com.wcautopecas.commons.seguranca.SessaoStore;
import br.com.wcautopecas.commons.teste.InfraTeste;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ClienteIT {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        InfraTeste.registrar(r);
    }

    /** Gera CPFs válidos e diferentes a cada teste (os testes dividem o mesmo LocalStack). */
    static final AtomicLong SEQ = new AtomicLong(System.nanoTime() % 100_000_000L);

    static String cpf() {
        String base = String.format("%09d", SEQ.incrementAndGet() % 1_000_000_000L);
        int[] d = new int[11];
        for (int i = 0; i < 9; i++) {
            d[i] = base.charAt(i) - '0';
        }
        for (int t = 9; t < 11; t++) {
            int soma = 0;
            for (int i = 0; i < t; i++) {
                soma += d[i] * (t + 1 - i);
            }
            d[t] = (soma * 10) % 11 % 10;
        }
        StringBuilder sb = new StringBuilder();
        for (int x : d) {
            sb.append(x);
        }
        return sb.toString();
    }

    @Autowired
    TestRestTemplate http;
    @Autowired
    JwtService jwt;
    @Autowired
    SessaoStore sessoes;
    @Autowired
    SqsClient sqs;
    @Autowired
    SnsClient sns;
    @Autowired
    ObjectMapper mapper;

    String admin;
    String vendedor;

    @BeforeEach
    void tokens() {
        admin = token(1, Papel.ADMIN);
        vendedor = token(2, Papel.VENDEDOR);
    }

    String token(long id, Papel papel) {
        var t = jwt.emitir(id, papel);
        sessoes.criar(id, t.token(), Duration.ofMinutes(10));
        return t.token();
    }

    ResponseEntity<JsonNode> chamar(HttpMethod metodo, String url, Object corpo, String token) {
        HttpHeaders h = new HttpHeaders();
        if (token != null) {
            h.setBearerAuth(token);
        }
        return http.exchange(url, metodo, new HttpEntity<>(corpo, h), JsonNode.class);
    }

    Map<String, Object> input(String nome, String documento, String email) {
        Map<String, Object> m = new HashMap<>();
        m.put("tipo", documento.replaceAll("\\D", "").length() == 14 ? "PJ" : "PF");
        m.put("documento", documento);
        m.put("nome", nome);
        m.put("email", email);
        m.put("telefone", "(11) 98765-4321");
        m.put("cidade", "São Paulo");
        m.put("uf", "SP");
        m.put("ativo", true);
        return m;
    }

    JsonNode criar(Map<String, Object> in) {
        var r = chamar(HttpMethod.POST, "/api/clientes", in, vendedor);
        assertThat(r.getStatusCode()).as(String.valueOf(r.getBody())).isEqualTo(HttpStatus.CREATED);
        return r.getBody();
    }

    @Test
    void criaNoFormatoDoFrontComMascaraLimpa() {
        String doc = cpf();
        String mascarado = doc.substring(0, 3) + "." + doc.substring(3, 6) + "." + doc.substring(6, 9) + "-"
                + doc.substring(9);
        JsonNode c = criar(input("José da Silva", mascarado, "Jose." + doc + "@Email.com"));
        assertThat(c.get("id").isNumber()).isTrue();
        assertThat(c.get("documento").asText()).isEqualTo(doc);
        assertThat(c.get("telefone").asText()).isEqualTo("11987654321");
        assertThat(c.get("email").asText()).isEqualTo("jose." + doc + "@email.com");
        assertThat(c.get("totalCompras").asLong()).isZero();
        assertThat(c.get("ultimaCompra").isNull()).isTrue();
        assertThat(c.get("ativo").asBoolean()).isTrue();
        List<String> campos = List.of("id", "tipo", "documento", "nome", "email", "telefone", "cidade", "uf",
                "ativo", "totalCompras", "ultimaCompra");
        assertThat(c.fieldNames()).toIterable().containsExactlyInAnyOrderElementsOf(campos);
    }

    @Test
    void documentoEEmailUnicos() {
        String doc = cpf();
        criar(input("Ana", doc, "ana" + doc + "@x.com"));
        var dupDoc = chamar(HttpMethod.POST, "/api/clientes", input("Outra", doc, null), vendedor);
        assertThat(dupDoc.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(dupDoc.getBody().get("message").asText()).isEqualTo("Documento já cadastrado");
        var dupEmail = chamar(HttpMethod.POST, "/api/clientes", input("Outra", cpf(), "ANA" + doc + "@x.com"),
                vendedor);
        assertThat(dupEmail.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(dupEmail.getBody().get("message").asText()).isEqualTo("E-mail já cadastrado");
    }

    @Test
    void validacoes() {
        var cpfInvalido = chamar(HttpMethod.POST, "/api/clientes", input("X", "11111111111", null), vendedor);
        assertThat(cpfInvalido.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(cpfInvalido.getBody().get("campos").get(0).get("campo").asText()).isEqualTo("documento");

        Map<String, Object> pjComCpf = input("X", cpf(), null);
        pjComCpf.put("tipo", "PJ");
        assertThat(chamar(HttpMethod.POST, "/api/clientes", pjComCpf, vendedor).getBody().get("message").asText())
                .isEqualTo("CPF informado para pessoa jurídica");

        Map<String, Object> uf = input("X", cpf(), null);
        uf.put("uf", "XX");
        assertThat(chamar(HttpMethod.POST, "/api/clientes", uf, vendedor).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        Map<String, Object> tel = input("X", cpf(), null);
        tel.put("telefone", "123");
        assertThat(chamar(HttpMethod.POST, "/api/clientes", tel, vendedor).getBody().get("campos").get(0)
                .get("campo").asText()).isEqualTo("telefone");

        assertThat(chamar(HttpMethod.GET, "/api/clientes?tipo=XX", null, vendedor).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void listaEmArrayOrdenadoEComFiltros() {
        String sufixo = Long.toString(SEQ.incrementAndGet());
        String docB = cpf();
        criar(input("Zé Buscável " + sufixo, docB, null));
        criar(input("Álvaro Buscável " + sufixo, cpf(), null));
        criar(input("Empresa Buscável " + sufixo, "11222333000181".equals(docB) ? "45997418000153" : cnpjUnico(),
                null));

        var todos = chamar(HttpMethod.GET, "/api/clientes?busca=buscavel " + sufixo, null, vendedor);
        assertThat(todos.getBody().isArray()).isTrue();
        assertThat(todos.getBody()).extracting(n -> n.get("nome").asText())
                .containsExactly("Álvaro Buscável " + sufixo, "Empresa Buscável " + sufixo, "Zé Buscável " + sufixo);

        var pf = chamar(HttpMethod.GET, "/api/clientes?tipo=PF&busca=BUSCÁVEL " + sufixo, null, vendedor);
        assertThat(pf.getBody()).hasSize(2);

        var porDoc = chamar(HttpMethod.GET, "/api/clientes?documento=" + docB, null, vendedor);
        assertThat(porDoc.getBody()).hasSize(1);
        assertThat(porDoc.getBody().get(0).get("documento").asText()).isEqualTo(docB);

        var porDigitos = chamar(HttpMethod.GET, "/api/clientes?busca=" + docB.substring(0, 9), null, vendedor);
        assertThat(porDigitos.getBody()).extracting(n -> n.get("documento").asText()).contains(docB);
    }

    static final AtomicLong CNPJ_SEQ = new AtomicLong(System.nanoTime() % 1_000_000L);

    static String cnpjUnico() {
        String base = String.format("%08d0001", CNPJ_SEQ.incrementAndGet() % 100_000_000L);
        int[] p1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] p2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        String com1 = base + dv(base, p1);
        return com1 + dv(com1, p2);
    }

    static int dv(String s, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (s.charAt(i) - '0') * pesos[i];
        }
        int r = soma % 11;
        return r < 2 ? 0 : 11 - r;
    }

    @Test
    void atualizaReativaETrocaDocumento() {
        String doc = cpf();
        JsonNode c = criar(input("Maria", doc, "maria" + doc + "@x.com"));
        long id = c.get("id").asLong();
        String novoDoc = cpf();
        Map<String, Object> in = input("Maria Souza", novoDoc, "maria" + doc + "@x.com");
        var r = chamar(HttpMethod.PUT, "/api/clientes/" + id, in, vendedor);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody().get("nome").asText()).isEqualTo("Maria Souza");
        assertThat(r.getBody().get("documento").asText()).isEqualTo(novoDoc);
        // o documento antigo ficou livre
        criar(input("Outra pessoa", doc, null));
        assertThat(chamar(HttpMethod.PUT, "/api/clientes/999999", in, vendedor).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void desativarSoAdminEIdempotente() {
        long id = criar(input("Carlos", cpf(), null)).get("id").asLong();
        assertThat(chamar(HttpMethod.DELETE, "/api/clientes/" + id, null, vendedor).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(chamar(HttpMethod.DELETE, "/api/clientes/" + id, null, admin).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(chamar(HttpMethod.DELETE, "/api/clientes/" + id, null, admin).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        JsonNode c = chamar(HttpMethod.GET, "/api/clientes/" + id, null, vendedor).getBody();
        assertThat(c.get("ativo").asBoolean()).isFalse();
        // reativa pelo PUT
        Map<String, Object> in = input("Carlos", c.get("documento").asText(), null);
        assertThat(chamar(HttpMethod.PUT, "/api/clientes/" + id, in, vendedor).getBody().get("ativo").asBoolean())
                .isTrue();
    }

    @Test
    void semTokenEh401() {
        assertThat(chamar(HttpMethod.GET, "/api/clientes", null, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void publicaEventoDeCriacao() throws Exception {
        String url = sqs.getQueueUrl(r -> r.queueName("customer-events-queue")).queueUrl();
        String doc = cpf();
        long id = criar(input("Evento", doc, "evento" + doc + "@x.com")).get("id").asLong();
        JsonNode achado = null;
        for (int i = 0; i < 10 && achado == null; i++) {
            for (Message m : sqs.receiveMessage(r -> r.queueUrl(url).maxNumberOfMessages(10).waitTimeSeconds(1)
                    .messageAttributeNames("All")).messages()) {
                JsonNode e = mapper.readTree(m.body());
                if (e.path("customerId").asLong() == id) {
                    achado = e;
                    assertThat(m.messageAttributes()).containsKeys("eventType", "correlationId");
                }
            }
        }
        assertThat(achado).isNotNull();
        assertThat(achado.get("eventType").asText()).isEqualTo("customer.created");
        assertThat(achado.get("data").get("documento").asText()).isEqualTo(doc);
    }

    @Test
    void vendaPeloTopicoAtualizaEstatisticasUmaVezSo() throws Exception {
        long id = criar(input("Comprador", cpf(), null)).get("id").asLong();
        String topico = sns.createTopic(r -> r.name("sales-events-topic")).topicArn();
        long venda = SEQ.incrementAndGet();
        String evento = mapper.writeValueAsString(Map.of("eventType", "sale.created", "saleId", venda,
                "tipoDocumento", "nfe", "clienteId", id, "valorTotal", 179.8, "timestamp", "2026-09-20T15:00:00Z"));
        for (int i = 0; i < 2; i++) { // entregue duas vezes: conta uma
            sns.publish(r -> r.topicArn(topico).message(evento).messageAttributes(Map.of("eventType",
                    software.amazon.awssdk.services.sns.model.MessageAttributeValue.builder().dataType("String")
                            .stringValue("sale.created").build())));
        }
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            JsonNode c = chamar(HttpMethod.GET, "/api/clientes/" + id, null, vendedor).getBody();
            assertThat(c.get("totalCompras").asLong()).isEqualTo(1);
            assertThat(c.get("ultimaCompra").asText()).isEqualTo("2026-09-20T15:00:00Z");
        });
        Thread.sleep(2000);
        assertThat(chamar(HttpMethod.GET, "/api/clientes/" + id, null, vendedor).getBody().get("totalCompras")
                .asLong()).isEqualTo(1);
    }

    @Test
    void vendaDeClienteInexistenteNaoTravaAFila() {
        String url = sqs.getQueueUrl(r -> r.queueName("customer-sales-events-queue")).queueUrl();
        sqs.sendMessage(r -> r.queueUrl(url).messageBody(
                "{\"eventType\":\"sale.created\",\"saleId\":" + SEQ.incrementAndGet()
                        + ",\"clienteId\":987654321,\"valorTotal\":10}")
                .messageAttributes(Map.of("correlationId",
                        MessageAttributeValue.builder().dataType("String").stringValue("teste").build())));
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(
                sqs.getQueueAttributes(r -> r.queueUrl(url).attributeNamesWithStrings(
                        "ApproximateNumberOfMessages", "ApproximateNumberOfMessagesNotVisible"))
                        .attributesAsStrings().values()).containsOnly("0"));
    }
}

package br.com.wcautopecas.product;

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
import java.util.UUID;
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
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProdutoIT {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        InfraTeste.registrar(r);
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
        h.setBearerAuth(token);
        return http.exchange(url, metodo, new HttpEntity<>(corpo, h), JsonNode.class);
    }

    static String codigo() {
        return "T-" + UUID.randomUUID().toString().substring(0, 8);
    }

    Map<String, Object> input(String codigo, String nome, long atual, long minimo) {
        Map<String, Object> m = new HashMap<>();
        m.put("codigo", codigo);
        m.put("nome", nome);
        m.put("ncm", "87083010");
        m.put("estoqueAtual", atual);
        m.put("estoqueMinimo", minimo);
        m.put("precoCusto", 45.5);
        m.put("precoVenda", 89.9);
        return m;
    }

    JsonNode criar(Map<String, Object> in) {
        var r = chamar(HttpMethod.POST, "/api/produtos", in, vendedor);
        assertThat(r.getStatusCode()).as(String.valueOf(r.getBody())).isEqualTo(HttpStatus.CREATED);
        return r.getBody();
    }

    JsonNode produto(long id) {
        return chamar(HttpMethod.GET, "/api/produtos/" + id, null, vendedor).getBody();
    }

    void enviar(Map<String, Object> op) throws Exception {
        String url = sqs.getQueueUrl(r -> r.queueName("inventory-updates-queue")).queueUrl();
        String corpo = mapper.writeValueAsString(op);
        sqs.sendMessage(r -> r.queueUrl(url).messageBody(corpo));
    }

    @Test
    void criaPlanoComStatusEMovimentoInicial() {
        String cod = codigo();
        JsonNode p = criar(input(cod, "Pastilha Teste", 45, 15));
        assertThat(p.get("id").isNumber()).isTrue();
        assertThat(p.fieldNames()).toIterable().containsExactlyInAnyOrder("id", "codigo", "nome", "ncm",
                "estoqueAtual", "estoqueMinimo", "precoCusto", "precoVenda", "status");
        assertThat(p.get("status").asText()).isEqualTo("ok");
        assertThat(p.get("precoVenda").decimalValue()).isEqualByComparingTo("89.90");
        var mov = chamar(HttpMethod.GET, "/api/produtos/" + p.get("id").asLong() + "/movimentos", null, vendedor);
        assertThat(mov.getBody()).hasSize(1);
        assertThat(mov.getBody().get(0).get("motivo").asText()).isEqualTo("Estoque inicial");

        var dup = chamar(HttpMethod.POST, "/api/produtos", input(cod, "Outro", 1, 1), vendedor);
        assertThat(dup.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void validacoes() {
        Map<String, Object> preco = input(codigo(), "X", 1, 1);
        preco.put("precoVenda", 10);
        var r = chamar(HttpMethod.POST, "/api/produtos", preco, vendedor);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody().get("campos").get(0).get("campo").asText()).isEqualTo("precoVenda");

        Map<String, Object> ncm = input(codigo(), "X", 1, 1);
        ncm.put("ncm", "123");
        assertThat(chamar(HttpMethod.POST, "/api/produtos", ncm, vendedor).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(chamar(HttpMethod.GET, "/api/produtos?status=xx", null, vendedor).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void listaFiltraEOrdena() {
        String marca = UUID.randomUUID().toString().substring(0, 6);
        criar(input(codigo(), "Zebra " + marca, 50, 10));
        criar(input(codigo(), "Árvore " + marca, 1, 10));
        var lista = chamar(HttpMethod.GET, "/api/produtos?busca=" + marca, null, vendedor).getBody();
        assertThat(lista).extracting(n -> n.get("nome").asText()).containsExactly("Árvore " + marca, "Zebra " + marca);
        var criticos = chamar(HttpMethod.GET, "/api/produtos?status=critico", null, vendedor).getBody();
        assertThat(criticos).extracting(n -> n.get("nome").asText()).contains("Árvore " + marca)
                .doesNotContain("Zebra " + marca);
        var resumo = chamar(HttpMethod.GET, "/api/produtos/resumo", null, vendedor).getBody();
        assertThat(resumo.fieldNames()).toIterable()
                .containsExactlyInAnyOrder("totalProdutos", "estoqueBaixo", "valorEmEstoque", "itensCriticos");
        assertThat(resumo.get("itensCriticos").asLong()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void alterarQuantidadeSoAdminERegistraAjuste() {
        String cod = codigo();
        long id = criar(input(cod, "Filtro", 20, 10)).get("id").asLong();
        var negado = chamar(HttpMethod.PUT, "/api/produtos/" + id, input(cod, "Filtro", 3, 10), vendedor);
        assertThat(negado.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        var soNome = chamar(HttpMethod.PUT, "/api/produtos/" + id, input(cod, "Filtro Novo", 20, 10), vendedor);
        assertThat(soNome.getStatusCode()).isEqualTo(HttpStatus.OK);

        var ok = chamar(HttpMethod.PUT, "/api/produtos/" + id, input(cod, "Filtro Novo", 3, 10), admin);
        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ok.getBody().get("estoqueAtual").asLong()).isEqualTo(3);
        assertThat(ok.getBody().get("status").asText()).isEqualTo("critico");
        JsonNode mov = chamar(HttpMethod.GET, "/api/produtos/" + id + "/movimentos", null, vendedor).getBody().get(0);
        assertThat(mov.get("tipo").asText()).isEqualTo("ajuste");
        assertThat(mov.get("quantidade").asLong()).isEqualTo(-17);
        assertThat(mov.get("usuarioId").asLong()).isEqualTo(1);
        assertThat(mov.get("motivo").asText()).isEqualTo("Ajuste pela tela de estoque");

        // estoque mínimo menor recalcula o status sem movimento
        var min = chamar(HttpMethod.PUT, "/api/produtos/" + id, input(cod, "Filtro Novo", 3, 2), vendedor);
        assertThat(min.getBody().get("status").asText()).isEqualTo("ok");
    }

    @Test
    void filaDeEstoqueReservaBaixaEntradaEAjuste() throws Exception {
        long id = criar(input(codigo(), "Correia", 10, 4)).get("id").asLong();
        long venda = System.nanoTime();
        enviar(Map.of("type", "reserve", "produtoId", id, "quantidade", 3, "referenciaId", venda,
                "referenciaType", "sale"));
        enviar(Map.of("type", "reserve", "produtoId", id, "quantidade", 3, "referenciaId", venda,
                "referenciaType", "sale")); // repetida: ignorada
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(
                chamar(HttpMethod.GET, "/internal/produtos/" + id, null, vendedor).getBody().get("reservado").asLong())
                .isEqualTo(3));

        enviar(Map.of("type", "decrease", "produtoId", id, "quantidade", 3, "referenciaId", venda,
                "referenciaType", "sale"));
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            JsonNode f = chamar(HttpMethod.GET, "/internal/produtos/" + id, null, vendedor).getBody();
            assertThat(f.get("estoqueAtual").asLong()).isEqualTo(7);
            assertThat(f.get("reservado").asLong()).isZero();
        });

        enviar(Map.of("type", "increase", "produtoId", id, "quantidade", 5, "referenciaId", venda + 1,
                "referenciaType", "entry"));
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(produto(id).get("estoqueAtual").asLong()).isEqualTo(12));

        var ajuste = chamar(HttpMethod.POST, "/api/produtos/" + id + "/ajuste",
                Map.of("quantidadeNova", 1, "motivo", "Contagem de inventário"), admin);
        assertThat(ajuste.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            JsonNode p = produto(id);
            assertThat(p.get("estoqueAtual").asLong()).isEqualTo(1);
            assertThat(p.get("status").asText()).isEqualTo("critico");
        });
        List<String> tipos = chamar(HttpMethod.GET, "/api/produtos/" + id + "/movimentos", null, vendedor)
                .getBody().findValuesAsText("tipo");
        assertThat(tipos).containsExactly("ajuste", "entrada", "saida", "reserva", "ajuste");
    }

    @Test
    void mudarParaCriticoAvisaAEmailQueue() throws Exception {
        String cod = codigo();
        long id = criar(input(cod, "Radiador", 10, 10)).get("id").asLong();
        chamar(HttpMethod.PUT, "/api/produtos/" + id, input(cod, "Radiador", 2, 10), admin);
        String url = sqs.getQueueUrl(r -> r.queueName("email-queue")).queueUrl();
        JsonNode achado = null;
        for (int i = 0; i < 10 && achado == null; i++) {
            for (Message m : sqs.receiveMessage(r -> r.queueUrl(url).maxNumberOfMessages(10).waitTimeSeconds(1))
                    .messages()) {
                JsonNode e = mapper.readTree(m.body());
                if (e.path("produtoId").asLong() == id) {
                    achado = e;
                }
            }
        }
        assertThat(achado).isNotNull();
        assertThat(achado.get("status").asText()).isEqualTo("critico");
        assertThat(achado.get("codigo").asText()).isEqualTo(cod);
    }

    @Test
    void desativarSoAdminEBloqueadoComReserva() throws Exception {
        long livre = criar(input(codigo(), "Livre", 5, 1)).get("id").asLong();
        assertThat(chamar(HttpMethod.DELETE, "/api/produtos/" + livre, null, vendedor).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(chamar(HttpMethod.DELETE, "/api/produtos/" + livre, null, admin).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(chamar(HttpMethod.GET, "/api/produtos/" + livre, null, vendedor).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        long reservado = criar(input(codigo(), "Reservado", 5, 1)).get("id").asLong();
        enviar(Map.of("type", "reserve", "produtoId", reservado, "quantidade", 2, "referenciaId", System.nanoTime(),
                "referenciaType", "sale"));
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(chamar(HttpMethod.GET,
                "/internal/produtos/" + reservado, null, vendedor).getBody().get("reservado").asLong()).isEqualTo(2));
        var r = chamar(HttpMethod.DELETE, "/api/produtos/" + reservado, null, admin);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(r.getBody().get("message").asText()).isEqualTo("Produto com estoque reservado");
    }

    @Test
    void disponibilidadeSomaLinhasDoMesmoProduto() {
        long id = criar(input(codigo(), "Vela", 5, 1)).get("id").asLong();
        var r = chamar(HttpMethod.POST, "/internal/produtos/disponibilidade", List.of(
                Map.of("produtoId", id, "quantidade", 3), Map.of("produtoId", id, "quantidade", 3),
                Map.of("produtoId", 999999, "quantidade", 1)), vendedor).getBody();
        assertThat(r).hasSize(2);
        assertThat(r.get(0).get("disponivel").asLong()).isEqualTo(5);
        assertThat(r.get(0).get("suficiente").asBoolean()).isFalse();
        assertThat(r.get(1).get("existe").asBoolean()).isFalse();
    }
}

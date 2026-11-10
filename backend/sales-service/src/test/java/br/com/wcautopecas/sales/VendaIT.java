package br.com.wcautopecas.sales;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

import br.com.wcautopecas.commons.erro.ApiException;
import br.com.wcautopecas.commons.seguranca.JwtService;
import br.com.wcautopecas.commons.seguranca.Papel;
import br.com.wcautopecas.commons.seguranca.SessaoStore;
import br.com.wcautopecas.commons.teste.InfraTeste;
import br.com.wcautopecas.sales.integracao.Integracoes;
import br.com.wcautopecas.sales.integracao.Integracoes.Cliente;
import br.com.wcautopecas.sales.integracao.Integracoes.Produto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class VendaIT {

    static final long SEM_ESTOQUE = 900;
    static final long INATIVO = 901;
    static final long CLIENTE_COM_EMAIL = 10;
    static final long CLIENTE_INATIVO = 11;
    static final long CLIENTE_FORA = 12;

    /** Mensagens já lidas de cada fila (as filas são compartilhadas entre os testes). */
    static final Map<String, List<JsonNode>> LIDAS = new ConcurrentHashMap<>();

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
    @MockBean
    Integracoes integracoes;

    String vendedor;

    @BeforeEach
    void preparar() {
        var t = jwt.emitir(2, Papel.VENDEDOR);
        sessoes.criar(2, t.token(), Duration.ofMinutes(10));
        vendedor = t.token();
        given(integracoes.produto(anyLong())).willAnswer(inv -> {
            long id = inv.getArgument(0);
            return new Produto(id, "P-" + id, "Peça " + id, new BigDecimal("89.90"), id != INATIVO,
                    id == SEM_ESTOQUE ? 3 : 100);
        });
        given(integracoes.cliente(anyLong())).willAnswer(inv -> {
            long id = inv.getArgument(0);
            if (id == CLIENTE_FORA) {
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Serviço de clientes indisponível");
            }
            return new Cliente(id, "Cliente " + id, id == CLIENTE_COM_EMAIL ? "cliente@teste.com" : "",
                    id != CLIENTE_INATIVO);
        });
    }

    ResponseEntity<JsonNode> chamar(HttpMethod metodo, String url, Object corpo) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(vendedor);
        return http.exchange(url, metodo, new HttpEntity<>(corpo, h), JsonNode.class);
    }

    static Map<String, Object> item(long produtoId, long qtd, double valor) {
        return Map.of("produtoId", produtoId, "quantidade", qtd, "valorUnitario", valor);
    }

    static Map<String, Object> venda(String status, Long clienteId, List<Map<String, Object>> itens) {
        Map<String, Object> m = new HashMap<>();
        m.put("tipoDocumento", "nfce");
        m.put("modalidade", "fisica");
        m.put("clienteId", clienteId);
        m.put("metodoPagamento", "credito");
        m.put("bandeira", "visa");
        m.put("destinoDinheiro", null);
        m.put("desconto", 0);
        m.put("status", status);
        m.put("itens", itens);
        return m;
    }

    JsonNode criar(Map<String, Object> in) {
        var r = chamar(HttpMethod.POST, "/api/vendas", in);
        assertThat(r.getStatusCode()).as(String.valueOf(r.getBody())).isEqualTo(HttpStatus.CREATED);
        return r.getBody();
    }

    List<JsonNode> mensagens(String fila) {
        String url = sqs.getQueueUrl(r -> r.queueName(fila)).queueUrl();
        List<JsonNode> lidas = LIDAS.computeIfAbsent(fila, f -> new ArrayList<>());
        for (var m : sqs.receiveMessage(r -> r.queueUrl(url).maxNumberOfMessages(10).waitTimeSeconds(1)).messages()) {
            try {
                lidas.add(mapper.readTree(m.body()));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            sqs.deleteMessage(r -> r.queueUrl(url).receiptHandle(m.receiptHandle()));
        }
        return lidas;
    }

    List<JsonNode> esperar(String fila, int quantidade, Predicate<JsonNode> filtro) {
        List<JsonNode> achadas = new ArrayList<>();
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            achadas.clear();
            mensagens(fila).stream().filter(filtro).forEach(achadas::add);
            assertThat(achadas).hasSize(quantidade);
        });
        return achadas;
    }

    static Predicate<JsonNode> daVenda(String campo, long id) {
        return n -> n.path(campo).asLong(-1) == id;
    }

    @Test
    void vendaFinalizadaResponde201ReservaEPublicaEventos() {
        JsonNode v = criar(venda("finalizada", CLIENTE_COM_EMAIL,
                List.of(item(7, 2, 89.9), item(8, 1, 10.0), item(7, 1, 89.9))));
        assertThat(v.fieldNames()).toIterable().containsExactlyInAnyOrder("id", "tipoDocumento", "modalidade",
                "clienteId", "metodoPagamento", "bandeira", "destinoDinheiro", "desconto", "subtotal", "total",
                "status", "itens");
        assertThat(v.get("id").isNumber()).isTrue();
        assertThat(v.get("subtotal").decimalValue()).isEqualByComparingTo("279.70");
        assertThat(v.get("total").decimalValue()).isEqualByComparingTo("279.70");
        assertThat(v.get("destinoDinheiro").isNull()).isTrue();
        assertThat(v.get("itens")).hasSize(3);
        assertThat(v.get("itens").get(0).fieldNames()).toIterable()
                .containsExactly("produtoId", "quantidade", "valorUnitario");
        long id = v.get("id").asLong();

        // linhas do mesmo produto viram uma reserva só (a idempotência do estoque é por produto)
        var reservas = esperar("inventory-updates-queue", 2, daVenda("referenciaId", id));
        assertThat(reservas).allSatisfy(m -> {
            assertThat(m.get("type").asText()).isEqualTo("reserve");
            assertThat(m.get("referenciaType").asText()).isEqualTo("sale");
            assertThat(m.get("usuarioId").asLong()).isEqualTo(2);
        });
        assertThat(reservas).anySatisfy(m -> {
            assertThat(m.get("produtoId").asLong()).isEqualTo(7);
            assertThat(m.get("quantidade").asLong()).isEqualTo(3);
        });

        JsonNode criada = esperar("sales-events-queue", 1, daVenda("saleId", id)).get(0);
        assertThat(criada.get("eventType").asText()).isEqualTo("sale.created");
        assertThat(criada.get("tipoDocumento").asText()).isEqualTo("nfce");
        assertThat(criada.get("valorTotal").decimalValue()).isEqualByComparingTo("279.70");
        esperar("customer-sales-events-queue", 1, daVenda("saleId", id));

        JsonNode email = esperar("email-queue", 1, daVenda("vendaId", id)).get(0);
        assertThat(email.get("MessageType").asText()).isEqualTo("sale_completed");
        assertThat(email.get("clienteEmail").asText()).isEqualTo("cliente@teste.com");
        assertThat(email.get("numero").asText()).isEqualTo(String.format("VD-%06d", id));
    }

    @Test
    void validacoesDeEntrada() {
        var desconto = venda("finalizada", null, List.of(item(7, 2, 89.9)));
        desconto.put("desconto", 200);
        var r = chamar(HttpMethod.POST, "/api/vendas", desconto);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody().get("message").asText()).isEqualTo("Desconto maior que o subtotal");
        assertThat(r.getBody().get("campos").get(0).get("campo").asText()).isEqualTo("desconto");

        var semBandeira = venda("finalizada", null, List.of(item(7, 1, 89.9)));
        semBandeira.put("bandeira", null);
        r = chamar(HttpMethod.POST, "/api/vendas", semBandeira);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody().get("campos").get(0).get("campo").asText()).isEqualTo("bandeira");

        var dinheiro = venda("finalizada", null, List.of(item(7, 1, 89.9)));
        dinheiro.put("metodoPagamento", "dinheiro");
        dinheiro.put("bandeira", null);
        r = chamar(HttpMethod.POST, "/api/vendas", dinheiro);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody().get("campos").get(0).get("campo").asText()).isEqualTo("destinoDinheiro");

        var semItens = venda("finalizada", null, List.of());
        assertThat(chamar(HttpMethod.POST, "/api/vendas", semItens).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        var quantidadeZero = venda("finalizada", null, List.of(item(7, 0, 89.9)));
        assertThat(chamar(HttpMethod.POST, "/api/vendas", quantidadeZero).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        var statusCancelada = venda("cancelada", null, List.of(item(7, 1, 89.9)));
        assertThat(chamar(HttpMethod.POST, "/api/vendas", statusCancelada).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void regrasDeClienteParaNfe() {
        var nfe = venda("finalizada", null, List.of(item(7, 1, 89.9)));
        nfe.put("tipoDocumento", "nfe");
        var r = chamar(HttpMethod.POST, "/api/vendas", nfe);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody().get("message").asText()).isEqualTo("NF-e exige cliente identificado");

        nfe.put("clienteId", CLIENTE_INATIVO);
        r = chamar(HttpMethod.POST, "/api/vendas", nfe);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(r.getBody().get("message").asText()).isEqualTo("Cliente inativo");

        nfe.put("clienteId", CLIENTE_FORA);
        r = chamar(HttpMethod.POST, "/api/vendas", nfe);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(r.getBody().get("message").asText()).isEqualTo("Serviço de clientes indisponível");

        nfe.put("clienteId", CLIENTE_COM_EMAIL);
        assertThat(criar(nfe).get("tipoDocumento").asText()).isEqualTo("nfe");
    }

    @Test
    void estoqueEProdutoInvalidos() {
        // 2 + 2 linhas do mesmo produto passam do disponível (3)
        var r = chamar(HttpMethod.POST, "/api/vendas",
                venda("finalizada", null, List.of(item(7, 1, 89.9), item(SEM_ESTOQUE, 2, 10), item(SEM_ESTOQUE, 2, 10))));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(r.getBody().get("message").asText()).isEqualTo("Estoque insuficiente para P-900: disponível 3");
        assertThat(r.getBody().get("campos").get(0).get("campo").asText()).isEqualTo("itens[1].quantidade");

        r = chamar(HttpMethod.POST, "/api/vendas", venda("finalizada", null, List.of(item(INATIVO, 1, 10))));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(r.getBody().get("message").asText()).contains("P-901");
    }

    @Test
    void orcamentoSoReservaEDepoisFinaliza() {
        long id = criar(venda("orcamento", CLIENTE_COM_EMAIL, List.of(item(7, 1, 89.9)))).get("id").asLong();
        esperar("inventory-updates-queue", 1, daVenda("referenciaId", id));
        assertThat(mensagens("sales-events-queue")).noneMatch(daVenda("saleId", id));

        var r = chamar(HttpMethod.PUT, "/api/vendas/" + id + "/status", Map.of("status", "finalizada"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody().get("status").asText()).isEqualTo("finalizada");
        esperar("sales-events-queue", 1, daVenda("saleId", id));

        r = chamar(HttpMethod.PUT, "/api/vendas/" + id + "/status", Map.of("status", "cancelada"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(r.getBody().get("message").asText())
                .isEqualTo("Vendas finalizadas devem ser canceladas pela nota fiscal");
        assertThat(chamar(HttpMethod.DELETE, "/api/vendas/" + id, null).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void cancelarOrcamentoLiberaEstoque() {
        long id = criar(venda("orcamento", null, List.of(item(7, 1, 89.9), item(8, 2, 5)))).get("id").asLong();
        assertThat(chamar(HttpMethod.DELETE, "/api/vendas/" + id, null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        var release = esperar("inventory-updates-queue", 2,
                daVenda("referenciaId", id).and(m -> "release".equals(m.path("type").asText())));
        assertThat(release).extracting(m -> m.get("produtoId").asLong()).containsExactlyInAnyOrder(7L, 8L);
        assertThat(chamar(HttpMethod.GET, "/api/vendas/" + id, null).getBody().get("status").asText())
                .isEqualTo("cancelada");
    }

    @Test
    void finalizacoesConcorrentesSoUmaPassa() throws Exception {
        long id = criar(venda("orcamento", null, List.of(item(7, 1, 89.9)))).get("id").asLong();
        try (var pool = Executors.newFixedThreadPool(4)) {
            List<Future<HttpStatus>> rs = IntStream.range(0, 4).mapToObj(i -> pool.submit(() -> HttpStatus.valueOf(
                    chamar(HttpMethod.PUT, "/api/vendas/" + id + "/status", Map.of("status", "finalizada"))
                            .getStatusCode().value()))).toList();
            List<HttpStatus> status = new ArrayList<>();
            for (var f : rs) {
                status.add(f.get());
            }
            assertThat(status).filteredOn(s -> s == HttpStatus.OK).hasSize(1);
            assertThat(status).filteredOn(s -> s != HttpStatus.OK)
                    .allMatch(s -> s == HttpStatus.CONFLICT || s == HttpStatus.UNPROCESSABLE_ENTITY);
        }
        esperar("sales-events-queue", 1, daVenda("saleId", id));
    }

    @Test
    void criacoesParalelasGeramIdsUnicos() throws Exception {
        try (var pool = Executors.newFixedThreadPool(10)) {
            var futuros = IntStream.range(0, 20).mapToObj(i -> pool.submit(
                    () -> criar(venda("orcamento", null, List.of(item(7, 1, 1)))).get("id").asLong())).toList();
            List<Long> ids = new ArrayList<>();
            for (var f : futuros) {
                ids.add(f.get());
            }
            assertThat(ids).doesNotHaveDuplicates().hasSize(20);
        }
    }

    @Test
    void listarDetalharEVincularNota() {
        long orc = criar(venda("orcamento", 30L, List.of(item(7, 1, 89.9)))).get("id").asLong();
        long fin = criar(venda("finalizada", 30L, List.of(item(8, 2, 10)))).get("id").asLong();

        var lista = chamar(HttpMethod.GET, "/api/vendas?clienteId=30", null).getBody();
        assertThat(lista).extracting(n -> n.get("id").asLong()).containsExactly(fin, orc);

        lista = chamar(HttpMethod.GET, "/api/vendas?status=orcamento&clienteId=30", null).getBody();
        assertThat(lista).extracting(n -> n.get("id").asLong()).containsExactly(orc);

        String hoje = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString();
        lista = chamar(HttpMethod.GET, "/api/vendas?status=finalizada&de=" + hoje + "&ate=" + hoje, null).getBody();
        assertThat(lista).extracting(n -> n.get("id").asLong()).contains(fin);
        assertThat(chamar(HttpMethod.GET, "/api/vendas?de=ontem", null).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        var d = chamar(HttpMethod.GET, "/api/vendas/" + fin, null).getBody();
        assertThat(d.get("notaFiscalId").isNull()).isTrue();
        assertThat(d.get("itens").get(0).get("quantidade").asLong()).isEqualTo(2);

        assertThat(chamar(HttpMethod.PUT, "/internal/vendas/" + fin + "/nota-fiscal", Map.of("notaFiscalId", 77))
                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(chamar(HttpMethod.GET, "/api/vendas/" + fin, null).getBody().get("notaFiscalId").asLong())
                .isEqualTo(77);
        assertThat(chamar(HttpMethod.PUT, "/internal/vendas/999999/nota-fiscal", Map.of("notaFiscalId", 1))
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(chamar(HttpMethod.GET, "/api/vendas/999999", null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void notaCanceladaCancelaAVenda() {
        long orc = criar(venda("orcamento", 31L, List.of(item(7, 1, 89.9)))).get("id").asLong();
        long fin = criar(venda("finalizada", 31L, List.of(item(8, 1, 10)))).get("id").asLong();
        String url = "/internal/vendas/" + fin + "/cancelamento";

        assertThat(chamar(HttpMethod.PUT, url, Map.of("notaFiscalId", 78)).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        var d = chamar(HttpMethod.GET, "/api/vendas/" + fin, null).getBody();
        assertThat(d.get("status").asText()).isEqualTo("cancelada");
        assertThat(d.get("notaFiscalId").asLong()).isEqualTo(78);
        // repetido (retentativa do fiscal-service) não dá erro
        assertThat(chamar(HttpMethod.PUT, url, Map.of("notaFiscalId", 78)).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        // orçamento não tem nota: cancela pela tela, não por aqui
        assertThat(chamar(HttpMethod.PUT, "/internal/vendas/" + orc + "/cancelamento", Map.of("notaFiscalId", 1))
                .getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void semTokenResponde401() {
        var r = http.postForEntity("/api/vendas", venda("orcamento", null, List.of(item(7, 1, 1))), JsonNode.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}

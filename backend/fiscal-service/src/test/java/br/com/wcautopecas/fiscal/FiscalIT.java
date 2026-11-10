package br.com.wcautopecas.fiscal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import br.com.wcautopecas.commons.seguranca.JwtService;
import br.com.wcautopecas.commons.seguranca.Papel;
import br.com.wcautopecas.commons.seguranca.SessaoStore;
import br.com.wcautopecas.commons.teste.InfraTeste;
import br.com.wcautopecas.fiscal.integracao.Integracoes;
import br.com.wcautopecas.fiscal.integracao.Integracoes.Cliente;
import br.com.wcautopecas.fiscal.integracao.Integracoes.ItemVenda;
import br.com.wcautopecas.fiscal.integracao.Integracoes.ProdutoFiscal;
import br.com.wcautopecas.fiscal.integracao.Integracoes.Venda;
import br.com.wcautopecas.fiscal.nota.NotaItem;
import br.com.wcautopecas.fiscal.nota.NotaLinha;
import br.com.wcautopecas.fiscal.nota.NotaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
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
import br.com.wcautopecas.fiscal.xml.CertificadoA1;
import br.com.wcautopecas.fiscal.xml.ChaveAcesso;
import br.com.wcautopecas.fiscal.xml.CertificadosTeste;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FiscalIT {

    static final Map<String, List<JsonNode>> LIDAS = new ConcurrentHashMap<>();
    /** Ids de venda altos e únicos por teste, para não colidir com outros testes da mesma JVM. */
    static final AtomicLong VENDAS = new AtomicLong(System.currentTimeMillis() % 1_000_000 * 1000);
    /** Série própria por execução, para a numeração e as faixas não colidirem. */
    static final String SERIE = String.valueOf(100 + System.currentTimeMillis() % 800);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        InfraTeste.registrar(r);
        r.add("wc.fiscal.serie", () -> SERIE);
        r.add("wc.fiscal.visibilidade-jobs", () -> "2");
        r.add("wc.fiscal.visibilidade-sefaz", () -> "2");
        r.add("wc.fiscal.sefaz.url", SefazMockTeste::url);
        r.add("wc.fiscal.sefaz.esperas-consulta", () -> "1s,1s,1s");
        r.add("wc.fiscal.sefaz.esperas-retentativa", () -> "1s,1s");
        r.add("wc.fiscal.sefaz.atraso-reenvio", () -> "1s");
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
    @Autowired
    NotaRepository repo;
    @Autowired
    S3Client s3;
    @Autowired
    SecretsManagerClient secrets;
    @Autowired
    CertificadoA1 certificado;
    @MockBean
    Integracoes integracoes;

    String admin;
    String vendedor;

    @BeforeEach
    void preparar() {
        admin = token(1, Papel.ADMIN);
        vendedor = token(2, Papel.VENDEDOR);
        given(integracoes.produto(anyLong())).willAnswer(inv -> {
            long id = inv.getArgument(0);
            return new ProdutoFiscal(id, "P-" + id, "Peça " + id, "87083010", "5102", "0", "00", new BigDecimal("18"));
        });
        given(integracoes.cliente(anyLong())).willAnswer(inv -> {
            long id = inv.getArgument(0);
            return new Cliente(id, "Cliente " + id, "52998224725", id == 99 ? "Rio de Janeiro" : "São Paulo",
                    id == 99 ? "RJ" : "SP", "c@teste.com", true);
        });
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

    long venda(String tipoDocumento, Long clienteId) {
        long id = VENDAS.incrementAndGet();
        given(integracoes.venda(id)).willReturn(new Venda(id, tipoDocumento, "fisica", clienteId, "credito", "visa", new BigDecimal("10.00"),
                new BigDecimal("189.80"), new BigDecimal("179.80"), "finalizada",
                List.of(new ItemVenda(7, 2, new BigDecimal("89.90")), new ItemVenda(8, 1, new BigDecimal("10.00"))),
                null));
        return id;
    }

    void enviarVenda(long id) throws Exception {
        String url = sqs.getQueueUrl(r -> r.queueName("sales-events-queue")).queueUrl();
        String corpo = mapper.writeValueAsString(Map.of("eventType", "sale.created", "saleId", id,
                "tipoDocumento", "nfce", "valorTotal", 179.8, "timestamp", Instant.now().toString()));
        sqs.sendMessage(r -> r.queueUrl(url).messageBody(corpo));
    }

    List<JsonNode> notasDaVenda(long vendaId) {
        return toList(chamar(HttpMethod.GET, "/api/notas?vendaId=" + vendaId, null, vendedor).getBody());
    }

    static List<JsonNode> toList(JsonNode arr) {
        List<JsonNode> l = new ArrayList<>();
        arr.forEach(l::add);
        return l;
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

    /** Nota de venda já autorizada, gravada direto na tabela (o fluxo da SEFAZ é dos EPB-08/09). */
    NotaItem notaAutorizada(String tipo, Instant autorizadaEm) {
        long vendaId = VENDAS.incrementAndGet();
        NotaItem n = new NotaItem();
        n.setId(vendaId);
        n.setTipo(tipo);
        n.setModelo("NFCE".equals(tipo) ? "65" : "55");
        n.setSerie(SERIE);
        n.setNumero(String.format("%06d", vendaId % 1_000_000));
        n.setChaveAcesso(ChaveAcesso.gerar("35", "2611", "12345678000195", n.getModelo(), SERIE,
                Long.parseLong(n.getNumero()), "1").valor());
        n.setStatus("autorizada");
        n.setVendaId(vendaId);
        n.setClienteId(5L);
        n.setClienteNome("Cliente 5");
        n.setClienteUf("SP");
        n.setClienteDocumento("52998224725");
        n.setClienteCidade("São Paulo");
        n.setMetodoPagamento("pix");
        n.setModalidade("fisica");
        n.setValorProdutos(new BigDecimal("189.80"));
        n.setValorDesconto(new BigDecimal("10.00"));
        n.setValorTotal(new BigDecimal("179.80"));
        n.setEmitidaEm(autorizadaEm.toString());
        n.setAutorizadaEm(autorizadaEm.toString());
        n.setTentativasEnvio(0);
        NotaLinha a = linha(n.getId(), 1, 7, 2, "89.90");
        NotaLinha b = linha(n.getId(), 2, 8, 1, "10.00");
        NotaLinha c = linha(n.getId(), 3, 7, 1, "89.90");
        repo.criar(n, List.of(a, b, c), List.of());
        return n;
    }

    static NotaLinha linha(long notaId, long item, long produto, long qtd, String valor) {
        NotaLinha l = new NotaLinha();
        l.setNotaId(notaId);
        l.setItemId(item);
        l.setProdutoId(produto);
        l.setCodigo("P-" + produto);
        l.setDescricao("Peça " + produto);
        l.setNcm("87083010");
        l.setCfop("5102");
        l.setQuantidade(qtd);
        l.setValorUnitario(new BigDecimal(valor));
        l.setQuantidadeDevolvida(0L);
        l.setQuantidadeEmDevolucao(0L);
        return l;
    }

    @Test
    void vendaViraNotaUmaVezSo() throws Exception {
        long vendaId = venda("nfce", null);
        enviarVenda(vendaId);
        enviarVenda(vendaId);
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(notasDaVenda(vendaId)).hasSize(1));
        JsonNode n = notasDaVenda(vendaId).get(0);
        assertThat(n.fieldNames()).toIterable().containsExactlyInAnyOrder("id", "tipo", "numero", "serie",
                "chaveAcesso", "clienteId", "clienteNome", "clienteDocumento", "fornecedorId", "valorTotal",
                "emitidaEm", "status", "vendaId", "notaReferenciadaId");
        assertThat(n.get("tipo").asText()).isEqualTo("NFCE");
        assertThat(n.get("status").asText()).isEqualTo("processando");
        assertThat(n.get("numero").asText()).matches("\\d{6}");
        assertThat(n.get("serie").asText()).isEqualTo(SERIE);
        assertThat(n.get("clienteNome").isNull()).isTrue();
        assertThat(n.get("valorTotal").decimalValue()).isEqualByComparingTo("179.80");
        long notaId = n.get("id").asLong();

        var itens = chamar(HttpMethod.GET, "/api/notas/" + notaId + "/itens", null, vendedor).getBody();
        assertThat(itens).hasSize(2);
        assertThat(itens.get(0).fieldNames()).toIterable().containsExactlyInAnyOrder("produtoId", "codigo",
                "descricao", "ncm", "cfop", "quantidade", "valorUnitario");
        assertThat(itens.get(0).get("cfop").asText()).isEqualTo("5102");

        verify(integracoes, atLeastOnce()).vincularNota(eq(vendaId), eq(notaId));
        // generate_xml → XML assinado no S3 → sefaz-queue → simulador responde 100
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(repo.porId(notaId).orElseThrow().getStatus()).isEqualTo("autorizada"));
        NotaItem gravada = repo.porId(notaId).orElseThrow();
        assertThat(gravada.getProtocolo()).matches("135\\d{12}");
        assertThat(gravada.getCodigoStatus()).isEqualTo("100");
        assertThat(gravada.getAutorizadaEm()).isNotNull();
        assertThat(gravada.getChaveAcesso()).hasSize(44).contains("65" + String.format("%03d", Integer.parseInt(SERIE)));
        String caminho = gravada.getXmlPath().replace("s3://wc-documents-bucket/", "");
        assertThat(caminho).startsWith("NFCE/").endsWith(gravada.getChaveAcesso() + ".xml");
        String xml = s3.getObjectAsBytes(r -> r.bucket("wc-documents-bucket").key(caminho)).asUtf8String();
        assertThat(xml).contains("<nfeProc").contains("<Signature").contains("<protNFe").contains("<qrCode>");

        // baixa de estoque por produto da venda e aviso ao document-service
        var baixas = esperar("inventory-updates-queue", 2, m -> m.path("referenciaId").asLong() == vendaId
                && "decrease".equals(m.path("type").asText()));
        assertThat(baixas).allMatch(m -> "sale".equals(m.path("referenciaType").asText()));
        esperar("document-events-queue", 1, m -> m.path("notaId").asLong() == notaId
                && "nota.autorizada".equals(m.path("eventType").asText()));
        assertThat(chamar(HttpMethod.GET, "/api/notas/" + notaId, null, vendedor).getBody().get("status").asText())
                .isEqualTo("autorizada");
    }

    @Test
    void numeracaoIndependentePorModeloECfopInterestadual() throws Exception {
        long nfce1 = venda("nfce", null);
        long nfe1 = venda("nfe", 99L);
        long nfce2 = venda("nfce", null);
        for (long v : List.of(nfce1, nfe1, nfce2)) {
            enviarVenda(v);
            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(notasDaVenda(v)).hasSize(1));
        }
        int a = notasDaVenda(nfce1).get(0).get("numero").asInt();
        int b = notasDaVenda(nfce2).get(0).get("numero").asInt();
        JsonNode nfe = notasDaVenda(nfe1).get(0);
        assertThat(b).isEqualTo(a + 1); // a NF-e no meio não consumiu número da NFC-e
        assertThat(nfe.get("tipo").asText()).isEqualTo("NFE");
        assertThat(nfe.get("clienteNome").asText()).isEqualTo("Cliente 99");
        assertThat(nfe.get("clienteDocumento").asText()).isEqualTo("52998224725");
        var itens = chamar(HttpMethod.GET, "/api/notas/" + nfe.get("id").asLong() + "/itens", null, vendedor);
        assertThat(itens.getBody().get(0).get("cfop").asText()).isEqualTo("6102");
    }

    @Test
    void listarComFiltros() {
        NotaItem n = notaAutorizada("NFE", Instant.now());
        String hoje = n.getEmitidaEm().substring(0, 10);
        var lista = toList(chamar(HttpMethod.GET, "/api/notas?status=autorizada&de=" + hoje + "&ate=" + hoje, null,
                vendedor).getBody());
        assertThat(lista).extracting(x -> x.get("id").asLong()).contains(n.getId());
        assertThat(lista).allMatch(x -> "autorizada".equals(x.get("status").asText()));
        lista = toList(chamar(HttpMethod.GET, "/api/notas?tipo=NFE", null, vendedor).getBody());
        assertThat(lista).extracting(x -> x.get("id").asLong()).contains(n.getId());
        assertThat(chamar(HttpMethod.GET, "/api/notas?status=AUTORIZADA", null, vendedor).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(chamar(HttpMethod.GET, "/api/notas?tipo=xyz", null, vendedor).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(chamar(HttpMethod.GET, "/api/notas/987654321/itens", null, vendedor).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void cancelamento() {
        NotaItem n = notaAutorizada("NFCE", Instant.now().minusSeconds(3600));
        String url = "/api/notas/" + n.getId() + "/cancelamento";
        Map<String, String> ok = Map.of("justificativa", "Cliente desistiu da compra após emissão");

        assertThat(chamar(HttpMethod.POST, url, ok, vendedor).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        var r = chamar(HttpMethod.POST, url, Map.of("justificativa", "   curta   "), admin);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody().get("campos").get(0).get("campo").asText()).isEqualTo("justificativa");

        r = chamar(HttpMethod.POST, url, ok, admin);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody().get("status").asText()).isEqualTo("processando");
        esperar("fiscal-events-queue", 1, m -> m.path("notaId").asLong() == n.getId()
                && "cancelamento".equals(m.path("eventType").asText()) && m.hasNonNull("eventoId")
                && n.getChaveAcesso().equals(m.path("chaveAcesso").asText()));

        assertThat(chamar(HttpMethod.POST, url, ok, admin).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        NotaItem velha = notaAutorizada("NFCE", Instant.now().minus(Duration.ofHours(25)));
        r = chamar(HttpMethod.POST, "/api/notas/" + velha.getId() + "/cancelamento", ok, admin);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(r.getBody().get("message").asText()).isEqualTo("Prazo de cancelamento expirado");
    }

    @Test
    void devolucao() {
        NotaItem original = notaAutorizada("NFE", Instant.now());
        Map<String, Object> pedido = Map.of("notaReferenciadaId", original.getId(), "motivo", "",
                "itens", List.of(Map.of("produtoId", 7, "quantidade", 2, "valorUnitario", 1.0)));
        var r = chamar(HttpMethod.POST, "/api/notas/devolucao", pedido, vendedor);
        assertThat(r.getStatusCode()).as(String.valueOf(r.getBody())).isEqualTo(HttpStatus.CREATED);
        JsonNode dev = r.getBody();
        assertThat(dev.get("tipo").asText()).isEqualTo("DEVOLUCAO");
        assertThat(dev.get("status").asText()).isEqualTo("processando");
        assertThat(dev.get("notaReferenciadaId").asLong()).isEqualTo(original.getId());
        assertThat(dev.get("clienteId").asLong()).isEqualTo(5);
        assertThat(dev.get("valorTotal").decimalValue()).isEqualByComparingTo("179.80"); // 2 × 89,90 da original
        var itens = chamar(HttpMethod.GET, "/api/notas/" + dev.get("id").asLong() + "/itens", null, vendedor).getBody();
        assertThat(itens.get(0).get("cfop").asText()).isEqualTo("1202");
        long devId = dev.get("id").asLong();
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(repo.porId(devId).orElseThrow().getStatus()).isEqualTo("autorizada"));
        esperar("inventory-updates-queue", 1, m -> m.path("referenciaId").asLong() == devId
                && "increase".equals(m.path("type").asText()) && "return".equals(m.path("referenciaType").asText())
                && m.path("quantidade").asLong() == 2);
        NotaLinha base = repo.itens(original.getId()).get(0);
        assertThat(base.getQuantidadeDevolvida()).isEqualTo(2);
        assertThat(base.getQuantidadeEmDevolucao()).isZero();

        // vendidas 3 do produto 7 (duas linhas), 2 em devolução: sobra 1
        r = chamar(HttpMethod.POST, "/api/notas/devolucao", Map.of("notaReferenciadaId", original.getId(),
                "itens", List.of(Map.of("produtoId", 7, "quantidade", 2))), vendedor);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(r.getBody().get("message").asText()).isEqualTo("Quantidade acima do saldo de P-7: pode devolver até 1");

        r = chamar(HttpMethod.POST, "/api/notas/devolucao", Map.of("notaReferenciadaId", original.getId(),
                "itens", List.of(Map.of("produtoId", 55, "quantidade", 1))), vendedor);
        assertThat(r.getBody().get("message").asText()).isEqualTo("Produto não consta na nota original");

        r = chamar(HttpMethod.POST, "/api/notas/devolucao", Map.of("notaReferenciadaId", original.getId(),
                "itens", List.of(Map.of("produtoId", 7, "quantidade", 0))), vendedor);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        r = chamar(HttpMethod.POST, "/api/notas/devolucao", Map.of("notaReferenciadaId", dev.get("id").asLong(),
                "itens", List.of(Map.of("produtoId", 7, "quantidade", 1))), vendedor);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void inutilizacao() {
        NotaItem emitida = notaAutorizada("NFE", Instant.now());
        String serie = emitida.getSerie();
        long num = Long.parseLong(emitida.getNumero());
        String just = "Numeração pulada por erro no sistema";

        var r = chamar(HttpMethod.POST, "/api/inutilizacoes", Map.of("serie", serie, "numeroInicial", num - 2,
                "numeroFinal", num + 2, "justificativa", just), admin);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(r.getBody().get("message").asText()).contains(emitida.getNumero());

        Map<String, Object> faixa = Map.of("serie", serie, "numeroInicial", num + 10, "numeroFinal", num + 15,
                "justificativa", just);
        assertThat(chamar(HttpMethod.POST, "/api/inutilizacoes", faixa, vendedor).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        r = chamar(HttpMethod.POST, "/api/inutilizacoes", faixa, admin);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(r.getBody().fieldNames()).toIterable().containsExactlyInAnyOrder("id", "serie", "numeroInicial",
                "numeroFinal", "justificativa", "protocolo", "registradaEm");
        assertThat(r.getBody().get("protocolo").isNull()).isTrue();
        long id = r.getBody().get("id").asLong();
        esperar("fiscal-events-queue", 1, m -> m.path("eventoId").asLong() == id
                && "inutilizacao".equals(m.path("eventType").asText()));

        r = chamar(HttpMethod.POST, "/api/inutilizacoes", Map.of("serie", serie, "numeroInicial", num + 15,
                "numeroFinal", num + 20, "justificativa", just), admin);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        r = chamar(HttpMethod.POST, "/api/inutilizacoes", Map.of("serie", serie, "numeroInicial", 9,
                "numeroFinal", 3, "justificativa", just), admin);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        r = chamar(HttpMethod.POST, "/api/inutilizacoes", Map.of("serie", serie, "numeroInicial", 1,
                "numeroFinal", 1, "justificativa", "curta"), admin);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        var lista = toList(chamar(HttpMethod.GET, "/api/inutilizacoes", null, vendedor).getBody());
        assertThat(lista.get(0).get("id").asLong()).isEqualTo(id);
    }

    @Test
    void reenviarSoRejeitada() {
        NotaItem n = notaAutorizada("NFCE", Instant.now());
        assertThat(chamar(HttpMethod.POST, "/api/notas/" + n.getId() + "/reenviar", null, vendedor).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        n.setStatus("rejeitada");
        n.setMotivoRejeicao("CPF inválido");
        repo.salvarSeStatus(n, "autorizada");
        var r = chamar(HttpMethod.POST, "/api/notas/" + n.getId() + "/reenviar", null, vendedor);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody().get("status").asText()).isEqualTo("processando");
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(repo.porId(n.getId()).orElseThrow().getStatus()).isEqualTo("autorizada"));
    }

    void enviarJob(long notaId) throws Exception {
        String url = sqs.getQueueUrl(r -> r.queueName("fiscal-jobs-queue")).queueUrl();
        String corpo = mapper.writeValueAsString(Map.of("jobType", "generate_xml", "notaId", notaId, "tipoNota", "NFCE"));
        sqs.sendMessage(r -> r.queueUrl(url).messageBody(corpo));
    }

    /** NFC-e "processando" com um item só: o valor escolhe o cenário do simulador (,04 ,13 ,50 ,77). */
    NotaItem notaProcessando(String ncm, String valor) {
        long id = VENDAS.incrementAndGet();
        NotaItem n = new NotaItem();
        n.setId(id);
        n.setTipo("NFCE");
        n.setModelo("65");
        n.setSerie(SERIE);
        n.setNumero(String.format("%06d", id % 1_000_000));
        n.setStatus("processando");
        n.setVendaId(id);
        n.setMetodoPagamento("dinheiro");
        n.setModalidade("fisica");
        n.setValorProdutos(new BigDecimal(valor));
        n.setValorDesconto(BigDecimal.ZERO);
        n.setValorTotal(new BigDecimal(valor));
        n.setEmitidaEm(Instant.now().toString());
        n.setTentativasEnvio(0);
        NotaLinha l = linha(id, 1, 7, 1, valor);
        l.setNcm(ncm);
        repo.criar(n, List.of(l), List.of());
        return n;
    }

    NotaItem esperarStatus(long id, String status) {
        await().atMost(Duration.ofSeconds(40)).untilAsserted(() ->
                assertThat(repo.porId(id).orElseThrow().getStatus()).isEqualTo(status));
        return repo.porId(id).orElseThrow();
    }

    @Test
    void rejeicaoDaSefaz() throws Exception {
        NotaItem n = notaProcessando("87083010", "10.04");
        enviarJob(n.getId());
        NotaItem r = esperarStatus(n.getId(), "rejeitada");
        assertThat(r.getCodigoStatus()).isEqualTo("204");
        assertThat(r.getMotivoRejeicao()).contains("Duplicidade");
        assertThat(mensagens("inventory-updates-queue")).noneMatch(m -> m.path("referenciaId").asLong() == n.getVendaId());
        // aviso "medium" não vai para o email-queue (o filtro da assinatura só passa high/critical)
    }

    @Test
    void loteEmProcessamentoConsultaORecibo() throws Exception {
        NotaItem n = notaProcessando("87083010", "10.13");
        enviarJob(n.getId());
        NotaItem r = esperarStatus(n.getId(), "autorizada");
        assertThat(r.getProtocolo()).isNotBlank();
    }

    @Test
    void sefazVoltaNaSegundaTentativa() throws Exception {
        // cenário do WireMock: 500 na primeira chamada com total ,77; depois cai no mapeamento padrão (100)
        String cenario = "{\"scenarioName\":\"volta-" + SERIE + "\",\"requiredScenarioState\":\"Started\","
                + "\"newScenarioState\":\"ok\",\"priority\":1,\"request\":{\"method\":\"POST\","
                + "\"urlPath\":\"/ws/NFeAutorizacao4\",\"bodyPatterns\":[{\"matches\":\"(?s).*<vNF>[0-9]+\\\\.77</vNF>.*\"}]},"
                + "\"response\":{\"status\":500}}";
        var req = java.net.http.HttpRequest.newBuilder(java.net.URI.create(SefazMockTeste.url() + "/__admin/mappings"))
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(cenario)).build();
        assertThat(java.net.http.HttpClient.newHttpClient().send(req, java.net.http.HttpResponse.BodyHandlers.ofString())
                .statusCode()).isEqualTo(201);

        NotaItem n = notaProcessando("87083010", "10.77");
        enviarJob(n.getId());
        NotaItem r = esperarStatus(n.getId(), "autorizada");
        assertThat(r.getTentativasEnvio()).isEqualTo(1);
    }

    @Test
    void sefazForaDoArVaiParaDlq() throws Exception {
        NotaItem n = notaProcessando("87083010", "10.50");
        enviarJob(n.getId());
        await().atMost(Duration.ofSeconds(60)).untilAsserted(() -> assertThat(mensagens("fiscal-dlq"))
                .anyMatch(m -> m.path("notaId").asLong() == n.getId() && m.has("xmlPath")));
        NotaItem r = repo.porId(n.getId()).orElseThrow();
        assertThat(r.getStatus()).isEqualTo("processando");
        assertThat(r.getTentativasEnvio()).isEqualTo(3);
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(mensagens("email-queue")).anyMatch(m ->
                "fiscal-error".equals(m.path("MessageType").asText()) && "high".equals(m.path("severity").asText())
                        && m.path("message").asText().contains("id " + n.getId())));
    }

    @Test
    void ncmInvalidoRejeitaNoXsd() throws Exception {
        NotaItem n = notaProcessando("8708301", "10.00");
        enviarJob(n.getId());
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(repo.porId(n.getId()).orElseThrow().getStatus()).isEqualTo("rejeitada"));
        NotaItem r = repo.porId(n.getId()).orElseThrow();
        assertThat(r.getCodigoStatus()).isEqualTo("XSD");
        assertThat(r.getMotivoRejeicao()).contains("8708301");
        assertThat(mensagens("sefaz-queue")).noneMatch(m -> m.path("notaId").asLong() == n.getId());
    }

    @Test
    void certificadoVencidoVaiParaDlqComAvisoCritico() throws Exception {
        String original = secrets.getSecretValue(r -> r.secretId("wc-sefaz-certificate")).secretString();
        secrets.putSecretValue(r -> r.secretId("wc-sefaz-certificate").secretString(CertificadosTeste.secret(true)));
        certificado.esquecer();
        try {
            NotaItem n = notaProcessando("87083010", "10.00");
            enviarJob(n.getId());
            await().atMost(Duration.ofSeconds(60)).untilAsserted(() -> assertThat(mensagens("fiscal-dlq"))
                    .anyMatch(m -> m.path("notaId").asLong() == n.getId()));
            assertThat(mensagens("email-queue")).anyMatch(m -> "fiscal-error".equals(m.path("MessageType").asText())
                    && "critical".equals(m.path("severity").asText())
                    && m.path("message").asText().contains("Nota " + n.getId()));
            assertThat(repo.porId(n.getId()).orElseThrow().getStatus()).isEqualTo("processando");
        } finally {
            secrets.putSecretValue(r -> r.secretId("wc-sefaz-certificate").secretString(original));
            certificado.esquecer();
        }
    }
}

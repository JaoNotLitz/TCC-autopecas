package br.com.wcautopecas.entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import br.com.wcautopecas.commons.mensageria.EventPublisher;
import br.com.wcautopecas.commons.seguranca.JwtService;
import br.com.wcautopecas.commons.seguranca.Papel;
import br.com.wcautopecas.commons.seguranca.SessaoStore;
import br.com.wcautopecas.entry.api.NotaFiscalDto;
import br.com.wcautopecas.entry.entrada.EntradaRepository;
import br.com.wcautopecas.entry.entrada.ItemEntrada;
import br.com.wcautopecas.entry.entrada.NotaEntrada;
import br.com.wcautopecas.entry.integracao.Integracoes;
import br.com.wcautopecas.entry.integracao.Integracoes.CodigoProduto;
import br.com.wcautopecas.entry.integracao.Integracoes.NotaEntradaFiscal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;

/** HU-B11.3 e EPB-12: importação assíncrona do XML pelo XmlProcessorListener, na LocalStack. */
class ImportacaoXmlIT extends BaseIT {

    static final long PF_001 = 501;
    static final long XYZ_999 = 999;
    static final AtomicLong NUMEROS = new AtomicLong(500_000 + System.currentTimeMillis() % 100_000 * 1000);
    static final AtomicLong NOTAS = new AtomicLong(70_000);
    /** Mensagens já lidas das filas, por fila (o teste apaga ao ler). */
    static final Map<String, List<Message>> LIDAS = new java.util.concurrent.ConcurrentHashMap<>();

    @Autowired
    TestRestTemplate http;
    @Autowired
    JwtService jwt;
    @Autowired
    SessaoStore sessoes;
    @Autowired
    SqsClient sqs;
    @Autowired
    S3Client s3;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    EntradaRepository repo;
    @Autowired
    EventPublisher publisher;
    @Autowired
    br.com.wcautopecas.commons.dynamo.IdGenerator ids;

    String token;
    /** Códigos que o "catálogo" conhece neste teste. */
    Set<String> catalogo = Set.of("PF-001");

    @BeforeEach
    void preparar() {
        var t = jwt.emitir(3, Papel.VENDEDOR);
        sessoes.criar(3, t.token(), Duration.ofMinutes(10));
        token = t.token();
        given(integracoes.produtosPorCodigo(anyList())).willAnswer(inv -> {
            List<String> codigos = inv.getArgument(0);
            return codigos.stream().map(c -> catalogo.contains(c)
                    ? new CodigoProduto(c, "PF-001".equals(c) ? PF_001 : XYZ_999, "Peça", true)
                    : new CodigoProduto(c, null, null, false)).toList();
        });
        given(integracoes.registrarNota(any())).willAnswer(inv -> {
            NotaEntradaFiscal n = inv.getArgument(0);
            return new NotaFiscalDto(NOTAS.incrementAndGet(), "ENTRADA", n.numero(), n.serie(), n.chaveAcesso(), null,
                    null, null, n.fornecedorId(), BigDecimal.TEN, n.dataEmissao().toString(), "autorizada", null, null);
        });
    }

    // ---------- cenários

    @Test
    void xmlComTodosOsItensCadastradosFicaRegistrado() throws Exception {
        catalogo = Set.of("PF-001", "XYZ-999");
        long numero = NUMEROS.incrementAndGet();
        String chave = EntradaIT.chave(numero);

        ResponseEntity<JsonNode> r = enviar(xml(numero, chave));
        assertThat(r.getStatusCode()).as(String.valueOf(r.getBody())).isEqualTo(HttpStatus.ACCEPTED);
        long id = r.getBody().get("uploadId").asLong();
        String key = r.getBody().get("s3Key").asText();
        assertThat(key).isEqualTo("fornecedor/" + EntradaIT.CNPJ + "/2026/09/" + chave + ".xml");
        assertThat(r.getBody().get("message").asText()).isNotBlank();
        assertThat(s3.getObjectAsBytes(b -> b.bucket("wc-xml-storage").key(key)).asUtf8String()).contains(chave);

        NotaEntrada e = aguardar(id, "registrada");
        assertThat(e.getOrigem()).isEqualTo("xml");
        assertThat(e.getNumero()).isEqualTo(String.valueOf(numero));
        assertThat(e.getChaveAcesso()).isEqualTo(chave);
        assertThat(e.getNotaFiscalId()).isNotNull();
        assertThat(e.getItens()).extracting(ItemEntrada::getVinculado).containsOnly(true);
        assertThat(e.getDataEntrada()).isEqualTo(java.time.LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo"))
                .toString());

        List<JsonNode> aumentos = aumentos(id, 2);
        assertThat(aumentos).extracting(m -> m.get("produtoId").asLong()).containsExactlyInAnyOrder(PF_001, XYZ_999);
        assertThat(aumentos).extracting(m -> m.get("referenciaType").asText()).containsOnly("entry");
    }

    @Test
    void itemDesconhecidoDeixaPendenciaESoSomaOsVinculados() throws Exception {
        long numero = NUMEROS.incrementAndGet();
        long id = enviar(xml(numero, EntradaIT.chave(numero))).getBody().get("uploadId").asLong();

        NotaEntrada e = aguardar(id, "com_pendencias");
        ItemEntrada desconhecido = e.getItens().stream().filter(i -> "XYZ-999".equals(i.getCodigo())).findFirst()
                .orElseThrow();
        assertThat(desconhecido.getVinculado()).isFalse();
        assertThat(desconhecido.getProdutoId()).isNull();

        List<JsonNode> aumentos = aumentos(id, 1);
        assertThat(aumentos.get(0).get("produtoId").asLong()).isEqualTo(PF_001);
        assertThat(aumentos.get(0).get("quantidade").asLong()).isEqualTo(100);

        // A nota ENTRADA do fiscal leva só o item com produto.
        ArgumentCaptor<NotaEntradaFiscal> nota = ArgumentCaptor.forClass(NotaEntradaFiscal.class);
        verify(integracoes, org.mockito.Mockito.atLeastOnce()).registrarNota(nota.capture());
        NotaEntradaFiscal enviada = nota.getAllValues().stream().filter(n -> n.entradaId() == id).findFirst()
                .orElseThrow();
        assertThat(enviada.itens()).extracting(Integracoes.ItemNota::codigo).containsExactly("PF-001");
    }

    @Test
    void xmlMalformadoViraErroEVaiParaADlq() {
        String quebrado = "<?xml version=\"1.0\"?><nfeProc xmlns=\"http://www.portalfiscal.inf.br/nfe\"><NFe><infNFe>";
        ResponseEntity<JsonNode> r = enviar(quebrado.getBytes(StandardCharsets.UTF_8));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        long id = r.getBody().get("uploadId").asLong();
        assertThat(r.getBody().get("s3Key").asText()).startsWith("fornecedor/desconhecido/");

        NotaEntrada e = aguardar(id, "erro");
        assertThat(e.getErro()).isEqualTo("XML inválido ou não é NF-e");

        Message naDlq = aguardarNaDlq(id);
        assertThat(naDlq.messageAttributes().get(EventPublisher.ORIGEM_FILA).stringValue())
                .isEqualTo("entry-events-queue");
        assertThat(json(naDlq.body()).get("erro").asText()).isEqualTo("XML inválido ou não é NF-e");
    }

    @Test
    void dadosInvalidosNoXmlViramErroComOMotivo() throws Exception {
        long numero = NUMEROS.incrementAndGet();
        String xml = new String(xml(numero, EntradaIT.chave(numero)), StandardCharsets.UTF_8)
                .replace("<NCM>87083010</NCM>", "<NCM>123</NCM>");
        long id = enviar(xml.getBytes(StandardCharsets.UTF_8)).getBody().get("uploadId").asLong();

        NotaEntrada e = aguardar(id, "erro");
        assertThat(e.getErro()).containsIgnoringCase("NCM");
        aguardarNaDlq(id);
        verify(integracoes, org.mockito.Mockito.never()).registrarNota(org.mockito.ArgumentMatchers
                .argThat(n -> n.entradaId() == id));
    }

    @Test
    void reentregaNaoSomaEstoqueDeNovo() throws Exception {
        long numero = NUMEROS.incrementAndGet();
        ResponseEntity<JsonNode> r = enviar(xml(numero, EntradaIT.chave(numero)));
        long id = r.getBody().get("uploadId").asLong();
        aguardar(id, "com_pendencias");
        aumentos(id, 1);

        publisher.enviarFila("entry-events-queue", "entry.uploaded",
                Map.of("eventType", "entry.uploaded", "entryId", id, "s3Key", r.getBody().get("s3Key").asText()));

        // Dá tempo de a reentrega ser consumida e confere que nenhum increase novo apareceu.
        await().pollDelay(Duration.ofSeconds(4)).atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(filaVazia("entry-events-queue")).isTrue());
        assertThat(aumentosAgora(id)).hasSize(1);
    }

    @Test
    void chaveJaRegistradaPorOutraEntradaViraErroSemDlq() throws Exception {
        long numero = NUMEROS.incrementAndGet();
        String chave = EntradaIT.chave(numero);
        ResponseEntity<JsonNode> r = enviar(xml(numero, chave));
        aguardar(r.getBody().get("uploadId").asLong(), "com_pendencias");

        // O mesmo XML de novo pelo upload responde 409...
        assertThat(enviar(xml(numero, chave)).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // ...e uma segunda entrada que chegue à fila com ele fica em erro, sem ir para a DLQ.
        NotaEntrada outra = new NotaEntrada();
        outra.setId(ids.proximo("entradas"));
        outra.setStatus("recebida");
        outra.setOrigem("xml");
        outra.setS3Key(r.getBody().get("s3Key").asText());
        repo.criar(outra);
        publisher.enviarFila("entry-events-queue", "entry.uploaded",
                Map.of("eventType", "entry.uploaded", "entryId", outra.getId(), "s3Key", outra.getS3Key()));

        NotaEntrada e = aguardar(outra.getId(), "erro");
        assertThat(e.getErro()).startsWith("Chave já registrada");
        assertThat(mensagensDlq(outra.getId())).isEmpty();
    }

    @Test
    void arquivoQueNaoEXmlResponde400() {
        assertThat(enviar("não é xml".getBytes(StandardCharsets.UTF_8)).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ---------- apoio

    ResponseEntity<JsonNode> enviar(byte[] conteudo) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.MULTIPART_FORM_DATA);
        var corpo = new LinkedMultiValueMap<String, Object>();
        corpo.add("arquivo", new ByteArrayResource(conteudo) {
            @Override
            public String getFilename() {
                return "nota.xml";
            }
        });
        return http.exchange("/api/notas/entrada/importar-xml/lote", HttpMethod.POST, new HttpEntity<>(corpo, h),
                JsonNode.class);
    }

    static byte[] xml(long numero, String chave) throws Exception {
        String xml = new String(EntradaIT.fixture(), StandardCharsets.UTF_8)
                .replace("35260911222333000181550010000123451123456784", chave)
                .replace("<nNF>12345</nNF>", "<nNF>" + numero + "</nNF>");
        return xml.getBytes(StandardCharsets.UTF_8);
    }

    NotaEntrada aguardar(long id, String status) {
        await("entrada " + id + " " + status).atMost(Duration.ofSeconds(30)).until(() ->
                repo.porId(id).map(e -> status.equals(e.getStatus())).orElse(false));
        return repo.porId(id).orElseThrow();
    }

    /** Lê a fila, guarda o que leu e apaga. */
    List<Message> ler(String fila) {
        String url = sqs.getQueueUrl(r -> r.queueName(fila)).queueUrl();
        List<Message> lidas = LIDAS.computeIfAbsent(fila, f -> new java.util.concurrent.CopyOnWriteArrayList<>());
        for (Message m : sqs.receiveMessage(r -> r.queueUrl(url).maxNumberOfMessages(10).waitTimeSeconds(1)
                .messageAttributeNames("All")).messages()) {
            lidas.add(m);
            sqs.deleteMessage(r -> r.queueUrl(url).receiptHandle(m.receiptHandle()));
        }
        return lidas;
    }

    List<JsonNode> aumentosAgora(long entradaId) {
        List<JsonNode> achados = new ArrayList<>();
        for (Message m : ler("inventory-updates-queue")) {
            JsonNode j = json(m.body());
            if (j.path("referenciaId").asLong() == entradaId && "increase".equals(j.path("type").asText())) {
                achados.add(j);
            }
        }
        return achados;
    }

    List<JsonNode> aumentos(long entradaId, int quantidade) {
        await("increase da entrada " + entradaId).atMost(Duration.ofSeconds(20))
                .until(() -> aumentosAgora(entradaId).size() >= quantidade);
        List<JsonNode> achados = aumentosAgora(entradaId);
        assertThat(achados).hasSize(quantidade);
        return achados;
    }

    List<Message> mensagensDlq(long entradaId) {
        return ler("fiscal-dlq").stream().filter(m -> json(m.body()).path("entryId").asLong() == entradaId).toList();
    }

    Message aguardarNaDlq(long entradaId) {
        await("entrada " + entradaId + " na DLQ").atMost(Duration.ofSeconds(20))
                .until(() -> !mensagensDlq(entradaId).isEmpty());
        return mensagensDlq(entradaId).get(0);
    }

    boolean filaVazia(String fila) {
        String url = sqs.getQueueUrl(r -> r.queueName(fila)).queueUrl();
        Map<String, String> a = sqs.getQueueAttributes(r -> r.queueUrl(url).attributeNamesWithStrings(
                "ApproximateNumberOfMessages", "ApproximateNumberOfMessagesNotVisible")).attributesAsStrings();
        return "0".equals(a.get("ApproximateNumberOfMessages"))
                && "0".equals(a.get("ApproximateNumberOfMessagesNotVisible"));
    }

    JsonNode json(String s) {
        try {
            return mapper.readTree(s);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

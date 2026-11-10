package br.com.wcautopecas.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import br.com.wcautopecas.commons.seguranca.JwtService;
import br.com.wcautopecas.commons.seguranca.Papel;
import br.com.wcautopecas.commons.seguranca.SessaoStore;
import br.com.wcautopecas.commons.teste.InfraTeste;
import br.com.wcautopecas.document.danfe.DanfeGeradorTest;
import br.com.wcautopecas.document.nota.Nota;
import br.com.wcautopecas.document.nota.NotaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "wc.documentos.contador-email=contador@exemplo.com.br")
@ActiveProfiles("test")
class DocumentoIT {

    static final AtomicLong IDS = new AtomicLong(System.currentTimeMillis() % 100_000 * 100);
    static final List<JsonNode> EMAILS = new ArrayList<>();

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
    DynamoDbEnhancedClient enhanced;
    @Autowired
    S3Client s3;
    @Autowired
    SqsClient sqs;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    NotaRepository repo;
    @Autowired
    DocumentosProperties props;

    String token;

    @BeforeEach
    void preparar() {
        var t = jwt.emitir(1, Papel.ADMIN);
        sessoes.criar(1, t.token(), Duration.ofMinutes(10));
        token = t.token();
    }

    /** Grava a nota como o fiscal-service faria (item + XML no S3) e devolve o id. */
    long nota(String status, String fixture, Instant emitidaEm) throws Exception {
        long id = IDS.incrementAndGet();
        String pasta = fixture.startsWith("nfce") ? "NFCE" : "NFE";
        String chave = "35261112345678000195" + (pasta.equals("NFCE") ? "65" : "55") + "001"
                + String.format("%09d", id % 1_000_000_000L) + "1" + "1234567" + (id % 10);
        String key = pasta + "/2026/11/" + chave + ".xml";
        s3.putObject(b -> b.bucket(props.getBucketDocumentos()).key(key), RequestBody.fromBytes(xml(fixture)));
        if ("cancelada".equals(status)) {
            s3.putObject(b -> b.bucket(props.getBucketDocumentos()).key(key.replace(".xml", "-cancelamento.xml")),
                    RequestBody.fromString("<procEventoNFe/>"));
        }
        Nota n = new Nota();
        n.setId(id);
        n.setTipo(pasta);
        n.setModelo(pasta.equals("NFCE") ? "65" : "55");
        n.setSerie("1");
        n.setNumero(String.valueOf(id));
        n.setChaveAcesso(chave);
        n.setStatus(status);
        n.setValorTotal(new BigDecimal("224.80"));
        n.setEmitidaEm(emitidaEm.toString());
        n.setXmlPath("processando".equals(status) ? null : "s3://" + props.getBucketDocumentos() + "/" + key);
        enhanced.table(props.getTabelaNotas(), TableSchema.fromBean(Nota.class)).putItem(n);
        return id;
    }

    static byte[] xml(String fixture) throws Exception {
        return DanfeGeradorTest.fixture(fixture);
    }

    <T> ResponseEntity<T> chamar(HttpMethod metodo, String url, Class<T> tipo) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return http.exchange(url, metodo, new HttpEntity<>(h), tipo);
    }

    List<JsonNode> emails() {
        String url = sqs.getQueueUrl(r -> r.queueName(props.getFilaEmail())).queueUrl();
        for (var m : sqs.receiveMessage(r -> r.queueUrl(url).maxNumberOfMessages(10).waitTimeSeconds(1)).messages()) {
            try {
                EMAILS.add(mapper.readTree(m.body()));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            sqs.deleteMessage(r -> r.queueUrl(url).receiptHandle(m.receiptHandle()));
        }
        return EMAILS;
    }

    @Test
    void xmlDevolveONfeProcComoAnexo() throws Exception {
        long id = nota("autorizada", "nfe-proc.xml", Instant.now());
        var r = chamar(HttpMethod.GET, "/api/notas/" + id + "/xml", byte[].class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_XML);
        assertThat(r.getHeaders().getContentDisposition().getFilename()).startsWith("NFe35").endsWith(".xml");
        assertThat(r.getBody()).isEqualTo(xml("nfe-proc.xml"));
    }

    @Test
    void xmlInexistenteOuAindaNaoEmitido() throws Exception {
        assertThat(chamar(HttpMethod.GET, "/api/notas/999999999/xml", String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        long id = nota("processando", "nfe-proc.xml", Instant.now());
        var r = chamar(HttpMethod.GET, "/api/notas/" + id + "/xml", JsonNode.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(r.getBody().path("message").asText()).isEqualTo("XML ainda não disponível");
    }

    @Test
    void pdfGeradoNaHoraFicaSalvoAoLadoDoXml() throws Exception {
        long id = nota("autorizada", "nfce-proc.xml", Instant.now());
        var r = chamar(HttpMethod.GET, "/api/notas/" + id + "/pdf?inline=true", byte[].class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(r.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).startsWith("inline");
        assertThat(new String(r.getBody(), 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");

        Nota salva = repo.porId(id).orElseThrow();
        assertThat(salva.getPdfPath()).isEqualTo(salva.getXmlPath().replace(".xml", ".pdf"));
        String key = salva.getPdfPath().substring(("s3://" + props.getBucketDocumentos() + "/").length());
        byte[] noS3 = s3.getObjectAsBytes(b -> b.bucket(props.getBucketDocumentos()).key(key)).asByteArray();
        assertThat(noS3).isEqualTo(r.getBody());

        // segunda chamada usa o arquivo salvo
        assertThat(chamar(HttpMethod.GET, "/api/notas/" + id + "/pdf", byte[].class).getBody()).isEqualTo(noS3);
    }

    @Test
    void canceladaSaiComCarimbo() throws Exception {
        long id = nota("cancelada", "nfe-proc.xml", Instant.now().minus(40, ChronoUnit.DAYS));
        var r = chamar(HttpMethod.GET, "/api/notas/" + id + "/pdf", byte[].class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getHeaders().getContentDisposition().isAttachment()).isTrue();
        assertThat(DanfeGeradorTest.texto(r.getBody())).contains("CANCELADA");
    }

    @Test
    void pdfDeNotaNaoAutorizada() throws Exception {
        long id = nota("processando", "nfe-proc.xml", Instant.now());
        var r = chamar(HttpMethod.GET, "/api/notas/" + id + "/pdf", JsonNode.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(chamar(HttpMethod.GET, "/api/notas/999999999/pdf", String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(http.getForEntity("/api/notas/" + id + "/pdf", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void notaAutorizadaGeraDanfePelaFila() throws Exception {
        long id = nota("autorizada", "nfe-proc.xml", Instant.now().minus(40, ChronoUnit.DAYS));
        String url = sqs.getQueueUrl(r -> r.queueName(props.getFilaDocumentos())).queueUrl();
        Map<String, Object> evento = new HashMap<>();
        evento.put("eventType", "nota.autorizada");
        evento.put("notaId", id);
        String corpo = mapper.writeValueAsString(evento);
        sqs.sendMessage(r -> r.queueUrl(url).messageBody(corpo));
        sqs.sendMessage(r -> r.queueUrl(url).messageBody(corpo)); // repetido: idempotente
        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> assertThat(repo.porId(id).orElseThrow().getPdfPath()).isNotNull());
    }

    @Test
    void loteDoContador() throws Exception {
        long a = nota("autorizada", "nfe-proc.xml", Instant.now());
        long c = nota("cancelada", "nfce-proc.xml", Instant.now());
        long antiga = nota("autorizada", "nfe-proc.xml", Instant.now().minus(40, ChronoUnit.DAYS));
        emails();
        int antes = EMAILS.size();

        assertThat(chamar(HttpMethod.POST, "/api/notas/lote-contador", Void.class).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        List<JsonNode> achados = new ArrayList<>();
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            achados.clear();
            List<JsonNode> todos = emails();
            todos.subList(antes, todos.size()).stream()
                    .filter(e -> "contador-lote".equals(e.path("template").asText())).forEach(achados::add);
            assertThat(achados).hasSize(1);
        });
        JsonNode email = achados.get(0);
        assertThat(email.path("to").asText()).isEqualTo("contador@exemplo.com.br");
        assertThat(email.path("data").path("validadeDias").asInt()).isEqualTo(7);

        // o link pré-assinado baixa o ZIP de verdade
        HttpResponse<byte[]> zip = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(email.path("data").path("link").asText())).build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertThat(zip.statusCode()).isEqualTo(200);
        Map<String, String> arquivos = new HashMap<>();
        try (var z = new ZipInputStream(new ByteArrayInputStream(zip.body()))) {
            for (var e = z.getNextEntry(); e != null; e = z.getNextEntry()) {
                arquivos.put(e.getName(), new String(z.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        String chaveA = repo.porId(a).orElseThrow().getChaveAcesso();
        String chaveC = repo.porId(c).orElseThrow().getChaveAcesso();
        String chaveAntiga = repo.porId(antiga).orElseThrow().getChaveAcesso();
        assertThat(arquivos).containsKeys(chaveA + ".xml", chaveC + ".xml", chaveC + "-cancelamento.xml",
                "resumo.csv").doesNotContainKey(chaveAntiga + ".xml");
        assertThat(arquivos.get("resumo.csv")).startsWith("numero;serie;chave;tipo;status;valor")
                .contains(chaveA + ";NFE;autorizada;224,80").contains(chaveC + ";NFCE;cancelada");

        assertThat(repo.porId(a).orElseThrow().getEnviadaContadorEm()).isNotNull();
        assertThat(repo.porId(antiga).orElseThrow().getEnviadaContadorEm()).isNull();

        var de_novo = chamar(HttpMethod.POST, "/api/notas/lote-contador", JsonNode.class);
        assertThat(de_novo.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(de_novo.getBody().path("message").asText()).isEqualTo("Nenhuma nota pendente de envio neste mês");
    }

    @Test
    void loteGrandeSaiEmPartes() throws Exception {
        int maximo = props.getMaximoPorLote();
        props.setMaximoPorLote(2);
        try {
            List<Long> ids = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                ids.add(nota("autorizada", "nfe-proc.xml", Instant.now()));
            }
            emails();
            int antes = EMAILS.size();
            assertThat(chamar(HttpMethod.POST, "/api/notas/lote-contador", Void.class).getStatusCode())
                    .isEqualTo(HttpStatus.NO_CONTENT);
            List<JsonNode> partes = new ArrayList<>();
            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
                partes.clear();
                List<JsonNode> todos = emails();
                todos.subList(antes, todos.size()).stream()
                        .filter(e -> "contador-lote".equals(e.path("template").asText())).forEach(partes::add);
                assertThat(partes).isNotEmpty();
                assertThat(partes).hasSize(partes.get(0).path("data").path("partes").asInt());
            });
            assertThat(partes.size()).isGreaterThanOrEqualTo(2);
            assertThat(partes).allSatisfy(e -> assertThat(e.path("data").path("quantidade").asInt()).isLessThanOrEqualTo(2));
            assertThat(partes.get(0).path("subject").asText()).contains("(parte 1 de " + partes.size() + ")");
            ids.forEach(id -> assertThat(repo.porId(id).orElseThrow().getEnviadaContadorEm()).isNotNull());
        } finally {
            props.setMaximoPorLote(maximo);
        }
    }

    @Test
    void semEmailDoContadorConfigurado() {
        String antes = props.getContadorEmail();
        props.setContadorEmail("");
        try {
            var r = chamar(HttpMethod.POST, "/api/notas/lote-contador", JsonNode.class);
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(r.getBody().path("message").asText()).isEqualTo("E-mail do contador não configurado");
        } finally {
            props.setContadorEmail(antes);
        }
    }
}

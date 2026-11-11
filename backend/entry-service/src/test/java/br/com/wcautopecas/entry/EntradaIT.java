package br.com.wcautopecas.entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import br.com.wcautopecas.commons.erro.ApiException;
import br.com.wcautopecas.commons.seguranca.JwtService;
import br.com.wcautopecas.commons.seguranca.Papel;
import br.com.wcautopecas.commons.seguranca.SessaoStore;
import br.com.wcautopecas.commons.validacao.Documentos;
import br.com.wcautopecas.entry.api.NotaFiscalDto;
import br.com.wcautopecas.entry.entrada.EntradaRepository;
import br.com.wcautopecas.entry.integracao.Integracoes;
import br.com.wcautopecas.entry.integracao.Integracoes.CodigoProduto;
import br.com.wcautopecas.entry.integracao.Integracoes.NotaEntradaFiscal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
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
import software.amazon.awssdk.services.sqs.SqsClient;

class EntradaIT extends BaseIT {

    static final String CNPJ = "11222333000181";
    static final long PF_001 = 501;
    static final Map<String, List<JsonNode>> LIDAS = new ConcurrentHashMap<>();
    static final AtomicLong NUMEROS = new AtomicLong(System.currentTimeMillis() % 100_000 * 1000);
    static final AtomicLong NOTAS = new AtomicLong(9000);

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
    EntradaRepository repo;

    String token;

    @BeforeEach
    void preparar() {
        var t = jwt.emitir(3, Papel.VENDEDOR);
        sessoes.criar(3, t.token(), Duration.ofMinutes(10));
        token = t.token();
        given(integracoes.produtosPorCodigo(anyList())).willAnswer(inv -> {
            List<String> codigos = inv.getArgument(0);
            return codigos.stream().map(c -> "PF-001".equals(c)
                    ? new CodigoProduto(c, PF_001, "Pastilha", true) : new CodigoProduto(c, null, null, false)).toList();
        });
        given(integracoes.registrarNota(any())).willAnswer(inv -> {
            NotaEntradaFiscal n = inv.getArgument(0);
            BigDecimal total = n.itens().stream()
                    .map(i -> i.valorUnitario().multiply(BigDecimal.valueOf(i.quantidade())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new NotaFiscalDto(NOTAS.incrementAndGet(), "ENTRADA", n.numero(), n.serie(), n.chaveAcesso(), null,
                    null, null, n.fornecedorId(), total, n.dataEmissao().toString(), "autorizada", null, null);
        });
    }

    /** Chave válida (DV certo) com número único por teste. */
    static String chave(long numero) {
        String base = "35" + "2609" + CNPJ + "55" + "001" + String.format("%09d", numero) + "1" + "12345678";
        return base + Documentos.dvChave(base);
    }

    static Map<String, Object> entrada(String chave, long numero, List<Map<String, Object>> itens) {
        Map<String, Object> m = new HashMap<>();
        m.put("fornecedor", Map.of("cnpj", CNPJ, "razaoSocial", "Distribuidora Freios Brasil LTDA",
                "ie", "244123456119", "uf", "SP"));
        m.put("numero", String.valueOf(numero));
        m.put("serie", "1");
        m.put("dataEmissao", "2026-09-20");
        m.put("dataEntrada", "2026-09-21");
        m.put("chaveAcesso", chave);
        m.put("naturezaOperacao", "Compra para comercialização");
        m.put("itens", itens);
        return m;
    }

    static Map<String, Object> item(Long produtoId, String codigo, String cfop, long qtd, double valor) {
        Map<String, Object> m = new HashMap<>();
        m.put("produtoId", produtoId);
        m.put("codigo", codigo);
        m.put("descricao", "Peça " + codigo);
        m.put("ncm", "87083010");
        m.put("cfop", cfop);
        m.put("quantidade", qtd);
        m.put("valorUnitario", valor);
        return m;
    }

    ResponseEntity<JsonNode> chamar(HttpMethod metodo, String url, Object corpo) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return http.exchange(url, metodo, new HttpEntity<>(corpo, h), JsonNode.class);
    }

    ResponseEntity<JsonNode> enviarXml(byte[] conteudo) {
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
        return http.exchange("/api/notas/entrada/importar-xml", HttpMethod.POST, new HttpEntity<>(corpo, h),
                JsonNode.class);
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

    static byte[] fixture() throws Exception {
        try (var in = EntradaIT.class.getResourceAsStream("/xml/nfe-fornecedor.xml")) {
            return in.readAllBytes();
        }
    }

    @Test
    void entradaValidaSomaEstoqueEDevolveNotaFiscal() {
        long numero = NUMEROS.incrementAndGet();
        String chave = chave(numero);
        var r = chamar(HttpMethod.POST, "/api/notas/entrada", entrada(chave, numero, List.of(
                item(null, "PF-001", "1102", 100, 45.5), item(7L, "P-7", "2102", 5, 10.0))));
        assertThat(r.getStatusCode()).as(String.valueOf(r.getBody())).isEqualTo(HttpStatus.CREATED);
        JsonNode nota = r.getBody();
        assertThat(nota.fieldNames()).toIterable().containsExactlyInAnyOrder("id", "tipo", "numero", "serie",
                "chaveAcesso", "clienteId", "clienteNome", "clienteDocumento", "fornecedorId", "valorTotal",
                "emitidaEm", "status", "vendaId", "notaReferenciadaId");
        assertThat(nota.get("tipo").asText()).isEqualTo("ENTRADA");
        assertThat(nota.get("status").asText()).isEqualTo("autorizada");
        assertThat(nota.get("fornecedorId").asLong()).isPositive();

        var enviada = ArgumentCaptor.forClass(NotaEntradaFiscal.class);
        verify(integracoes).registrarNota(enviada.capture());
        assertThat(enviada.getValue().itens()).extracting(Integracoes.ItemNota::produtoId).containsExactly(PF_001, 7L);
        assertThat(enviada.getValue().dataEmissao()).isEqualTo(LocalDate.of(2026, 9, 20));

        var gravada = repo.porChave(chave).orElseThrow();
        assertThat(gravada.getStatus()).isEqualTo("registrada");
        assertThat(gravada.getOrigem()).isEqualTo("manual");
        assertThat(gravada.getFornecedorCnpj()).isEqualTo(CNPJ);
        assertThat(gravada.getValorTotal()).isEqualByComparingTo("4600.00");
        assertThat(gravada.getNotaFiscalId()).isEqualTo(nota.get("id").asLong());
        assertThat(gravada.getItens()).hasSize(2);
        assertThat(gravada.getUsuarioId()).isEqualTo(3);

        var ops = esperar("inventory-updates-queue", 2, m -> m.path("referenciaId").asLong() == gravada.getId()
                && "entry".equals(m.path("referenciaType").asText()));
        assertThat(ops).allMatch(m -> "increase".equals(m.path("type").asText()));
        assertThat(ops).anyMatch(m -> m.path("produtoId").asLong() == PF_001 && m.path("quantidade").asLong() == 100);

        // mesmo fornecedor numa segunda nota: mesmo id, dados atualizados
        long n2 = NUMEROS.incrementAndGet();
        Map<String, Object> outra = entrada(chave(n2), n2, List.of(item(7L, "P-7", "1102", 1, 3.0)));
        outra.put("fornecedor", Map.of("cnpj", CNPJ, "razaoSocial", "Freios Brasil Distribuidora", "uf", "SP"));
        var r2 = chamar(HttpMethod.POST, "/api/notas/entrada", outra);
        assertThat(r2.getBody().get("fornecedorId").asLong()).isEqualTo(nota.get("fornecedorId").asLong());

        var lista = chamar(HttpMethod.GET, "/api/notas/entrada", null).getBody();
        assertThat(lista.get(0).get("chaveAcesso").asText()).isEqualTo(chave(n2));
        assertThat(lista.get(0).get("fornecedorNome").asText()).isEqualTo("Freios Brasil Distribuidora");
        assertThat(chamar(HttpMethod.GET, "/api/notas/entrada/" + gravada.getId(), null).getBody()
                .get("itens").get(0).get("codigo").asText()).isEqualTo("PF-001");
    }

    @Test
    void codigoDesconhecidoResponde422() {
        long numero = NUMEROS.incrementAndGet();
        var r = chamar(HttpMethod.POST, "/api/notas/entrada", entrada(chave(numero), numero, List.of(
                item(null, "PF-001", "1102", 1, 45.5), item(null, "XYZ-999", "1102", 2, 10.0))));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(r.getBody().get("message").asText()).contains("XYZ-999").doesNotContain("PF-001");
        assertThat(r.getBody().get("campos").get(0).get("campo").asText()).isEqualTo("itens[1].codigo");
        assertThat(r.getBody().get("campos").get(0).get("message").asText()).isEqualTo("Produto não cadastrado");
        verify(integracoes, never()).registrarNota(any());
    }

    @Test
    void validacoes() {
        long numero = NUMEROS.incrementAndGet();
        String chave = chave(numero);
        List<Map<String, Object>> itens = List.of(item(7L, "P-7", "1102", 1, 10.0));

        Map<String, Object> datas = entrada(chave, numero, itens);
        datas.put("dataEntrada", "2026-09-19");
        var r = chamar(HttpMethod.POST, "/api/notas/entrada", datas);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody().get("campos").get(0).get("campo").asText()).isEqualTo("dataEntrada");

        r = chamar(HttpMethod.POST, "/api/notas/entrada", entrada(chave, numero,
                List.of(item(7L, "P-7", "5102", 1, 10.0))));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody().get("campos").get(0).get("campo").asText()).isEqualTo("itens[0].cfop");

        r = chamar(HttpMethod.POST, "/api/notas/entrada", entrada(chave.substring(0, 43) + "0".repeat(1)
                .replace("0", String.valueOf((Documentos.dvChave(chave.substring(0, 43)) + 1) % 10)), numero, itens));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody().get("campos").get(0).get("campo").asText()).isEqualTo("chaveAcesso");

        Map<String, Object> cnpj = entrada(chave, numero, itens);
        cnpj.put("fornecedor", Map.of("cnpj", "11222333000100", "razaoSocial", "X"));
        r = chamar(HttpMethod.POST, "/api/notas/entrada", cnpj);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody().get("campos").get(0).get("campo").asText()).isEqualTo("fornecedor.cnpj");

        r = chamar(HttpMethod.POST, "/api/notas/entrada", entrada(chave, numero, List.of()));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(integracoes, never()).registrarNota(any());
    }

    @Test
    void chaveRepetidaResponde409() {
        long numero = NUMEROS.incrementAndGet();
        Map<String, Object> in = entrada(chave(numero), numero, List.of(item(7L, "P-7", "1102", 1, 10.0)));
        assertThat(chamar(HttpMethod.POST, "/api/notas/entrada", in).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var r = chamar(HttpMethod.POST, "/api/notas/entrada", in);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(r.getBody().get("message").asText()).contains("já registrada");
    }

    @Test
    void fiscalForaDoArNaoGravaNada() {
        org.mockito.Mockito.doThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Serviço fiscal indisponível"))
                .when(integracoes).registrarNota(any());
        long numero = NUMEROS.incrementAndGet();
        String chave = chave(numero);
        var r = chamar(HttpMethod.POST, "/api/notas/entrada", entrada(chave, numero,
                List.of(item(7L, "P-7", "1102", 1, 10.0))));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(repo.porChave(chave)).isEmpty();
        assertThat(mensagens("inventory-updates-queue")).noneMatch(m -> "entry".equals(m.path("referenciaType").asText())
                && m.path("motivo").asText().contains("NF " + numero + " "));
    }

    @Test
    void previaDoXml() throws Exception {
        var r = enviarXml(fixture());
        assertThat(r.getStatusCode()).as(String.valueOf(r.getBody())).isEqualTo(HttpStatus.OK);
        JsonNode e = r.getBody();
        assertThat(e.fieldNames()).toIterable().containsExactlyInAnyOrder("fornecedor", "numero", "serie",
                "dataEmissao", "dataEntrada", "chaveAcesso", "naturezaOperacao", "itens");
        assertThat(e.get("fornecedor").get("cnpj").asText()).isEqualTo(CNPJ);
        assertThat(e.get("dataEmissao").asText()).isEqualTo("2026-09-20");
        assertThat(e.get("dataEntrada").asText()).isEqualTo(LocalDate.now(ZoneId.of("America/Sao_Paulo")).toString());
        assertThat(e.get("itens").get(0).get("produtoId").asLong()).isEqualTo(PF_001);
        assertThat(e.get("itens").get(0).get("cfop").asText()).isEqualTo("1102");
        assertThat(e.get("itens").get(1).get("produtoId").isNull()).isTrue();
        String chave = e.get("chaveAcesso").asText();
        assertThat(repo.porChave(chave)).isEmpty();
        verify(integracoes, never()).registrarNota(any());

        // conferido e completado na tela, vira entrada; a prévia do mesmo XML passa a responder 409
        Map<String, Object> confirmada = mapper.convertValue(e, Map.class);
        ((List<Map<String, Object>>) confirmada.get("itens")).get(1).put("produtoId", 8);
        assertThat(chamar(HttpMethod.POST, "/api/notas/entrada", confirmada).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(enviarXml(fixture()).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void xmlInvalidoOuGrande() {
        var r = enviarXml("não é xml".getBytes());
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody().get("message").asText()).isEqualTo("XML inválido ou não é NF-e");

        r = enviarXml(("<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]><x>&e;</x>")
                .getBytes());
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(enviarXml(new byte[6 * 1024 * 1024]).getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }
}

package br.com.wcautopecas.fiscal.sefaz;

import br.com.wcautopecas.commons.mensageria.Outbox;
import br.com.wcautopecas.fiscal.FiscalProperties;
import br.com.wcautopecas.fiscal.nota.NotaItem;
import br.com.wcautopecas.fiscal.nota.NotaLinha;
import br.com.wcautopecas.fiscal.nota.NotaRepository;
import br.com.wcautopecas.fiscal.nota.SaldoDevolucao;
import br.com.wcautopecas.fiscal.sefaz.SefazClient.ComunicacaoException;
import br.com.wcautopecas.fiscal.sefaz.SefazClient.Resposta;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Autorização na SEFAZ (EPB-09): envia o XML assinado, trata 100, 103/105 (consulta do recibo),
 * rejeição e erro de comunicação. Autorizada: nfeProc no S3, baixa de estoque (ou entrada da
 * devolução) e aviso ao document-service.
 */
@Service
public class AutorizacaoService {

    private static final Logger log = LoggerFactory.getLogger(AutorizacaoService.class);
    static final Set<String> AUTORIZADA = Set.of("100", "150");
    static final Set<String> EM_PROCESSAMENTO = Set.of("103", "105");
    static final String LOTE_PROCESSADO = "104";

    private final NotaRepository repo;
    private final SefazClient sefaz;
    private final S3Client s3;
    private final SqsClient sqs;
    private final Outbox outbox;
    private final EfeitosFiscais efeitos;
    private final ObjectMapper mapper;
    private final FiscalProperties props;
    private final Clock clock;
    private final SaldoDevolucao saldo;

    public AutorizacaoService(NotaRepository repo, SefazClient sefaz, S3Client s3, SqsClient sqs, Outbox outbox,
            EfeitosFiscais efeitos, ObjectMapper mapper, FiscalProperties props, Clock clock, SaldoDevolucao saldo) {
        this.saldo = saldo;
        this.repo = repo;
        this.sefaz = sefaz;
        this.s3 = s3;
        this.sqs = sqs;
        this.outbox = outbox;
        this.efeitos = efeitos;
        this.mapper = mapper;
        this.props = props;
        this.clock = clock;
    }

    public enum Resultado { AUTORIZADA, REJEITADA, REENFILEIRADA, IGNORADA }

    /**
     * @param tentativa  tentativa lógica (sobe quando o lote fica em processamento)
     * @param recebimento quantas vezes a SQS já entregou esta mensagem (1 na primeira)
     */
    public Resultado processar(long notaId, int tentativa, int recebimento) {
        NotaItem n = repo.porId(notaId).orElse(null);
        if (n == null) {
            throw new IllegalStateException("nota " + notaId + " não existe");
        }
        if (!"processando".equals(n.getStatus()) || n.getCancelamentoId() != null || n.getXmlPath() == null) {
            log.info("nota {} está {}; envio ignorado", notaId, n.getStatus());
            return Resultado.IGNORADA;
        }
        String xml = s3.getObjectAsBytes(r -> r.bucket(bucket(n)).key(chave(n))).asUtf8String();

        Resposta r;
        try {
            r = sefaz.autorizar(xml, n.getId());
            String lote = r.texto("retEnviNFe", "cStat");
            if (EM_PROCESSAMENTO.contains(lote)) {
                r = consultarRecibo(r.texto("nRec"));
                if (r == null) {
                    reenfileirar(n, tentativa + 1);
                    return Resultado.REENFILEIRADA;
                }
            }
        } catch (ComunicacaoException e) {
            registrarFalha(n, recebimento, e);
            throw e;
        }

        String lote = r.texto("cStat");
        if (!LOTE_PROCESSADO.equals(lote) || r.trecho("protNFe") == null) {
            return rejeitar(n, lote, r.texto("xMotivo"));
        }
        String cStat = r.texto("infProt", "cStat");
        if (!AUTORIZADA.contains(cStat)) {
            return rejeitar(n, cStat, r.texto("infProt", "xMotivo"));
        }
        return autorizar(n, xml, r, cStat);
    }

    /** Até 3 consultas (5, 10 e 20 s); {@code null} se o lote continua em processamento. */
    private Resposta consultarRecibo(String nRec) {
        for (Duration espera : props.getSefaz().getEsperasConsulta()) {
            dormir(espera);
            Resposta r = sefaz.consultarRecibo(nRec);
            if (!EM_PROCESSAMENTO.contains(r.texto("cStat"))) {
                return r;
            }
        }
        return null;
    }

    private Resultado autorizar(NotaItem n, String xml, Resposta r, String cStat) {
        String nfeProc = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><nfeProc xmlns=\"" + SefazClient.NFE
                + "\" versao=\"4.00\">" + SefazClient.semDeclaracao(xml) + r.trecho("protNFe") + "</nfeProc>";
        s3.putObject(b -> b.bucket(bucket(n)).key(chave(n)).contentType("application/xml"),
                RequestBody.fromString(nfeProc, StandardCharsets.UTF_8));

        n.setStatus("autorizada");
        n.setProtocolo(r.texto("infProt", "nProt"));
        String dh = r.texto("infProt", "dhRecbto");
        n.setAutorizadaEm(dh == null ? clock.instant().toString() : OffsetDateTime.parse(dh).toInstant().toString());
        n.setCodigoStatus(cStat);
        n.setMotivoRejeicao(null);
        if (!repo.salvarSeStatus(n, "processando")) {
            log.info("nota {} já tinha sido tratada; autorização repetida ignorada", n.getId());
            return Resultado.IGNORADA;
        }
        log.info("nota {} {} autorizada, protocolo {}", n.getId(), n.getNumero(), n.getProtocolo());

        List<NotaLinha> linhas = repo.itens(n.getId());
        Map<Long, Long> somas = new LinkedHashMap<>();
        linhas.forEach(l -> somas.merge(l.getProdutoId(), l.getQuantidade(), Long::sum));
        if ("DEVOLUCAO".equals(n.getTipo())) {
            somas.forEach((produto, q) -> efeitos.estoque("increase", produto, q, n.getId(), "return",
                    "Devolução NF-e " + n.getNumero()));
            saldo.aplicar(n, SaldoDevolucao.Evento.AUTORIZADA);
        } else if (n.getVendaId() != null) {
            somas.forEach((produto, q) -> efeitos.estoque("decrease", produto, q, n.getVendaId(), "sale",
                    "NF-e " + n.getNumero() + " autorizada"));
        }
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("eventType", "nota.autorizada");
        doc.put("notaId", n.getId());
        doc.put("chaveAcesso", n.getChaveAcesso());
        doc.put("tipo", n.getTipo());
        doc.put("timestamp", clock.instant().toString());
        outbox.fila(props.getFilaDocumentos(), "nota.autorizada", doc);
        return Resultado.AUTORIZADA;
    }

    private Resultado rejeitar(NotaItem n, String cStat, String motivo) {
        n.setStatus("rejeitada");
        n.setCodigoStatus(cStat);
        n.setMotivoRejeicao(motivo);
        if (!repo.salvarSeStatus(n, "processando")) {
            return Resultado.IGNORADA;
        }
        log.warn("nota {} {} rejeitada pela SEFAZ: {} {}", n.getId(), n.getNumero(), cStat, motivo);
        saldo.aplicar(n, SaldoDevolucao.Evento.REJEITADA);
        efeitos.avisar("medium", "Nota " + n.getNumero() + " (id " + n.getId() + ") rejeitada: " + cStat + " " + motivo);
        return Resultado.REJEITADA;
    }

    private void registrarFalha(NotaItem n, int recebimento, ComunicacaoException e) {
        n.setTentativasEnvio((n.getTentativasEnvio() == null ? 0 : n.getTentativasEnvio()) + 1);
        repo.salvarSeStatus(n, "processando");
        log.warn("falha de comunicação com a SEFAZ na nota {} (tentativa {}): {}", n.getId(), recebimento,
                e.getMessage());
        if (recebimento >= props.getSefaz().getMaxTentativas()) {
            efeitos.avisar("high", "SEFAZ indisponível: nota " + n.getNumero() + " (id " + n.getId() + ") foi para a DLQ após "
                    + recebimento + " tentativas. " + e.getMessage());
        }
    }

    /** Espera antes da próxima entrega, depois da falha número {@code recebimento}. */
    public Duration esperaDepoisDaFalha(int recebimento) {
        List<Duration> esperas = props.getSefaz().getEsperasRetentativa();
        return recebimento - 1 < esperas.size() ? esperas.get(recebimento - 1) : Duration.ZERO;
    }

    private void reenfileirar(NotaItem n, int tentativa) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("notaId", n.getId());
        msg.put("xmlPath", n.getXmlPath());
        msg.put("ambiente", props.getAmbiente());
        msg.put("tipoNota", n.getTipo());
        msg.put("tentativa", tentativa);
        msg.put("timestamp", clock.instant().toString());
        try {
            String url = sqs.getQueueUrl(r -> r.queueName(props.getFilaSefaz())).queueUrl();
            String corpo = mapper.writeValueAsString(msg);
            int atraso = (int) Math.min(900, props.getSefaz().getAtrasoReenvio().toSeconds());
            sqs.sendMessage(r -> r.queueUrl(url).messageBody(corpo).delaySeconds(atraso));
            log.info("lote da nota {} segue em processamento; nova consulta na tentativa {}", n.getId(), tentativa);
        } catch (Exception e) {
            throw new IllegalStateException("não foi possível devolver a nota " + n.getId() + " para a fila", e);
        }
    }

    private static String bucket(NotaItem n) {
        return n.getXmlPath().substring(5, n.getXmlPath().indexOf('/', 5));
    }

    private static String chave(NotaItem n) {
        return n.getXmlPath().substring(n.getXmlPath().indexOf('/', 5) + 1);
    }

    private static void dormir(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}

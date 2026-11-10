package br.com.wcautopecas.fiscal.xml;

import br.com.wcautopecas.commons.erro.NaoEncontradoException;
import br.com.wcautopecas.commons.mensageria.EventPublisher;
import br.com.wcautopecas.commons.mensageria.IdempotencyGuard;
import br.com.wcautopecas.commons.mensageria.Outbox;
import br.com.wcautopecas.fiscal.FiscalProperties;
import br.com.wcautopecas.fiscal.nota.NotaItem;
import br.com.wcautopecas.fiscal.nota.NotaLinha;
import br.com.wcautopecas.fiscal.nota.NotaRepository;
import br.com.wcautopecas.fiscal.nota.SaldoDevolucao;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Job {@code generate_xml} (EPB-08): chave, XML, assinatura, XSD, S3 e {@code sefaz-queue}.
 * Erro de dados (XSD) rejeita a nota; certificado inválido é definitivo (vai para a DLQ com aviso
 * crítico); o resto é transitório e a SQS reentrega.
 */
@Service
public class ProcessadorXml {

    private static final Logger log = LoggerFactory.getLogger(ProcessadorXml.class);
    static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    static final DateTimeFormatter AAMM = DateTimeFormatter.ofPattern("yyMM");

    private final NotaRepository repo;
    private final NfeXmlBuilder builder;
    private final AssinadorXml assinador;
    private final ValidadorXsd validador;
    private final CertificadoA1 certificado;
    private final S3Client s3;
    private final Outbox outbox;
    private final EventPublisher publisher;
    private final IdempotencyGuard avisos;
    private final FiscalProperties props;
    private final Clock clock;
    private final SaldoDevolucao saldo;

    public ProcessadorXml(NotaRepository repo, NfeXmlBuilder builder, AssinadorXml assinador, ValidadorXsd validador,
            CertificadoA1 certificado, S3Client s3, Outbox outbox, EventPublisher publisher, IdempotencyGuard avisos,
            FiscalProperties props, Clock clock, SaldoDevolucao saldo) {
        this.saldo = saldo;
        this.repo = repo;
        this.builder = builder;
        this.assinador = assinador;
        this.validador = validador;
        this.certificado = certificado;
        this.s3 = s3;
        this.outbox = outbox;
        this.publisher = publisher;
        this.avisos = avisos;
        this.props = props;
        this.clock = clock;
    }

    public enum Resultado { ENVIADA, REJEITADA_XSD, IGNORADA }

    /** Um job por nota de cada vez; job repetido de nota que já saiu para a SEFAZ é ignorado. */
    public Resultado processar(long notaId) {
        String lock = "xml-nota:" + notaId;
        if (!avisos.primeiraVez(lock, Duration.ofMinutes(2))) {
            throw new IllegalStateException("nota " + notaId + " já está sendo processada; a SQS reentrega depois");
        }
        try {
            return processarSemLock(notaId);
        } finally {
            avisos.liberar(lock);
        }
    }

    private Resultado processarSemLock(long notaId) {
        NotaItem n = repo.porId(notaId).orElseThrow(() -> {
            erroDefinitivo(notaId, "nota inexistente", "high");
            return new NaoEncontradoException("Nota " + notaId + " não encontrada");
        });
        if (!"processando".equals(n.getStatus()) || n.getCancelamentoId() != null) {
            log.info("nota {} está {}; job ignorado", notaId, n.getStatus());
            return Resultado.IGNORADA;
        }
        if (n.getXmlPath() != null) {
            log.info("nota {} já foi enviada para a fila da SEFAZ; job repetido ignorado", notaId);
            return Resultado.IGNORADA;
        }
        CertificadoA1.Credencial cred;
        try {
            cred = certificado.get();
            long dias = CertificadoA1.diasRestantes(cred, clock.instant());
            if (!cred.validoAte().isAfter(clock.instant())) {
                throw new CertificadoA1.CertificadoException("certificado vencido em " + cred.validoAte());
            }
            if (dias < 30 && avisos.primeiraVez("aviso:certificado:" + Instant.now().toString().substring(0, 10),
                    Duration.ofDays(1))) {
                avisar("warning", "high", "Certificado A1 vence em " + dias + " dias");
            }
        } catch (CertificadoA1.CertificadoException e) {
            certificado.esquecer();
            erroDefinitivo(notaId, e.getMessage(), "critical");
            throw e;
        }

        List<NotaLinha> linhas = repo.itens(notaId);
        ZonedDateTime emissao = Instant.parse(n.getEmitidaEm()).atZone(FUSO).withNano(0);
        ChaveAcesso chave = n.getChaveAcesso() != null
                ? new ChaveAcesso(n.getChaveAcesso())
                : ChaveAcesso.gerar(Ibge.codigoUf(props.getEmitente().getUf()), AAMM.format(emissao),
                        props.getEmitente().getCnpj(), n.getModelo(), n.getSerie(), Long.parseLong(n.getNumero()), "1");

        Document doc = builder.montar(n, linhas, chave, emissao);
        try {
            assinador.assinar(doc, "infNFe", doc.getDocumentElement(), cred);
        } catch (CertificadoA1.CertificadoException e) {
            erroDefinitivo(notaId, e.getMessage(), "critical");
            throw e;
        }
        String xml = AssinadorXml.texto(doc);

        Optional<String> erro = validador.validar(xml);
        if (erro.isPresent()) {
            log.warn("nota {} rejeitada no XSD: {}", notaId, erro.get());
            n.setChaveAcesso(chave.valor());
            n.setStatus("rejeitada");
            n.setCodigoStatus("XSD");
            n.setMotivoRejeicao(limitar(erro.get()));
            if (repo.salvarSeStatus(n, "processando")) {
                saldo.aplicar(n, SaldoDevolucao.Evento.REJEITADA);
            }
            return Resultado.REJEITADA_XSD;
        }

        String caminho = n.getTipo() + "/" + emissao.getYear() + "/" + String.format("%02d", emissao.getMonthValue())
                + "/" + chave.valor() + ".xml";
        s3.putObject(r -> r.bucket(props.getBucketDocumentos()).key(caminho).contentType("application/xml"),
                RequestBody.fromString(xml, StandardCharsets.UTF_8));
        n.setChaveAcesso(chave.valor());
        n.setXmlPath("s3://" + props.getBucketDocumentos() + "/" + caminho);
        if (!repo.salvarSeStatus(n, "processando")) {
            log.info("nota {} mudou de status durante o processamento; não envia à SEFAZ", notaId);
            return Resultado.IGNORADA;
        }

        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("notaId", n.getId());
        msg.put("xmlPath", n.getXmlPath());
        msg.put("ambiente", props.getAmbiente());
        msg.put("tipoNota", n.getTipo());
        msg.put("tentativa", 1);
        msg.put("timestamp", clock.instant().toString());
        outbox.fila(props.getFilaSefaz(), "send_sefaz", msg);
        log.info("nota {} assinada ({}) e enviada para a fila da SEFAZ", notaId, chave.valor());
        return Resultado.ENVIADA;
    }

    private void erroDefinitivo(long notaId, String motivo, String severidade) {
        log.error("erro definitivo no XML da nota {}: {}", notaId, motivo);
        avisar("fiscal-error", severidade, "Nota " + notaId + ": " + motivo);
    }

    private void avisar(String tipo, String severidade, String mensagem) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("MessageType", tipo);
        msg.put("service", "fiscal-service");
        msg.put("severity", severidade);
        msg.put("message", mensagem);
        msg.put("timestamp", clock.instant().toString());
        try {
            publisher.publicarTopico(props.getTopicoSistema(), tipo, msg, Map.of("severity", severidade));
        } catch (Exception e) {
            log.error("falha ao publicar aviso de sistema", e);
        }
    }

    private static String limitar(String s) {
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}

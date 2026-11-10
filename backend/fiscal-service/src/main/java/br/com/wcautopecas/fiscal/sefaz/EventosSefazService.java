package br.com.wcautopecas.fiscal.sefaz;

import br.com.wcautopecas.fiscal.FiscalProperties;
import br.com.wcautopecas.fiscal.evento.EventoFiscal;
import br.com.wcautopecas.fiscal.evento.EventoRepository;
import br.com.wcautopecas.fiscal.integracao.Integracoes;
import br.com.wcautopecas.fiscal.nota.NotaItem;
import br.com.wcautopecas.fiscal.nota.NotaLinha;
import br.com.wcautopecas.fiscal.nota.NotaRepository;
import br.com.wcautopecas.fiscal.nota.Numerador;
import br.com.wcautopecas.fiscal.nota.SaldoDevolucao;
import br.com.wcautopecas.fiscal.sefaz.SefazClient.ComunicacaoException;
import br.com.wcautopecas.fiscal.sefaz.SefazClient.Resposta;
import br.com.wcautopecas.fiscal.xml.AssinadorXml;
import br.com.wcautopecas.fiscal.xml.CertificadoA1;
import br.com.wcautopecas.fiscal.xml.EventoXmlBuilder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * EPB-10: cancelamento (evento 110111) e inutilização na SEFAZ. Homologado: nota cancelada,
 * estoque de volta e venda cancelada, ou protocolo da inutilização. Rejeitado: evento rejeitado
 * com o motivo e a nota volta para autorizada. Erro de comunicação: relança (retentativa e DLQ).
 */
@Service
public class EventosSefazService {

    private static final Logger log = LoggerFactory.getLogger(EventosSefazService.class);
    static final Set<String> CANCELAMENTO_OK = Set.of("135", "155");
    static final String INUTILIZACAO_OK = "102";
    static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    public enum Resultado { CONCLUIDO, REJEITADO, IGNORADO }

    private final EventoRepository eventos;
    private final NotaRepository notas;
    private final EventoXmlBuilder builder;
    private final AssinadorXml assinador;
    private final CertificadoA1 certificado;
    private final SefazClient sefaz;
    private final S3Client s3;
    private final DynamoDbClient dynamo;
    private final EfeitosFiscais efeitos;
    private final SaldoDevolucao saldo;
    private final Numerador numerador;
    private final Integracoes integracoes;
    private final FiscalProperties props;
    private final Clock clock;

    public EventosSefazService(EventoRepository eventos, NotaRepository notas, EventoXmlBuilder builder,
            AssinadorXml assinador, CertificadoA1 certificado, SefazClient sefaz, S3Client s3, DynamoDbClient dynamo,
            EfeitosFiscais efeitos, SaldoDevolucao saldo, Numerador numerador, Integracoes integracoes,
            FiscalProperties props, Clock clock) {
        this.eventos = eventos;
        this.notas = notas;
        this.builder = builder;
        this.assinador = assinador;
        this.certificado = certificado;
        this.sefaz = sefaz;
        this.s3 = s3;
        this.dynamo = dynamo;
        this.efeitos = efeitos;
        this.saldo = saldo;
        this.numerador = numerador;
        this.integracoes = integracoes;
        this.props = props;
        this.clock = clock;
    }

    /** @param recebimento quantas vezes a SQS já entregou esta mensagem (1 na primeira) */
    public Resultado processar(long eventoId, int recebimento) {
        EventoFiscal e = eventos.porId(eventoId).orElse(null);
        if (e == null || !"processando".equals(e.getStatus())) {
            log.info("evento fiscal {} não está em processamento; mensagem ignorada", eventoId);
            return Resultado.IGNORADO;
        }
        return EventoFiscal.CANCELAMENTO.equals(e.getTipo()) ? cancelar(e, recebimento) : inutilizar(e, recebimento);
    }

    // ---- cancelamento (HU-B10.1)

    private Resultado cancelar(EventoFiscal e, int recebimento) {
        NotaItem n = notas.porId(e.getNotaId()).orElseThrow(
                () -> new IllegalStateException("nota " + e.getNotaId() + " do evento " + e.getId() + " não existe"));
        if (!"processando".equals(n.getStatus()) || !e.getId().equals(n.getCancelamentoId())) {
            log.warn("nota {} está {} e não espera o cancelamento {}", n.getId(), n.getStatus(), e.getId());
            return Resultado.IGNORADO;
        }
        Document doc = builder.cancelamento(n.getChaveAcesso(), n.getProtocolo(), e.getJustificativa(), agora());
        assinador.assinar(doc, "infEvento", doc.getDocumentElement(), credencial(e));
        String xml = AssinadorXml.texto(doc);

        Resposta r = enviar(e, recebimento, () -> sefaz.enviarEvento(xml, e.getId()));
        String cStat = r.texto("retEvento", "cStat");
        String motivo = r.texto("retEvento", "xMotivo");
        if (cStat == null) {
            cStat = r.texto("cStat");
            motivo = r.texto("xMotivo");
        }
        if (!CANCELAMENTO_OK.contains(cStat)) {
            return rejeitarCancelamento(e, n, cStat, motivo);
        }

        String caminho = chaveS3(n).replaceFirst("\\.xml$", "-cancelamento.xml");
        String proc = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><procEventoNFe xmlns=\"" + SefazClient.NFE
                + "\" versao=\"1.00\">" + SefazClient.semDeclaracao(xml) + r.trecho("retEvento") + "</procEventoNFe>";
        salvarS3(caminho, proc);

        String quando = clock.instant().toString();
        e.setStatus("concluido");
        e.setProtocolo(r.texto("retEvento", "nProt"));
        e.setCodigoStatus(cStat);
        e.setMotivoRejeicao(null);
        e.setXmlPath("s3://" + props.getBucketDocumentos() + "/" + caminho);
        n.setStatus("cancelada");
        n.setCanceladaEm(quando);
        n.setCodigoStatus(cStat);
        if (!salvarJuntos(n, e)) {
            log.info("cancelamento {} já tinha sido tratado", e.getId());
            return Resultado.IGNORADO;
        }
        log.info("nota {} {} cancelada, protocolo {}", n.getId(), n.getNumero(), e.getProtocolo());

        Map<Long, Long> somas = new LinkedHashMap<>();
        List<NotaLinha> linhas = notas.itens(n.getId());
        linhas.forEach(l -> somas.merge(l.getProdutoId(), l.getQuantidade(), Long::sum));
        boolean devolucao = "DEVOLUCAO".equals(n.getTipo());
        somas.forEach((produto, q) -> efeitos.estoque(devolucao ? "decrease" : "increase", produto, q, n.getId(),
                "cancellation", "Cancelamento da nota " + n.getNumero()));
        if (devolucao) {
            saldo.aplicar(n, SaldoDevolucao.Evento.CANCELADA);
        } else if (n.getVendaId() != null) {
            try {
                integracoes.cancelarVenda(n.getVendaId(), n.getId());
            } catch (RuntimeException ex) {
                efeitos.avisar("high", "Nota " + n.getNumero() + " (id " + n.getId() + ") cancelada, mas a venda "
                        + n.getVendaId() + " não foi marcada como cancelada: " + ex.getMessage());
            }
        }
        return Resultado.CONCLUIDO;
    }

    private Resultado rejeitarCancelamento(EventoFiscal e, NotaItem n, String cStat, String motivo) {
        e.setStatus("rejeitado");
        e.setCodigoStatus(cStat);
        e.setMotivoRejeicao(motivo);
        n.setStatus("autorizada");
        n.setCancelamentoId(null);
        if (!salvarJuntos(n, e)) {
            return Resultado.IGNORADO;
        }
        log.warn("cancelamento da nota {} rejeitado: {} {}", n.getId(), cStat, motivo);
        efeitos.avisar("medium", "Cancelamento da nota " + n.getNumero() + " (id " + n.getId() + ") rejeitado: "
                + cStat + " " + motivo);
        return Resultado.REJEITADO;
    }

    private boolean salvarJuntos(NotaItem n, EventoFiscal e) {
        try {
            dynamo.transactWriteItems(r -> r.transactItems(notas.seStatusTx(n, "processando"),
                    eventos.seStatusTx(e, "processando")));
            return true;
        } catch (TransactionCanceledException ex) {
            return false;
        }
    }

    // ---- inutilização (HU-B10.2)

    private Resultado inutilizar(EventoFiscal e, int recebimento) {
        ZonedDateTime quando = agora();
        Document doc = builder.inutilizacao(e.getModelo(), e.getSerie(), e.getNumeroInicial(), e.getNumeroFinal(),
                e.getJustificativa(), quando);
        assinador.assinar(doc, "infInut", doc.getDocumentElement(), credencial(e));
        String xml = AssinadorXml.texto(doc);

        Resposta r = enviar(e, recebimento, () -> sefaz.inutilizar(xml));
        String cStat = r.texto("infInut", "cStat");
        String motivo = r.texto("infInut", "xMotivo");
        e.setCodigoStatus(cStat);
        if (!INUTILIZACAO_OK.equals(cStat)) {
            e.setStatus("rejeitado");
            e.setMotivoRejeicao(motivo);
            if (salvar(e)) {
                log.warn("inutilização {} rejeitada: {} {}", e.getId(), cStat, motivo);
                efeitos.avisar("medium", "Inutilização " + e.getNumeroInicial() + " a " + e.getNumeroFinal()
                        + " da série " + e.getSerie() + " rejeitada: " + cStat + " " + motivo);
                return Resultado.REJEITADO;
            }
            return Resultado.IGNORADO;
        }

        String caminho = String.format("INUTILIZACAO/%04d/%02d/%d.xml", quando.getYear(), quando.getMonthValue(),
                e.getId());
        salvarS3(caminho, "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ProcInutNFe xmlns=\"" + SefazClient.NFE
                + "\" versao=\"4.00\">" + SefazClient.semDeclaracao(xml) + r.trecho("retInutNFe") + "</ProcInutNFe>");
        e.setStatus("concluido");
        e.setProtocolo(r.texto("infInut", "nProt"));
        e.setMotivoRejeicao(null);
        e.setXmlPath("s3://" + props.getBucketDocumentos() + "/" + caminho);
        if (!salvar(e)) {
            return Resultado.IGNORADO;
        }
        numerador.pularAte(e.getModelo(), e.getSerie(), e.getNumeroFinal());
        log.info("inutilização {} ({} a {}) homologada, protocolo {}", e.getId(), e.getNumeroInicial(),
                e.getNumeroFinal(), e.getProtocolo());
        return Resultado.CONCLUIDO;
    }

    private boolean salvar(EventoFiscal e) {
        try {
            dynamo.transactWriteItems(r -> r.transactItems(eventos.seStatusTx(e, "processando")));
            return true;
        } catch (TransactionCanceledException ex) {
            return false;
        }
    }

    // ---- comum

    private CertificadoA1.Credencial credencial(EventoFiscal e) {
        try {
            CertificadoA1.Credencial c = certificado.get();
            if (!c.validoAte().isAfter(clock.instant())) {
                throw new CertificadoA1.CertificadoException("certificado vencido em " + c.validoAte());
            }
            return c;
        } catch (CertificadoA1.CertificadoException ex) {
            certificado.esquecer();
            efeitos.avisar("critical", "Evento fiscal " + e.getId() + " não pôde ser assinado: " + ex.getMessage());
            throw ex;
        }
    }

    private Resposta enviar(EventoFiscal e, int recebimento, java.util.function.Supplier<Resposta> chamada) {
        try {
            return chamada.get();
        } catch (ComunicacaoException ex) {
            e.setTentativasEnvio((e.getTentativasEnvio() == null ? 0 : e.getTentativasEnvio()) + 1);
            salvar(e);
            log.warn("falha de comunicação com a SEFAZ no evento {} (tentativa {}): {}", e.getId(), recebimento,
                    ex.getMessage());
            if (recebimento >= props.getSefaz().getMaxTentativas()) {
                efeitos.avisar("high", "SEFAZ indisponível: evento fiscal " + e.getId() + " (" + e.getTipo()
                        + ") foi para a DLQ após " + recebimento + " tentativas. " + ex.getMessage());
            }
            throw ex;
        }
    }

    private void salvarS3(String caminho, String xml) {
        s3.putObject(b -> b.bucket(props.getBucketDocumentos()).key(caminho).contentType("application/xml"),
                RequestBody.fromString(xml, StandardCharsets.UTF_8));
    }

    private ZonedDateTime agora() {
        return clock.instant().atZone(FUSO).withNano(0);
    }

    private static String chaveS3(NotaItem n) {
        return n.getXmlPath().substring(n.getXmlPath().indexOf('/', 5) + 1);
    }
}

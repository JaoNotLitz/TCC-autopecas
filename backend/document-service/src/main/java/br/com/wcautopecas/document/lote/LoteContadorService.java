package br.com.wcautopecas.document.lote;

import br.com.wcautopecas.commons.dynamo.IdGenerator;
import br.com.wcautopecas.commons.erro.ApiException;
import br.com.wcautopecas.commons.erro.ConflitoException;
import br.com.wcautopecas.commons.mensageria.IdempotencyGuard;
import br.com.wcautopecas.commons.mensageria.Outbox;
import br.com.wcautopecas.document.DocumentosProperties;
import br.com.wcautopecas.document.nota.Nota;
import br.com.wcautopecas.document.nota.NotaRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * HU-B13.4: ZIP com os XMLs do mês (autorizadas e canceladas ainda não enviadas) e um
 * {@code resumo.csv}, no {@code wc-reports-bucket}, com link de 7 dias por e-mail ao contador.
 */
@Service
public class LoteContadorService {

    private static final Logger log = LoggerFactory.getLogger(LoteContadorService.class);
    static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    static final Set<String> ENVIAVEIS = Set.of("autorizada", "cancelada");

    private final NotaRepository notas;
    private final S3Client s3;
    private final S3Presigner presigner;
    private final Outbox outbox;
    private final IdGenerator ids;
    private final IdempotencyGuard locks;
    private final DocumentosProperties props;
    private final Clock clock;

    public LoteContadorService(NotaRepository notas, S3Client s3, S3Presigner presigner, Outbox outbox,
            IdGenerator ids, IdempotencyGuard locks, DocumentosProperties props, Clock clock) {
        this.notas = notas;
        this.s3 = s3;
        this.presigner = presigner;
        this.outbox = outbox;
        this.ids = ids;
        this.locks = locks;
        this.props = props;
        this.clock = clock;
    }

    public record Lote(long id, String caminho, int quantidade, String link) {
    }

    public List<Lote> enviar() {
        String email = props.getContadorEmail();
        if (email == null || email.isBlank()) {
            log.error("CONTADOR_EMAIL não configurado; lote do contador não enviado");
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "E-mail do contador não configurado");
        }
        if (!locks.primeiraVez("lote-contador", Duration.ofMinutes(5))) {
            throw new ConflitoException("Já existe um lote do contador sendo montado");
        }
        try {
            YearMonth mes = YearMonth.now(clock.withZone(FUSO));
            Instant inicio = mes.atDay(1).atStartOfDay(FUSO).toInstant();
            Instant fim = mes.plusMonths(1).atDay(1).atStartOfDay(FUSO).toInstant();
            List<Nota> pendentes = notas.todas().stream()
                    .filter(n -> ENVIAVEIS.contains(n.getStatus()) && n.getXmlPath() != null)
                    .filter(n -> n.getEnviadaContadorEm() == null)
                    .filter(n -> n.getEmitidaEm() != null && dentro(Instant.parse(n.getEmitidaEm()), inicio, fim))
                    .sorted(Comparator.comparing(Nota::getId)).toList();
            if (pendentes.isEmpty()) {
                throw new ConflitoException("Nenhuma nota pendente de envio neste mês");
            }
            List<Lote> lotes = new ArrayList<>();
            int partes = (pendentes.size() + props.getMaximoPorLote() - 1) / props.getMaximoPorLote();
            for (int p = 0; p < partes; p++) {
                List<Nota> parte = pendentes.subList(p * props.getMaximoPorLote(),
                        Math.min(pendentes.size(), (p + 1) * props.getMaximoPorLote()));
                lotes.add(montar(mes, parte, p + 1, partes, email));
            }
            return lotes;
        } finally {
            locks.liberar("lote-contador");
        }
    }

    private Lote montar(YearMonth mes, List<Nota> parte, int numeroParte, int partes, String email) {
        long id = ids.proximo("lotes_contador");
        String caminho = String.format("lotes/%04d/%02d/%d.zip", mes.getYear(), mes.getMonthValue(), id);
        byte[] zip = zip(parte);
        s3.putObject(b -> b.bucket(props.getBucketRelatorios()).key(caminho).contentType("application/zip"),
                RequestBody.fromBytes(zip));
        String link = presigner.presignGetObject(r -> r.signatureDuration(props.getValidadeLink())
                .getObjectRequest(g -> g.bucket(props.getBucketRelatorios()).key(caminho))).url().toString();

        String agora = clock.instant().toString();
        parte.forEach(n -> notas.marcarEnviada(n.getId(), agora));

        String referencia = String.format("%02d/%04d", mes.getMonthValue(), mes.getYear());
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("mes", referencia);
        dados.put("quantidade", parte.size());
        dados.put("link", link);
        dados.put("validadeDias", props.getValidadeLink().toDays());
        dados.put("parte", numeroParte);
        dados.put("partes", partes);
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("to", email);
        msg.put("cc", List.of());
        msg.put("subject", "WC Autopeças - XMLs de " + referencia
                + (partes > 1 ? " (parte " + numeroParte + " de " + partes + ")" : ""));
        msg.put("template", "contador-lote");
        msg.put("data", dados);
        outbox.fila(props.getFilaEmail(), "contador-lote", msg);
        log.info("lote do contador {} com {} notas em {}", id, parte.size(), caminho);
        return new Lote(id, caminho, parte.size(), link);
    }

    byte[] zip(List<Nota> parte) {
        var out = new ByteArrayOutputStream();
        try (var z = new ZipOutputStream(out)) {
            StringBuilder csv = new StringBuilder("numero;serie;chave;tipo;status;valor\n");
            for (Nota n : parte) {
                adicionar(z, n.getChaveAcesso() + ".xml", n.getXmlPath());
                if ("cancelada".equals(n.getStatus())) {
                    adicionar(z, n.getChaveAcesso() + "-cancelamento.xml",
                            n.getXmlPath().replaceFirst("\\.xml$", "-cancelamento.xml"));
                }
                csv.append(n.getNumero()).append(';').append(n.getSerie()).append(';').append(n.getChaveAcesso())
                        .append(';').append(n.getTipo()).append(';').append(n.getStatus()).append(';')
                        .append(n.getValorTotal() == null ? "" : n.getValorTotal().setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ','))
                        .append('\n');
            }
            z.putNextEntry(new ZipEntry("resumo.csv"));
            z.write(csv.toString().getBytes(StandardCharsets.UTF_8));
            z.closeEntry();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private void adicionar(ZipOutputStream z, String nome, String s3Path) throws IOException {
        String bucket = s3Path.substring(5, s3Path.indexOf('/', 5));
        String chave = s3Path.substring(s3Path.indexOf('/', 5) + 1);
        byte[] conteudo;
        try {
            conteudo = s3.getObjectAsBytes(r -> r.bucket(bucket).key(chave)).asByteArray();
        } catch (NoSuchKeyException e) {
            log.warn("{} não está no S3; fica fora do lote", s3Path);
            return;
        }
        z.putNextEntry(new ZipEntry(nome));
        z.write(conteudo);
        z.closeEntry();
    }

    private static boolean dentro(Instant t, Instant inicio, Instant fim) {
        return !t.isBefore(inicio) && t.isBefore(fim);
    }
}

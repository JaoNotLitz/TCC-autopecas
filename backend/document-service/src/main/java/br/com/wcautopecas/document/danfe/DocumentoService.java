package br.com.wcautopecas.document.danfe;

import br.com.wcautopecas.commons.erro.NaoEncontradoException;
import br.com.wcautopecas.commons.erro.RegraNegocioException;
import br.com.wcautopecas.document.nota.Nota;
import br.com.wcautopecas.document.nota.NotaRepository;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/** HU-B13.1 a B13.3: gera o DANFE, guarda no S3 ao lado do XML e entrega os arquivos. */
@Service
public class DocumentoService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoService.class);
    static final Set<String> COM_DANFE = Set.of("autorizada", "cancelada");

    private final NotaRepository notas;
    private final S3Client s3;
    private final DanfeGerador gerador;

    public DocumentoService(NotaRepository notas, S3Client s3, DanfeGerador gerador) {
        this.notas = notas;
        this.s3 = s3;
        this.gerador = gerador;
    }

    public record Arquivo(byte[] bytes, String nome) {
    }

    /** HU-B13.2: o {@code nfeProc} como foi gravado. */
    public Arquivo xml(long id) {
        Nota n = existente(id);
        if (n.getXmlPath() == null) {
            throw new NaoEncontradoException("XML ainda não disponível");
        }
        byte[] xml = ler(n.getXmlPath()).orElseThrow(() -> new NaoEncontradoException("XML ainda não disponível"));
        return new Arquivo(xml, "NFe" + n.getChaveAcesso() + ".xml");
    }

    /** HU-B13.3: PDF do S3, ou gerado agora; cancelada sai com carimbo (sem gravar). */
    public Arquivo pdf(long id) {
        Nota n = existente(id);
        if (!COM_DANFE.contains(n.getStatus())) {
            throw new RegraNegocioException("A nota ainda não foi autorizada");
        }
        if (n.getXmlPath() == null) {
            throw new RegraNegocioException("Esta nota não tem XML emitido pelo sistema para gerar o DANFE");
        }
        byte[] pdf = (n.getPdfPath() == null ? Optional.<byte[]>empty() : ler(n.getPdfPath()))
                .orElseGet(() -> gerarESalvar(n));
        if ("cancelada".equals(n.getStatus())) {
            pdf = gerador.carimbarCancelada(pdf);
        }
        return new Arquivo(pdf, "DANFE_" + n.getNumero() + ".pdf");
    }

    /** HU-B13.1: chamado pelo listener quando a nota é autorizada; idempotente. */
    public boolean gerarAoAutorizar(long id) {
        Nota n = notas.porId(id).orElse(null);
        if (n == null || n.getXmlPath() == null) {
            log.warn("nota {} sem XML; DANFE não gerado", id);
            return false;
        }
        if (n.getPdfPath() != null) {
            log.info("nota {} já tem DANFE; evento repetido ignorado", id);
            return false;
        }
        gerarESalvar(n);
        return true;
    }

    private byte[] gerarESalvar(Nota n) {
        long inicio = System.nanoTime();
        byte[] xml = ler(n.getXmlPath()).orElseThrow(() -> new NaoEncontradoException("XML ainda não disponível"));
        byte[] pdf = gerador.gerar(DanfeDados.ler(xml));
        String caminho = n.getXmlPath().replaceFirst("\\.xml$", ".pdf");
        s3.putObject(b -> b.bucket(bucket(caminho)).key(chave(caminho)).contentType("application/pdf"),
                RequestBody.fromBytes(pdf));
        notas.marcarPdf(n.getId(), caminho);
        n.setPdfPath(caminho);
        log.info("DANFE da nota {} {} gerado em {} ms ({} bytes)", n.getId(), n.getNumero(),
                (System.nanoTime() - inicio) / 1_000_000, pdf.length);
        return pdf;
    }

    private Optional<byte[]> ler(String s3Path) {
        try {
            return Optional.of(s3.getObjectAsBytes(r -> r.bucket(bucket(s3Path)).key(chave(s3Path))).asByteArray());
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        }
    }

    private Nota existente(long id) {
        return notas.porId(id).orElseThrow(() -> new NaoEncontradoException("Nota não encontrada"));
    }

    static String bucket(String s3Path) {
        return s3Path.substring(5, s3Path.indexOf('/', 5));
    }

    static String chave(String s3Path) {
        return s3Path.substring(s3Path.indexOf('/', 5) + 1);
    }

    static String texto(byte[] b) {
        return new String(b, StandardCharsets.UTF_8);
    }
}

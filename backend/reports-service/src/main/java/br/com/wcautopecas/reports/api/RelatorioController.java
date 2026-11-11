package br.com.wcautopecas.reports.api;

import br.com.wcautopecas.commons.erro.SemPermissaoException;
import br.com.wcautopecas.commons.seguranca.UsuarioAutenticado;
import br.com.wcautopecas.reports.painel.Dashboard;
import br.com.wcautopecas.reports.painel.DashboardService;
import br.com.wcautopecas.reports.planilha.Planilha;
import br.com.wcautopecas.reports.planilha.RelatoriosService;

import br.com.wcautopecas.reports.fiscal.RelatorioNotasService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Seção 5.9: {@code /api/notas/relatorio}, {@code /api/dashboard} e {@code /api/relatorios/**}, que o gateway manda para este serviço. */
@RestController
public class RelatorioController {

    static final MediaType CSV = new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8);

    private final RelatorioNotasService relatorio;
    private final RelatoriosService relatorios;
    private final DashboardService dashboard;

    public RelatorioController(RelatorioNotasService relatorio, RelatoriosService relatorios,
            DashboardService dashboard) {
        this.relatorio = relatorio;
        this.relatorios = relatorios;
        this.dashboard = dashboard;
    }

    /** Extra (HU-B14.1). {@code atualizar=true} recalcula na hora e é só do ADMIN. */
    @GetMapping("/api/dashboard")
    public Dashboard dashboard(@RequestParam(defaultValue = "false") boolean atualizar, UsuarioAutenticado usuario) {
        if (atualizar && !usuario.admin()) {
            throw new SemPermissaoException("Só o administrador pode forçar o recálculo do dashboard");
        }
        return dashboard.obter(atualizar);
    }

    /** Extra (HU-B14.2): {@code formato=csv|xlsx}. */
    @GetMapping("/api/relatorios/vendas")
    public ResponseEntity<byte[]> vendas(@RequestParam(required = false) String de,
            @RequestParam(required = false) String ate, @RequestParam(required = false) String formato,
            @RequestParam(required = false) Long clienteId, @RequestParam(required = false) Long vendedorId) {
        return download(relatorios.vendas(de, ate, formato, clienteId, vendedorId));
    }

    /** Extra (HU-B14.3): {@code formato=csv|xlsx}, {@code status=ok|atencao|baixo|critico}. */
    @GetMapping("/api/relatorios/estoque")
    public ResponseEntity<byte[]> estoque(@RequestParam(required = false) String formato,
            @RequestParam(required = false) String status) {
        return download(relatorios.estoque(formato, status));
    }

    private static ResponseEntity<byte[]> download(Planilha.Arquivo a) {
        return ResponseEntity.ok().contentType(a.tipo())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(a.nome()).build().toString())
                .contentLength(a.conteudo().length).body(a.conteudo());
    }

    @GetMapping("/api/notas/relatorio")
    public ResponseEntity<byte[]> notas(@RequestParam(required = false) String status,
            @RequestParam(required = false) String de, @RequestParam(required = false) String ate) {
        var r = relatorio.gerar(status, de, ate);
        return ResponseEntity.ok().contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(r.nomeArquivo()).build().toString())
                .contentLength(r.csv().length).body(r.csv());
    }
}

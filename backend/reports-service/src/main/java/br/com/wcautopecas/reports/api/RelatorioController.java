package br.com.wcautopecas.reports.api;

import br.com.wcautopecas.reports.fiscal.RelatorioNotasService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Seção 5.9: {@code /api/notas/relatorio}, que o gateway manda para este serviço. */
@RestController
public class RelatorioController {

    static final MediaType CSV = new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8);

    private final RelatorioNotasService relatorio;

    public RelatorioController(RelatorioNotasService relatorio) {
        this.relatorio = relatorio;
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

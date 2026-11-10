package br.com.wcautopecas.document.api;

import br.com.wcautopecas.document.danfe.DocumentoService;
import br.com.wcautopecas.document.lote.LoteContadorService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Seção 5.8: caminhos sob {@code /api/notas} porque o front chama assim. Devolve os bytes (o
 * front baixa com fetch + Authorization, então não há redirect para o S3).
 */
@RestController
@RequestMapping("/api/notas")
public class DocumentoController {

    private final DocumentoService documentos;
    private final LoteContadorService lotes;

    public DocumentoController(DocumentoService documentos, LoteContadorService lotes) {
        this.documentos = documentos;
        this.lotes = lotes;
    }

    @GetMapping("/{id}/xml")
    public ResponseEntity<byte[]> xml(@PathVariable long id) {
        var a = documentos.xml(id);
        return arquivo(a.bytes(), a.nome(), MediaType.APPLICATION_XML, false);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable long id, @RequestParam(defaultValue = "false") boolean inline) {
        var a = documentos.pdf(id);
        return arquivo(a.bytes(), a.nome(), MediaType.APPLICATION_PDF, inline);
    }

    @PostMapping("/lote-contador")
    public ResponseEntity<Void> loteContador() {
        lotes.enviar();
        return ResponseEntity.noContent().build();
    }

    private static ResponseEntity<byte[]> arquivo(byte[] bytes, String nome, MediaType tipo, boolean inline) {
        ContentDisposition cd = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(nome).build();
        return ResponseEntity.ok().contentType(tipo).header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .contentLength(bytes.length).body(bytes);
    }
}

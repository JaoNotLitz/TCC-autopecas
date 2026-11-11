package br.com.wcautopecas.sales.api;

import br.com.wcautopecas.sales.venda.VendaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rotas internas (decisão A4): o gateway não roteia {@code /internal}. */
@RestController
@RequestMapping("/internal/vendas")
public class InternoController {

    private final VendaService service;

    public InternoController(VendaService service) {
        this.service = service;
    }

    /**
     * EPB-14: vendas do período (dias no fuso da loja), com data e vendedor, para o relatório e o
     * dashboard do reports-service.
     */
    @GetMapping
    public java.util.List<VendaRelatorio> relatorio(@RequestParam String de, @RequestParam String ate) {
        return service.relatorio(de, ate);
    }

    /** HU-B06.7: o fiscal-service avisa qual nota foi criada para a venda. */
    @PutMapping("/{id}/nota-fiscal")
    public ResponseEntity<Void> vincularNota(@PathVariable long id, @Valid @RequestBody NotaFiscalInput in) {
        service.vincularNota(id, in.notaFiscalId());
        return ResponseEntity.noContent().build();
    }

    /** EPB-10: o fiscal-service avisa que a nota da venda foi cancelada na SEFAZ. */
    @PutMapping("/{id}/cancelamento")
    public ResponseEntity<Void> canceladaPelaNota(@PathVariable long id, @Valid @RequestBody NotaFiscalInput in) {
        service.canceladaPelaNota(id, in.notaFiscalId());
        return ResponseEntity.noContent().build();
    }

    public record NotaFiscalInput(@NotNull(message = "Informe a nota fiscal") Long notaFiscalId) {
    }
}

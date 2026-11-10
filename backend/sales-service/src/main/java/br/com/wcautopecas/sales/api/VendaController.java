package br.com.wcautopecas.sales.api;

import br.com.wcautopecas.commons.seguranca.UsuarioAutenticado;
import br.com.wcautopecas.sales.venda.VendaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Seção 5.4 da arquitetura. */
@RestController
@RequestMapping("/api/vendas")
public class VendaController {

    private final VendaService service;

    public VendaController(VendaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VendaDto criar(@Valid @RequestBody VendaInput in, UsuarioAutenticado usuario) {
        return service.criar(in, usuario);
    }

    /** Extra (HU-B06.5). */
    @GetMapping
    public List<VendaDto.Detalhe> listar(@RequestParam(required = false) String status,
            @RequestParam(required = false) Long clienteId, @RequestParam(required = false) String de,
            @RequestParam(required = false) String ate) {
        return service.listar(status, clienteId, de, ate);
    }

    /** Extra (HU-B06.5). */
    @GetMapping("/{id}")
    public VendaDto.Detalhe buscar(@PathVariable long id) {
        return service.buscar(id);
    }

    /** Extra (HU-B06.6): finaliza ou cancela um orçamento. */
    @PutMapping("/{id}/status")
    public VendaDto.Detalhe alterarStatus(@PathVariable long id, @Valid @RequestBody StatusInput in,
            UsuarioAutenticado usuario) {
        return service.alterarStatus(id, in.status(), usuario);
    }

    /** Extra (HU-B06.6): equivale a {@code status = cancelada}. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable long id, UsuarioAutenticado usuario) {
        service.alterarStatus(id, "cancelada", usuario);
        return ResponseEntity.noContent().build();
    }

    public record StatusInput(@NotBlank(message = "Informe o status") String status) {
    }
}

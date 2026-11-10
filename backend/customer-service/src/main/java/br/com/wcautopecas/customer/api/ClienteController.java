package br.com.wcautopecas.customer.api;

import br.com.wcautopecas.commons.seguranca.Papel;
import br.com.wcautopecas.commons.seguranca.RequerPapel;
import br.com.wcautopecas.customer.cliente.ClienteService;
import jakarta.validation.Valid;
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

/** Seção 5.2 da arquitetura. */
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @GetMapping
    public List<ClienteDto> listar(@RequestParam(required = false) String tipo,
            @RequestParam(required = false) String busca, @RequestParam(required = false) String documento) {
        return service.listar(tipo, busca, documento);
    }

    /** Extra: usado pelas chamadas internas (sales). */
    @GetMapping("/{id}")
    public ClienteDto buscar(@PathVariable long id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteDto criar(@Valid @RequestBody ClienteInput in) {
        return service.criar(in);
    }

    @PutMapping("/{id}")
    public ClienteDto atualizar(@PathVariable long id, @Valid @RequestBody ClienteInput in) {
        return service.atualizar(id, in);
    }

    @DeleteMapping("/{id}")
    @RequerPapel(Papel.ADMIN)
    public ResponseEntity<Void> desativar(@PathVariable long id) {
        service.desativar(id);
        return ResponseEntity.noContent().build();
    }
}

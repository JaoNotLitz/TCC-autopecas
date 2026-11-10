package br.com.wcautopecas.fiscal.api;

import br.com.wcautopecas.fiscal.nota.NotaItem;
import br.com.wcautopecas.fiscal.nota.NotaLinha;
import br.com.wcautopecas.fiscal.nota.NotaRepository;
import br.com.wcautopecas.fiscal.nota.NotaService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rotas internas (decisão A4): nota completa para o document-service e os testes ponta a ponta. */
@RestController
@RequestMapping("/internal/notas")
public class InternoController {

    private final NotaService notas;
    private final NotaRepository repo;

    public InternoController(NotaService notas, NotaRepository repo) {
        this.notas = notas;
        this.repo = repo;
    }

    public record NotaCompleta(NotaItem nota, List<NotaLinha> itens) {
    }

    @GetMapping("/{id}")
    public NotaCompleta buscar(@PathVariable long id) {
        return new NotaCompleta(notas.existente(id), repo.itens(id));
    }
}

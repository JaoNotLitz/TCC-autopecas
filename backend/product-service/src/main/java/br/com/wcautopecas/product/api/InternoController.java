package br.com.wcautopecas.product.api;

import br.com.wcautopecas.product.api.Internos.Disponibilidade;
import br.com.wcautopecas.product.api.Internos.ItemDisponibilidade;
import br.com.wcautopecas.product.api.Internos.ProdutoFiscal;
import br.com.wcautopecas.product.produto.ProdutoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rotas internas (decisão A4): chamadas por outros serviços na rede do Compose, repassando o
 * token do usuário. O gateway não roteia {@code /internal}.
 */
@RestController
@RequestMapping("/internal/produtos")
public class InternoController {

    private final ProdutoService service;

    public InternoController(ProdutoService service) {
        this.service = service;
    }

    /** EPB-14: posição do estoque para o relatório e o dashboard. */
    @GetMapping("/estoque")
    public List<Internos.PosicaoEstoque> estoque() {
        return service.posicaoEstoque();
    }

    @GetMapping("/{id}")
    public ProdutoFiscal fiscal(@PathVariable long id) {
        return service.fiscal(id);
    }

    @PostMapping("/por-codigo")
    public List<Internos.CodigoProduto> porCodigo(@RequestBody List<String> codigos) {
        return service.porCodigos(codigos);
    }

    @PostMapping("/disponibilidade")
    public List<Disponibilidade> disponibilidade(@Valid @RequestBody List<ItemDisponibilidade> itens) {
        return service.disponibilidade(itens);
    }
}

package br.com.wcautopecas.product.api;

import br.com.wcautopecas.commons.seguranca.Papel;
import br.com.wcautopecas.commons.seguranca.RequerPapel;
import br.com.wcautopecas.commons.seguranca.UsuarioAutenticado;
import br.com.wcautopecas.product.api.Internos.AjusteAceito;
import br.com.wcautopecas.product.api.Internos.AjusteInput;
import br.com.wcautopecas.product.api.Internos.Movimento;
import br.com.wcautopecas.product.produto.ProdutoService;
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

/** Seção 5.3 da arquitetura. */
@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    private final ProdutoService service;

    public ProdutoController(ProdutoService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProdutoDto> listar(@RequestParam(required = false) String busca,
            @RequestParam(required = false) String status) {
        return service.listar(busca, status);
    }

    @GetMapping("/resumo")
    public ResumoEstoque resumo() {
        return service.resumo();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProdutoDto criar(@Valid @RequestBody ProdutoInput in, UsuarioAutenticado usuario) {
        return service.criar(in, usuario);
    }

    /** VENDEDOR pode editar o cadastro; mudar {@code estoqueAtual} exige ADMIN (checado no serviço). */
    @PutMapping("/{id}")
    public ProdutoDto atualizar(@PathVariable long id, @Valid @RequestBody ProdutoInput in,
            UsuarioAutenticado usuario) {
        return service.atualizar(id, in, usuario);
    }

    @DeleteMapping("/{id}")
    @RequerPapel(Papel.ADMIN)
    public ResponseEntity<Void> desativar(@PathVariable long id) {
        service.desativar(id);
        return ResponseEntity.noContent().build();
    }

    /** Extra. */
    @GetMapping("/{id}")
    public ProdutoDto buscar(@PathVariable long id) {
        return service.buscar(id);
    }

    /** Extra. */
    @GetMapping("/{id}/movimentos")
    public List<Movimento> movimentos(@PathVariable long id, @RequestParam(required = false) String de,
            @RequestParam(required = false) String ate, @RequestParam(required = false) String tipo) {
        return service.movimentos(id, de, ate, tipo);
    }

    /** Extra: ajuste assíncrono pela fila. */
    @PostMapping("/{id}/ajuste")
    @RequerPapel(Papel.ADMIN)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AjusteAceito ajuste(@PathVariable long id, @Valid @RequestBody AjusteInput in, UsuarioAutenticado usuario) {
        return service.ajusteAssincrono(id, in, usuario);
    }
}

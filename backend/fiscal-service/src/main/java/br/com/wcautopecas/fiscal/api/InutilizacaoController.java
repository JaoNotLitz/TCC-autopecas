package br.com.wcautopecas.fiscal.api;

import br.com.wcautopecas.commons.seguranca.Papel;
import br.com.wcautopecas.commons.seguranca.RequerPapel;
import br.com.wcautopecas.commons.seguranca.UsuarioAutenticado;
import br.com.wcautopecas.fiscal.evento.InutilizacaoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Seção 5.6 da arquitetura. */
@RestController
@RequestMapping("/api/inutilizacoes")
public class InutilizacaoController {

    private final InutilizacaoService service;

    public InutilizacaoController(InutilizacaoService service) {
        this.service = service;
    }

    @GetMapping
    public List<InutilizacaoDto> listar() {
        return service.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequerPapel(Papel.ADMIN)
    public InutilizacaoDto registrar(@Valid @RequestBody InutilizacaoInput in, UsuarioAutenticado usuario) {
        return service.registrar(in, usuario);
    }
}

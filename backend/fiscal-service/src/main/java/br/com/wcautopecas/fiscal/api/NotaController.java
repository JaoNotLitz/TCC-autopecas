package br.com.wcautopecas.fiscal.api;

import br.com.wcautopecas.commons.seguranca.Papel;
import br.com.wcautopecas.commons.seguranca.RequerPapel;
import br.com.wcautopecas.commons.seguranca.UsuarioAutenticado;
import br.com.wcautopecas.fiscal.nota.DevolucaoService;
import br.com.wcautopecas.fiscal.nota.NotaService;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Seção 5.5 da arquitetura. */
@RestController
@RequestMapping("/api/notas")
public class NotaController {

    private final NotaService notas;
    private final DevolucaoService devolucoes;

    public NotaController(NotaService notas, DevolucaoService devolucoes) {
        this.notas = notas;
        this.devolucoes = devolucoes;
    }

    @GetMapping
    public List<NotaDto> listar(@RequestParam(required = false) String status,
            @RequestParam(required = false) String de, @RequestParam(required = false) String ate,
            @RequestParam(required = false) String tipo, @RequestParam(required = false) Long vendaId) {
        return notas.listar(status, de, ate, tipo, vendaId);
    }

    /** Extra (HU-B07.3). */
    @GetMapping("/{id}")
    public NotaDto buscar(@PathVariable long id) {
        return NotaDto.de(notas.existente(id));
    }

    @GetMapping("/{id}/itens")
    public List<NotaDto.Item> itens(@PathVariable long id) {
        return notas.itens(id);
    }

    @PostMapping("/{id}/cancelamento")
    @RequerPapel(Papel.ADMIN)
    public NotaDto cancelar(@PathVariable long id, @RequestBody CancelamentoInput in, UsuarioAutenticado usuario) {
        return notas.cancelar(id, in.justificativa(), usuario);
    }

    @PostMapping("/devolucao")
    @ResponseStatus(HttpStatus.CREATED)
    public NotaDto devolver(@Valid @RequestBody DevolucaoInput in) {
        return devolucoes.devolver(in);
    }

    /** Extra (HU-B07.4). */
    @PostMapping("/{id}/reenviar")
    public NotaDto reenviar(@PathVariable long id) {
        return notas.reenviar(id);
    }

    public record CancelamentoInput(String justificativa) {
    }
}

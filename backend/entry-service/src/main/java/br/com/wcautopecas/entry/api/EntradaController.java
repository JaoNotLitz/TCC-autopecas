package br.com.wcautopecas.entry.api;

import br.com.wcautopecas.commons.erro.ValidacaoException;
import br.com.wcautopecas.commons.seguranca.UsuarioAutenticado;
import br.com.wcautopecas.entry.entrada.EntradaService;
import br.com.wcautopecas.entry.entrada.NotaEntrada;
import br.com.wcautopecas.entry.xml.NfeEntradaParser;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Seção 5.7 da arquitetura: caminhos sob {@code /api/notas/entrada}, como o front chama. */
@RestController
@RequestMapping("/api/notas/entrada")
public class EntradaController {

    private final EntradaService service;

    public EntradaController(EntradaService service) {
        this.service = service;
    }

    /** HU-B11.1: registra a nota do fornecedor e devolve a {@code NotaFiscal} ENTRADA. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotaFiscalDto registrar(@Valid @RequestBody EntradaInput in, UsuarioAutenticado usuario) {
        return service.registrar(in, usuario);
    }

    /** HU-B11.2: lê o XML (campo {@code arquivo}) e devolve o formulário preenchido, sem gravar. */
    @PostMapping(path = "/importar-xml", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public EntradaInput importarXml(@RequestPart("arquivo") MultipartFile arquivo) throws IOException {
        if (arquivo.isEmpty()) {
            throw new ValidacaoException("Envie o arquivo XML da nota");
        }
        try {
            return service.previa(arquivo.getBytes());
        } catch (NfeEntradaParser.XmlInvalidoException e) {
            throw new ValidacaoException(e.getMessage());
        }
    }

    /** Extra (HU-B11.4). */
    @GetMapping
    public List<NotaEntrada> listar(@RequestParam(required = false) Long fornecedorId) {
        return service.listar(fornecedorId);
    }

    /** Extra (HU-B11.4). */
    @GetMapping("/{id}")
    public NotaEntrada buscar(@PathVariable long id) {
        return service.buscar(id);
    }
}

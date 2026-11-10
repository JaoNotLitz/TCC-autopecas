package br.com.wcautopecas.fiscal.nota;

import br.com.wcautopecas.commons.dynamo.IdGenerator;
import br.com.wcautopecas.fiscal.api.NotaDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * HU-B11.1 RF05: nota de entrada de fornecedor registrada pelo entry-service. É nota de terceiro,
 * já autorizada pela SEFAZ do emitente: entra como {@code ENTRADA / autorizada}, sem XML gerado
 * aqui. Idempotente pela chave de acesso (retentativa do entry-service devolve a mesma nota).
 */
@Service
public class EntradaService {

    private static final Logger log = LoggerFactory.getLogger(EntradaService.class);
    static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final NotaRepository repo;
    private final IdGenerator ids;

    public EntradaService(NotaRepository repo, IdGenerator ids) {
        this.repo = repo;
        this.ids = ids;
    }

    public record Item(long produtoId, String codigo, String descricao, String ncm, String cfop, long quantidade,
            BigDecimal valorUnitario) {
    }

    public record Entrada(long entradaId, long fornecedorId, String numero, String serie, String chaveAcesso,
            LocalDate dataEmissao, List<Item> itens) {
    }

    public NotaDto registrar(Entrada in) {
        var existente = repo.doTipo("ENTRADA").stream()
                .filter(n -> in.chaveAcesso().equals(n.getChaveAcesso())).findFirst();
        if (existente.isPresent()) {
            log.info("entrada {} já registrada como nota {}", in.chaveAcesso(), existente.get().getId());
            return NotaDto.de(existente.get());
        }
        long id = ids.proximo("notas");
        BigDecimal total = BigDecimal.ZERO;
        List<NotaLinha> linhas = new ArrayList<>();
        long n = 1;
        for (Item i : in.itens()) {
            NotaLinha l = new NotaLinha();
            l.setNotaId(id);
            l.setItemId(n++);
            l.setProdutoId(i.produtoId());
            l.setCodigo(i.codigo());
            l.setDescricao(i.descricao());
            l.setNcm(i.ncm());
            l.setCfop(i.cfop());
            l.setQuantidade(i.quantidade());
            l.setValorUnitario(i.valorUnitario());
            l.setQuantidadeDevolvida(0L);
            l.setQuantidadeEmDevolucao(0L);
            linhas.add(l);
            total = total.add(i.valorUnitario().multiply(BigDecimal.valueOf(i.quantidade())));
        }
        total = total.setScale(2, RoundingMode.HALF_EVEN);
        String emitida = in.dataEmissao().atStartOfDay(FUSO).toInstant().toString();

        NotaItem nota = new NotaItem();
        nota.setId(id);
        nota.setTipo("ENTRADA");
        nota.setModelo("55");
        nota.setSerie(in.serie());
        nota.setNumero(in.numero());
        nota.setChaveAcesso(in.chaveAcesso());
        nota.setStatus("autorizada");
        nota.setFornecedorId(in.fornecedorId());
        nota.setValorProdutos(total);
        nota.setValorDesconto(BigDecimal.ZERO);
        nota.setValorTotal(total);
        nota.setEmitidaEm(emitida);
        nota.setAutorizadaEm(emitida);
        nota.setTentativasEnvio(0);
        nota.setMotivo("Entrada " + in.entradaId());
        repo.criar(nota, linhas, List.of());
        log.info("nota de entrada {} (chave {}) registrada para a entrada {}", id, in.chaveAcesso(), in.entradaId());
        return NotaDto.de(nota);
    }
}

package br.com.wcautopecas.entry.integracao;

import br.com.wcautopecas.entry.api.NotaFiscalDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Chamadas síncronas ao product-service e ao fiscal-service (decisão A4). */
public interface Integracoes {

    record CodigoProduto(String codigo, Long produtoId, String nome, boolean ativo) {
    }

    record ItemNota(long produtoId, String codigo, String descricao, String ncm, String cfop, long quantidade,
            BigDecimal valorUnitario) {
    }

    /** Corpo do {@code POST /internal/notas/entrada} do fiscal-service. */
    record NotaEntradaFiscal(long entradaId, long fornecedorId, String numero, String serie, String chaveAcesso,
            LocalDate dataEmissao, List<ItemNota> itens) {
    }

    List<CodigoProduto> produtosPorCodigo(List<String> codigos);

    NotaFiscalDto registrarNota(NotaEntradaFiscal nota);
}

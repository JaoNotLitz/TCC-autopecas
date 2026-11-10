package br.com.wcautopecas.fiscal.api;

import br.com.wcautopecas.fiscal.nota.NotaItem;
import br.com.wcautopecas.fiscal.nota.NotaLinha;
import java.math.BigDecimal;

/** {@code NotaFiscal} do front (seção 5.5), sem campos a mais. */
public record NotaDto(long id, String tipo, String numero, String serie, String chaveAcesso, Long clienteId,
        String clienteNome, String clienteDocumento, Long fornecedorId, BigDecimal valorTotal, String emitidaEm,
        String status, Long vendaId, Long notaReferenciadaId) {

    public static NotaDto de(NotaItem n) {
        return new NotaDto(n.getId(), n.getTipo(), n.getNumero(), n.getSerie(), n.getChaveAcesso(), n.getClienteId(),
                n.getClienteNome(), n.getClienteDocumento(), n.getFornecedorId(), n.getValorTotal(), n.getEmitidaEm(),
                n.getStatus(), n.getVendaId(), n.getNotaReferenciadaId());
    }

    /** {@code NotaFiscalItem} do front. */
    public record Item(long produtoId, String codigo, String descricao, String ncm, String cfop, long quantidade,
            BigDecimal valorUnitario) {

        public static Item de(NotaLinha l) {
            return new Item(l.getProdutoId(), l.getCodigo(), l.getDescricao(), l.getNcm(), l.getCfop(),
                    l.getQuantidade(), l.getValorUnitario());
        }
    }
}

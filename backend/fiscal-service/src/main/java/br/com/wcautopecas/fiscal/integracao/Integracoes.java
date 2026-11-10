package br.com.wcautopecas.fiscal.integracao;

import java.math.BigDecimal;
import java.util.List;

/** Chamadas síncronas a sales, customer e product (decisão A4). */
public interface Integracoes {

    record Venda(long id, String tipoDocumento, String modalidade, Long clienteId, String metodoPagamento,
            String bandeira, BigDecimal desconto, BigDecimal subtotal, BigDecimal total, String status,
            List<ItemVenda> itens, Long notaFiscalId) {
    }

    record ItemVenda(long produtoId, long quantidade, BigDecimal valorUnitario) {
    }

    record Cliente(long id, String nome, String documento, String cidade, String uf, String email, boolean ativo) {
    }

    /** Recorte do {@code GET /internal/produtos/{id}}. */
    record ProdutoFiscal(long id, String codigo, String nome, String ncm, String cfop, String origem,
            String cstIcms, BigDecimal aliquotaIcms) {
    }

    Venda venda(long id);

    Cliente cliente(long id);

    ProdutoFiscal produto(long id);

    void vincularNota(long vendaId, long notaFiscalId);
}

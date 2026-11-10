package br.com.wcautopecas.product.api;

import br.com.wcautopecas.product.produto.EstoqueItem;
import br.com.wcautopecas.product.produto.ProdutoItem;
import java.math.BigDecimal;

/** {@code Produto} do front: plano, com {@code estoqueAtual} e {@code status} no próprio item. */
public record ProdutoDto(long id, String codigo, String nome, String ncm, long estoqueAtual, long estoqueMinimo,
        BigDecimal precoCusto, BigDecimal precoVenda, String status) {

    public static ProdutoDto de(ProdutoItem p, EstoqueItem e) {
        long atual = e == null || e.getEstoqueAtual() == null ? 0 : e.getEstoqueAtual();
        String status = e == null ? "critico" : e.getStatus();
        return new ProdutoDto(p.getId(), p.getCodigo(), p.getNome(), p.getNcm(), atual,
                p.getEstoqueMinimo() == null ? 0 : p.getEstoqueMinimo(), p.getPrecoCusto(), p.getPrecoVenda(), status);
    }
}

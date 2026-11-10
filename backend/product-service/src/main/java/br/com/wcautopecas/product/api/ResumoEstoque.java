package br.com.wcautopecas.product.api;

import java.math.BigDecimal;

/** {@code ResumoEstoque} do front. */
public record ResumoEstoque(long totalProdutos, long estoqueBaixo, BigDecimal valorEmEstoque, long itensCriticos) {
}

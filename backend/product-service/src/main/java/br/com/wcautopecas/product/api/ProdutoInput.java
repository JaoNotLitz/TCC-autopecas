package br.com.wcautopecas.product.api;

import br.com.wcautopecas.commons.validacao.NCM;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** {@code ProdutoInput} do front (POST e PUT). */
public record ProdutoInput(
        @NotBlank(message = "Informe o código") @Size(max = 40, message = "Código muito longo") String codigo,
        @NotBlank(message = "Informe o nome") @Size(max = 150, message = "Nome muito longo") String nome,
        @NotBlank(message = "Informe o NCM") @NCM String ncm,
        @NotNull(message = "Informe o estoque atual") @Min(value = 0, message = "Estoque não pode ser negativo") Long estoqueAtual,
        @NotNull(message = "Informe o estoque mínimo") @Min(value = 0, message = "Estoque mínimo não pode ser negativo") Long estoqueMinimo,
        @NotNull(message = "Informe o preço de custo") @DecimalMin(value = "0.00", message = "Preço de custo inválido") BigDecimal precoCusto,
        @NotNull(message = "Informe o preço de venda") @DecimalMin(value = "0.01", message = "Preço de venda inválido") BigDecimal precoVenda) {
}

package br.com.wcautopecas.sales.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** {@code VendaInput} do front (HU-B06.1). As regras que cruzam campos ficam no VendaService. */
public record VendaInput(
        @NotBlank(message = "Informe o tipo de documento")
        @Pattern(regexp = "nfe|nfce", message = "Tipo de documento inválido") String tipoDocumento,
        @NotBlank(message = "Informe a modalidade")
        @Pattern(regexp = "fisica|virtual", message = "Modalidade inválida") String modalidade,
        Long clienteId,
        @NotBlank(message = "Informe o método de pagamento")
        @Pattern(regexp = "dinheiro|credito|debito|pix|boleto", message = "Método de pagamento inválido")
        String metodoPagamento,
        String bandeira,
        String destinoDinheiro,
        @PositiveOrZero(message = "Desconto inválido") BigDecimal desconto,
        @NotBlank(message = "Informe o status")
        @Pattern(regexp = "orcamento|finalizada", message = "Status inicial deve ser orcamento ou finalizada")
        String status,
        @NotEmpty(message = "Inclua pelo menos um item")
        @Size(max = 98, message = "Venda com itens demais (máximo 98)") List<@Valid Item> itens) {

    public record Item(
            @NotNull(message = "Informe o produto") Long produtoId,
            @NotNull(message = "Informe a quantidade") @Min(value = 1, message = "Quantidade mínima é 1") Long quantidade,
            @NotNull(message = "Informe o valor unitário")
            @DecimalMin(value = "0.01", message = "Valor unitário deve ser maior que zero") BigDecimal valorUnitario) {
    }
}

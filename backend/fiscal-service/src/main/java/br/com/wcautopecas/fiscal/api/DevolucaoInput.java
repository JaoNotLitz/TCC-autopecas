package br.com.wcautopecas.fiscal.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;

/** {@code DevolucaoInput} do front. O {@code valorUnitario} enviado é ignorado (vale o da nota original). */
public record DevolucaoInput(
        @NotNull(message = "Informe a nota referenciada") Long notaReferenciadaId,
        String motivo,
        @NotEmpty(message = "Informe pelo menos um item com quantidade a devolver") List<@Valid Item> itens) {

    public record Item(
            @NotNull(message = "Informe o produto") Long produtoId,
            @NotNull(message = "Informe a quantidade") @PositiveOrZero(message = "Quantidade inválida") Long quantidade,
            BigDecimal valorUnitario) {
    }
}

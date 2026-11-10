package br.com.wcautopecas.fiscal.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/** {@code InutilizacaoInput} do front (modelo 55). */
public record InutilizacaoInput(
        @NotBlank(message = "Informe a série") @Pattern(regexp = "\\d{1,3}", message = "Série inválida") String serie,
        @NotNull(message = "Informe o número inicial") @Positive(message = "Número inicial inválido") Long numeroInicial,
        @NotNull(message = "Informe o número final") @Positive(message = "Número final inválido") Long numeroFinal,
        @NotBlank(message = "Informe a justificativa")
        @Pattern(regexp = "(?s)\\s*\\S.{13,}\\S\\s*", message = "A justificativa precisa ter pelo menos 15 caracteres")
        String justificativa) {
}

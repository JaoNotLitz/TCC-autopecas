package br.com.wcautopecas.commons.erro;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Corpo de todo erro 4xx/5xx (seção 5.0 da arquitetura). O front mostra {@code message};
 * os demais campos são extras. {@code campos} só aparece quando há
 * detalhe por campo (validação, duplicidade, estoque).
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(String message, int status, String erro, List<CampoErro> campos) {
}

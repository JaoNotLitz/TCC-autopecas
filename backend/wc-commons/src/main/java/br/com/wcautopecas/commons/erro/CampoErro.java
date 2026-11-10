package br.com.wcautopecas.commons.erro;

/** Erro de um campo do corpo da requisição. */
public record CampoErro(String campo, String message) {
}

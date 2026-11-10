package br.com.wcautopecas.commons.erro;

import java.util.List;
import org.springframework.http.HttpStatus;

/** 400 - dados inválidos. */
public class ValidacaoException extends ApiException {

    public ValidacaoException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

    public ValidacaoException(String message, List<CampoErro> campos) {
        super(HttpStatus.BAD_REQUEST, message, campos);
    }
}

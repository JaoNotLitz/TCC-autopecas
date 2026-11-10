package br.com.wcautopecas.commons.erro;

import java.util.List;
import org.springframework.http.HttpStatus;

/** 403 - papel sem permissão. */
public class SemPermissaoException extends ApiException {

    public SemPermissaoException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }

    public SemPermissaoException(String message, List<CampoErro> campos) {
        super(HttpStatus.FORBIDDEN, message, campos);
    }
}

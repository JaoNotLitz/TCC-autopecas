package br.com.wcautopecas.commons.erro;

import java.util.List;
import org.springframework.http.HttpStatus;

/** 422 - regra de negócio violada. */
public class RegraNegocioException extends ApiException {

    public RegraNegocioException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    public RegraNegocioException(String message, List<CampoErro> campos) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message, campos);
    }
}

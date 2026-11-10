package br.com.wcautopecas.commons.erro;

import java.util.List;
import org.springframework.http.HttpStatus;

/** 409 - duplicidade ou estado conflitante. */
public class ConflitoException extends ApiException {

    public ConflitoException(String message) {
        super(HttpStatus.CONFLICT, message);
    }

    public ConflitoException(String message, List<CampoErro> campos) {
        super(HttpStatus.CONFLICT, message, campos);
    }
}

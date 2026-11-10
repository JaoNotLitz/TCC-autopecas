package br.com.wcautopecas.commons.erro;

import java.util.List;
import org.springframework.http.HttpStatus;

/** 401 - token ausente, inválido ou sem sessão. */
public class NaoAutenticadoException extends ApiException {

    public NaoAutenticadoException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }

    public NaoAutenticadoException(String message, List<CampoErro> campos) {
        super(HttpStatus.UNAUTHORIZED, message, campos);
    }
}

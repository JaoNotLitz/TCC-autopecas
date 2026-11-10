package br.com.wcautopecas.commons.erro;

import java.util.List;
import org.springframework.http.HttpStatus;

/** 404 - recurso não existe. */
public class NaoEncontradoException extends ApiException {

    public NaoEncontradoException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    public NaoEncontradoException(String message, List<CampoErro> campos) {
        super(HttpStatus.NOT_FOUND, message, campos);
    }
}

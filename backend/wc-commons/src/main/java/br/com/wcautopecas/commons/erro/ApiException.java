package br.com.wcautopecas.commons.erro;

import java.util.List;
import org.springframework.http.HttpStatus;

/** Exceção de domínio que já sabe o status HTTP e a mensagem para o front. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final List<CampoErro> campos;

    public ApiException(HttpStatus status, String message) {
        this(status, message, List.of());
    }

    public ApiException(HttpStatus status, String message, List<CampoErro> campos) {
        super(message);
        this.status = status;
        this.campos = campos == null ? List.of() : List.copyOf(campos);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public List<CampoErro> getCampos() {
        return campos;
    }
}

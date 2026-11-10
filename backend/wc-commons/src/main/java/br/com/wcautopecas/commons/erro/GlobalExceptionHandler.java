package br.com.wcautopecas.commons.erro;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Converte qualquer exceção no JSON de erro da seção 5.0. */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** {@code campos} vazio some do JSON; quem lança a exceção decide se manda (ex.: 409 de estoque). */
    public static ResponseEntity<ApiError> resposta(HttpStatus status, String message, List<CampoErro> campos) {
        ApiError corpo = new ApiError(message, status.value(), status.name(), campos == null ? List.of() : campos);
        return ResponseEntity.status(status).body(corpo);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> api(ApiException e) {
        return resposta(e.getStatus(), e.getMessage(), e.getCampos());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacao(MethodArgumentNotValidException e) {
        List<CampoErro> campos = e.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::campo)
                .toList();
        String message = campos.isEmpty() ? "Dados inválidos" : campos.get(0).message();
        return resposta(HttpStatus.BAD_REQUEST, message, campos);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> violacao(ConstraintViolationException e) {
        List<CampoErro> campos = e.getConstraintViolations().stream()
                .map(v -> new CampoErro(ultimoNo(v.getPropertyPath().toString()), v.getMessage()))
                .toList();
        String message = campos.isEmpty() ? "Dados inválidos" : campos.get(0).message();
        return resposta(HttpStatus.BAD_REQUEST, message, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> corpoInvalido(HttpMessageNotReadableException e) {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido", List.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> parametroAusente(MissingServletRequestParameterException e) {
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório: " + e.getParameterName(),
                List.of(new CampoErro(e.getParameterName(), "Obrigatório")));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> tipoInvalido(MethodArgumentTypeMismatchException e) {
        return resposta(HttpStatus.BAD_REQUEST, "Valor inválido para " + e.getName(),
                List.of(new CampoErro(e.getName(), "Valor inválido")));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> rotaInexistente(NoResourceFoundException e) {
        return resposta(HttpStatus.NOT_FOUND, "Recurso não encontrado", List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> metodo(HttpRequestMethodNotSupportedException e) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, "Método não suportado", List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> mediaType(HttpMediaTypeNotSupportedException e) {
        return resposta(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de conteúdo não suportado", List.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> uploadGrande(MaxUploadSizeExceededException e) {
        return resposta(HttpStatus.PAYLOAD_TOO_LARGE, "Arquivo acima do tamanho máximo", List.of());
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiError> multipart(MultipartException e) {
        return resposta(HttpStatus.BAD_REQUEST, "Envio de arquivo inválido", List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> inesperado(Exception e) {
        log.error("Erro inesperado", e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente.", List.of());
    }

    private static CampoErro campo(FieldError f) {
        return new CampoErro(f.getField(), f.getDefaultMessage());
    }

    private static String ultimoNo(String caminho) {
        int i = caminho.lastIndexOf('.');
        return i < 0 ? caminho : caminho.substring(i + 1);
    }
}

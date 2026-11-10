package br.com.wcautopecas.commons.validacao;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** CNPJ inválido. Valor nulo ou vazio é aceito (combine com @NotBlank quando obrigatório). */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CNPJ.Validador.class)
public @interface CNPJ {

    String message() default "CNPJ inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<CNPJ, String> {
        @Override
        public boolean isValid(String v, ConstraintValidatorContext ctx) {
            return v == null || v.isEmpty() || Documentos.cnpjValido(v);
        }
    }
}

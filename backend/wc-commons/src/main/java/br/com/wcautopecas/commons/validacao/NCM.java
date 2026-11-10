package br.com.wcautopecas.commons.validacao;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** NCM deve ter 8 dígitos. Valor nulo ou vazio é aceito (combine com @NotBlank quando obrigatório). */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NCM.Validador.class)
public @interface NCM {

    String message() default "NCM deve ter 8 dígitos";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<NCM, String> {
        @Override
        public boolean isValid(String v, ConstraintValidatorContext ctx) {
            return v == null || v.isEmpty() || Documentos.ncmValido(v);
        }
    }
}

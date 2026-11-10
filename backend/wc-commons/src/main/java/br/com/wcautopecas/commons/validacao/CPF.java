package br.com.wcautopecas.commons.validacao;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** CPF inválido. Valor nulo ou vazio é aceito (combine com @NotBlank quando obrigatório). */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CPF.Validador.class)
public @interface CPF {

    String message() default "CPF inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<CPF, String> {
        @Override
        public boolean isValid(String v, ConstraintValidatorContext ctx) {
            return v == null || v.isEmpty() || Documentos.cpfValido(v);
        }
    }
}

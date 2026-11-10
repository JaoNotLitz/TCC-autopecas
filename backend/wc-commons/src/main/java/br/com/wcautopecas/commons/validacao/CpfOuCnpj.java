package br.com.wcautopecas.commons.validacao;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** CPF ou CNPJ inválido. Valor nulo ou vazio é aceito (combine com @NotBlank quando obrigatório). */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CpfOuCnpj.Validador.class)
public @interface CpfOuCnpj {

    String message() default "CPF ou CNPJ inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<CpfOuCnpj, String> {
        @Override
        public boolean isValid(String v, ConstraintValidatorContext ctx) {
            return v == null || v.isEmpty() || Documentos.cpfValido(v) || Documentos.cnpjValido(v);
        }
    }
}

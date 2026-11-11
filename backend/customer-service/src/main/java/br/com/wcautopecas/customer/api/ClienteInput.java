package br.com.wcautopecas.customer.api;

import br.com.wcautopecas.commons.validacao.UF;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** {@code ClienteInput} do front (corpo de POST e PUT). O endereço é opcional e vai para o {@code enderDest} da NF-e. */
public record ClienteInput(
        @NotBlank(message = "Informe o tipo") @Pattern(regexp = "PF|PJ", message = "Tipo deve ser PF ou PJ") String tipo,
        @NotBlank(message = "Informe o CPF ou CNPJ") String documento,
        @NotBlank(message = "Informe o nome") @Size(max = 150, message = "Nome muito longo") String nome,
        @Email(message = "E-mail inválido") @Size(max = 150, message = "E-mail muito longo") String email,
        String telefone,
        @Size(max = 100, message = "Cidade muito longa") String cidade,
        @UF String uf,
        Boolean ativo,
        @Size(max = 9, message = "CEP inválido") String cep,
        @Size(max = 60, message = "Logradouro muito longo") String logradouro,
        @Size(max = 60, message = "Número muito longo") String numero,
        @Size(max = 60, message = "Complemento muito longo") String complemento,
        @Size(max = 60, message = "Bairro muito longo") String bairro) {
}

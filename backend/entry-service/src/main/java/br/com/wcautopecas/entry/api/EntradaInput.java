package br.com.wcautopecas.entry.api;

import br.com.wcautopecas.commons.validacao.CNPJ;
import br.com.wcautopecas.commons.validacao.ChaveAcesso;
import br.com.wcautopecas.commons.validacao.NCM;
import br.com.wcautopecas.commons.validacao.UF;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * {@code EntradaInput} do front (seção 5.7), plano. É também a resposta da prévia do XML
 * (HU-B11.2), por isso as datas são {@code yyyy-MM-dd}.
 */
public record EntradaInput(
        @NotNull(message = "Informe o fornecedor") @Valid Fornecedor fornecedor,
        @NotBlank(message = "Informe o número da nota")
        @Pattern(regexp = "\\d{1,9}", message = "Número inválido") String numero,
        @NotBlank(message = "Informe a série") @Pattern(regexp = "\\d{1,3}", message = "Série inválida") String serie,
        @NotNull(message = "Informe a data de emissão") LocalDate dataEmissao,
        @NotNull(message = "Informe a data de entrada") LocalDate dataEntrada,
        @NotBlank(message = "Informe a chave de acesso") @ChaveAcesso String chaveAcesso,
        @Size(max = 60, message = "Natureza da operação muito longa") String naturezaOperacao,
        @NotEmpty(message = "Inclua pelo menos um item")
        @Size(max = 990, message = "Nota com itens demais") List<@Valid Item> itens) {

    public record Fornecedor(
            @NotBlank(message = "Informe o CNPJ do fornecedor") @CNPJ String cnpj,
            @NotBlank(message = "Informe a razão social") @Size(max = 60, message = "Razão social muito longa")
            String razaoSocial,
            @Size(max = 14, message = "IE inválida") String ie,
            @UF String uf) {
    }

    public record Item(
            Long produtoId,
            String codigo,
            @Size(max = 120, message = "Descrição muito longa") String descricao,
            @NotBlank(message = "Informe o NCM") @NCM String ncm,
            @NotBlank(message = "Informe o CFOP")
            @Pattern(regexp = "[12]\\d{3}", message = "CFOP de entrada deve começar com 1 ou 2") String cfop,
            @NotNull(message = "Informe a quantidade") @Min(value = 1, message = "Quantidade mínima é 1") Long quantidade,
            @NotNull(message = "Informe o valor unitário")
            @DecimalMin(value = "0.01", message = "Valor unitário deve ser maior que zero") BigDecimal valorUnitario) {
    }
}

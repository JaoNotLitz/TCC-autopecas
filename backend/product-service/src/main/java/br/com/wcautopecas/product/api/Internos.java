package br.com.wcautopecas.product.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;

/** DTOs das rotas internas e extras (não usadas pelo front atual). */
public final class Internos {

    private Internos() {
    }

    /** GET /internal/produtos/{id}: produto com os dados fiscais e o reservado (fiscal e sales). */
    public record ProdutoFiscal(long id, String codigo, String nome, String ncm, BigDecimal precoVenda,
            boolean ativo, long estoqueAtual, long reservado, long disponivel, String cfop, String origem,
            String cstIcms, BigDecimal aliquotaIcms) {
    }

    /** POST /internal/produtos/por-codigo: {@code produtoId} de cada código, ou null (entry-service). */
    public record CodigoProduto(String codigo, Long produtoId, String nome, boolean ativo) {
    }

    public record ItemDisponibilidade(@NotNull Long produtoId, @NotNull Long quantidade) {
    }

    public record Disponibilidade(long produtoId, boolean existe, boolean ativo, long disponivel,
            boolean suficiente) {
    }

    public record AjusteInput(
            @NotNull(message = "Informe a quantidade nova") @PositiveOrZero(message = "Quantidade inválida") Long quantidadeNova,
            @NotBlank(message = "Informe o motivo") String motivo) {
    }

    public record AjusteAceito(long produtoId, long quantidadeNova, String status) {
    }

    public record Movimento(long id, long produtoId, String tipo, long quantidade, long estoqueAnterior,
            long estoqueNovo, Long referenciaId, String referenciaType, Long usuarioId, String motivo, String data) {
    }

    public record ListaDisponibilidade(@Valid List<ItemDisponibilidade> itens) {
    }
}

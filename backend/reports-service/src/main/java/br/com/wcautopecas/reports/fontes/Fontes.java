package br.com.wcautopecas.reports.fontes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Dados dos outros serviços para o dashboard e os relatórios (EPB-14). Implementação HTTP em {@link FontesHttp}. */
public interface Fontes {

    /** {@code GET /internal/vendas}: vendas com {@code data} nos dias do período (fuso da loja). */
    List<Venda> vendas(LocalDate de, LocalDate ate);

    /** {@code GET /internal/produtos/estoque}: posição de cada produto ativo. */
    List<Posicao> estoque();

    /** {@code GET /api/produtos/resumo}: o mesmo {@code ResumoEstoque} que o front mostra. */
    ResumoEstoque resumoEstoque();

    /** {@code GET /internal/clientes/resumo}. */
    ResumoClientes resumoClientes(Instant desde);

    /** {@code GET /api/clientes}: nome por id, para a coluna Cliente. */
    Map<Long, String> nomesClientes();

    record Venda(long id, String numero, String data, Long clienteId, Long vendedorId, String tipoDocumento,
            String metodoPagamento, BigDecimal subtotal, BigDecimal desconto, BigDecimal total, String status,
            Long notaFiscalId) {
    }

    record Posicao(long id, String codigo, String nome, String ncm, long estoqueAtual, long reservado,
            long disponivel, long estoqueMinimo, String status, BigDecimal precoCusto, BigDecimal precoVenda) {
    }

    record ResumoEstoque(long totalProdutos, long estoqueBaixo, BigDecimal valorEmEstoque, long itensCriticos) {
    }

    record ResumoClientes(long total, long ativos, long novos) {
    }
}

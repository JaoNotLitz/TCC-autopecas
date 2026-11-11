package br.com.wcautopecas.reports.painel;

import java.math.BigDecimal;
import java.util.List;

/** {@code GET /api/dashboard} (HU-B14.1). Os campos de {@code estoque} têm os nomes do {@code ResumoEstoque} do front. */
public record Dashboard(Vendas vendas, Estoque estoque, Fiscal fiscal, Clientes clientes) {

    /** Só vendas finalizadas; {@code ticketMedio = valorMes / mes}. */
    public record Vendas(long hoje, BigDecimal valorHoje, long mes, BigDecimal valorMes, BigDecimal ticketMedio) {
    }

    public record Estoque(long totalProdutos, long estoqueBaixo, long itensCriticos, BigDecimal valorEmEstoque,
            List<Critico> criticos) {
    }

    public record Critico(long id, String codigo, String nome, String ncm, long estoqueAtual, long estoqueMinimo) {
    }

    /** Notas emitidas pela loja (sem as de entrada): as de hoje e, por status, as do mês. */
    public record Fiscal(long emitidasHoje, long autorizadas, long processando, long rejeitadas,
            BigDecimal valorHoje) {
    }

    public record Clientes(long total, long ativos, long novosNoMes) {
    }
}

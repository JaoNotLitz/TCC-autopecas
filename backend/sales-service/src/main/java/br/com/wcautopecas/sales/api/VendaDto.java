package br.com.wcautopecas.sales.api;

import br.com.wcautopecas.sales.venda.VendaItem;
import br.com.wcautopecas.sales.venda.VendaItemLinha;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;

/**
 * {@code Venda} do front, sem campos a mais (HU-B06.1 RF05). {@code notaFiscalId} só aparece no
 * detalhe (extra da HU-B06.5); no POST fica de fora.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record VendaDto(long id, String tipoDocumento, String modalidade, Long clienteId, String metodoPagamento,
        String bandeira, String destinoDinheiro, BigDecimal desconto, BigDecimal subtotal, BigDecimal total,
        String status, List<Item> itens) {

    public record Item(long produtoId, long quantidade, BigDecimal valorUnitario) {
    }

    /** Detalhe: a {@code Venda} mais {@code notaFiscalId} ({@code null} enquanto não há nota). */
    public record Detalhe(long id, String tipoDocumento, String modalidade, Long clienteId, String metodoPagamento,
            String bandeira, String destinoDinheiro, BigDecimal desconto, BigDecimal subtotal, BigDecimal total,
            String status, List<Item> itens, Long notaFiscalId) {
    }

    public static VendaDto de(VendaItem v, List<VendaItemLinha> linhas) {
        return new VendaDto(v.getId(), v.getTipoDocumento(), v.getModalidade(), v.getClienteId(),
                v.getMetodoPagamento(), v.getBandeira(), v.getDestinoDinheiro(), v.getDesconto(), v.getSubtotal(),
                v.getTotal(), v.getStatus(), itens(linhas));
    }

    public static Detalhe detalhe(VendaItem v, List<VendaItemLinha> linhas) {
        return new Detalhe(v.getId(), v.getTipoDocumento(), v.getModalidade(), v.getClienteId(),
                v.getMetodoPagamento(), v.getBandeira(), v.getDestinoDinheiro(), v.getDesconto(), v.getSubtotal(),
                v.getTotal(), v.getStatus(), itens(linhas), v.getNotaFiscalId());
    }

    private static List<Item> itens(List<VendaItemLinha> linhas) {
        return linhas.stream().map(l -> new Item(l.getProdutoId(), l.getQuantidade(), l.getValorUnitario())).toList();
    }
}

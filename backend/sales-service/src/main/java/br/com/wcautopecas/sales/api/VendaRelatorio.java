package br.com.wcautopecas.sales.api;

import br.com.wcautopecas.sales.venda.VendaItem;
import java.math.BigDecimal;

/** GET /internal/vendas: a venda com data e vendedor, que o contrato do front não tem (EPB-14). */
public record VendaRelatorio(long id, String numero, String data, Long clienteId, Long vendedorId,
        String tipoDocumento, String metodoPagamento, BigDecimal subtotal, BigDecimal desconto, BigDecimal total,
        String status, Long notaFiscalId) {

    public static VendaRelatorio de(VendaItem v) {
        return new VendaRelatorio(v.getId(), v.getNumero(), v.getData(), v.getClienteId(), v.getVendedorId(),
                v.getTipoDocumento(), v.getMetodoPagamento(), v.getSubtotal(), v.getDesconto(), v.getTotal(),
                v.getStatus(), v.getNotaFiscalId());
    }
}

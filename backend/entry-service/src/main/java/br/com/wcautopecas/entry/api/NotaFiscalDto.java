package br.com.wcautopecas.entry.api;

import java.math.BigDecimal;

/** {@code NotaFiscal} do front, como o fiscal-service devolve. */
public record NotaFiscalDto(long id, String tipo, String numero, String serie, String chaveAcesso, Long clienteId,
        String clienteNome, String clienteDocumento, Long fornecedorId, BigDecimal valorTotal, String emitidaEm,
        String status, Long vendaId, Long notaReferenciadaId) {
}

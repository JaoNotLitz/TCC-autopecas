package br.com.wcautopecas.reports.fiscal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

/** As colunas do relatório, tiradas do {@code NotaFiscal} que o fiscal-service lista em {@code GET /api/notas}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NotaResumo(long id, String tipo, String numero, String serie, String chaveAcesso, String clienteNome,
        String clienteDocumento, BigDecimal valorTotal, String emitidaEm, String status) {
}

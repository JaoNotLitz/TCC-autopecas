package br.com.wcautopecas.fiscal;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "wc.fiscal")
public class FiscalProperties {

    private String tabelaNotas = "notas_fiscais";
    private String tabelaItens = "notas_items";
    private String tabelaEventos = "fiscal_events";
    private String filaVendas = "sales-events-queue";
    private String filaJobs = "fiscal-jobs-queue";
    private String filaEventos = "fiscal-events-queue";
    private String salesUrl = "http://sales-service:8083";
    private String customerUrl = "http://customer-service:8081";
    private String productUrl = "http://product-service:8082";
    private Duration timeoutChamadas = Duration.ofSeconds(2);
    /** Série única da loja (NF-e, NFC-e e devolução). */
    private String serie = "1";
    private String ambiente = "homologacao";
    /** UF do emitente; define o CFOP da devolução (1202 dentro do estado, 2202 fora). */
    private String ufEmitente = "SP";
    private Duration prazoCancelamento = Duration.ofHours(24);
}

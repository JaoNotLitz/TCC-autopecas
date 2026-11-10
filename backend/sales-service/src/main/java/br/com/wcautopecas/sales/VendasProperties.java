package br.com.wcautopecas.sales;

import java.math.BigDecimal;
import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "wc.vendas")
public class VendasProperties {

    private String tabelaVendas = "sales";
    private String tabelaItens = "sale_items";
    private String filaEstoque = "inventory-updates-queue";
    private String topicoVendas = "sales-events-topic";
    private String topicoNotificacoes = "sales-notifications-topic";
    /** Valor mínimo de venda para avisar no sales-notifications-topic. */
    private BigDecimal notificacaoMinimo = BigDecimal.ZERO;
    private String customerUrl = "http://customer-service:8081";
    private String productUrl = "http://product-service:8082";
    private Duration timeoutChamadas = Duration.ofSeconds(2);
}

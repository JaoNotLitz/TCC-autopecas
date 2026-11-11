package br.com.wcautopecas.reports;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "wc.relatorios")
public class RelatoriosProperties {

    private String fiscalUrl = "http://fiscal-service:8084";
    private String salesUrl = "http://sales-service:8083";
    private String productUrl = "http://product-service:8082";
    private String customerUrl = "http://customer-service:8081";
    /** Tempo de vida do dashboard no Redis (HU-B14.1 RF04). */
    private Duration cacheDashboard = Duration.ofMinutes(5);
    /** A listagem de notas não é paginada; num mês cheio a resposta demora mais que as outras chamadas. */
    private Duration timeoutChamadas = Duration.ofSeconds(10);
}

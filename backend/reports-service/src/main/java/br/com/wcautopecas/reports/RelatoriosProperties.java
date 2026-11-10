package br.com.wcautopecas.reports;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "wc.relatorios")
public class RelatoriosProperties {

    private String fiscalUrl = "http://fiscal-service:8084";
    /** A listagem de notas não é paginada; num mês cheio a resposta demora mais que as outras chamadas. */
    private Duration timeoutChamadas = Duration.ofSeconds(10);
}
